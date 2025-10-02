package fail.scribble.seedchecker;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

/**
 * File watching service
 * @author Pancake
 */
public abstract class FileWatcher {

	private WatchService service;
	private Path rootDir;
	private Map<Path, WatchKey> watchKeys = new HashMap<>();

	/**
	 * Creates a new file watching service
	 * @param rootDir Base directory for watching
	 * @throws IOException Watch service not supported
	 */
	public FileWatcher(Path rootDir) throws IOException {
		if (!Files.isDirectory(rootDir))
			throw new IOException("Invalid path: " + rootDir);
		this.service = FileSystems.getDefault().newWatchService();
		this.rootDir = rootDir;
		this.init();
	}

	/**
	 * Initializes the watch service
	 * @throws IOException Watch service not supported
	 */
	private void init() throws IOException {
		// Add all existing subfolders recursively
		Files.walkFileTree(this.rootDir, new FileVisitor<Path>() {
			// @formatter:off
			@Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException { return FileVisitResult.CONTINUE; }
			@Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException { return FileVisitResult.CONTINUE; }
			@Override public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException { return FileVisitResult.CONTINUE; }
			// @formatter:on
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				FileWatcher.this.watchKeys.put(dir.toAbsolutePath(), dir.register(FileWatcher.this.service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE));
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public String getKeyByValue(Map<String, WatchKey> map, WatchKey value) {
		for (Map.Entry<String, WatchKey> entry : map.entrySet())
			if (value.equals(entry.getValue()))
				return entry.getKey();
		return null;
	}

	/**
	 * Watches the file system
	 * @throws InterruptedException Thread was interrupted
	 * @throws IOException File system problems
	 */
	public final void watch() throws InterruptedException, IOException {
		// Watch for file system events
		WatchKey key;
		while ((key = this.service.take()) != null) {
			Thread.sleep(400); // prevent multiple writes
			for (var event : key.pollEvents()) {
				// Find directory of event in the most inefficient way ever.
				Path parent = null;
				for (Map.Entry<Path, WatchKey> entry : this.watchKeys.entrySet())
					if (key.equals(entry.getValue()))
						parent = entry.getKey();

				// Trigger events for file system events
				var path = parent.resolve((Path) event.context()).toAbsolutePath();
				if (!Files.isDirectory(path) && path.toFile().length() <= 0 && event.kind() != StandardWatchEventKinds.ENTRY_DELETE)
					continue;

				if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
					if (this.watchKeys.containsKey(path))
						this.deleteDirectory(path);
					else
						this.onDeleteFile(path);
				} else if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
					if (Files.isDirectory(path))
						this.newDirectory(path);
					else
						this.onNewFile(path);
				} else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY)
					if (!Files.isDirectory(path))
						this.onModifyFile(path);
			}
			key.reset();
		}
	}

	/**
	 * Closes the file watcher
	 */
	public final void close() {
		try {
			this.service.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Update file watcher when a directory is created
	 * @param path Path to directory
	 * @throws IOException Watch service exception
	 */
	protected void newDirectory(Path path) throws IOException {
		// register new folder to watcher
		this.watchKeys.put(path, path.register(this.service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE));
	}

	/**
	 * Update file watcher when a directory is deleted
	 * @param path Path to directory
	 * @throws IOException Watch service exception
	 */
	protected void deleteDirectory(Path path) throws IOException {
		// unregister deleted folder from wather
		this.watchKeys.get(path).cancel();
	}

	/**
	 * Executed when a file was created
	 * @param path Path to file
	 */
	protected abstract void onNewFile(Path path);

	/**
	 * Executed when a file was modified
	 * @param path Path to file
	 */
	protected abstract void onModifyFile(Path path);

	/**
	 * Executed when a file was deleted
	 * @param path Path to file
	 */
	protected abstract void onDeleteFile(Path path);

}
