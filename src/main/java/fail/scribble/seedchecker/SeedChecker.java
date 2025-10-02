package fail.scribble.seedchecker;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;

import net.fabricmc.api.ModInitializer;

public class SeedChecker implements ModInitializer {

	public static SeedFile seedFile;
	public static FileWatcherThread filewatcher;

	@Override
	public void onInitialize() {
		System.setProperty("java.awt.headless", "false");
		seedFile = new SeedFile();

//		try {
//			filewatcher = new FileWatcherThread(new SeedFileWatcher(Path.of(".")), "SeedFileWatcher");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		filewatcher.run();
	}

	public static void openAndWatchFile() {

		try {
			Desktop.getDesktop().edit(seedFile.getFile().toFile());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private class SeedFileWatcher extends FileWatcher {

		public SeedFileWatcher(Path rootDir) throws IOException {
			super(rootDir);
		}

		@Override
		protected void onNewFile(Path path) {

		}

		@Override
		protected void onModifyFile(Path path) {
			if (path.getFileName().equals(seedFile.getFile())) {
				seedFile.load();
			}
		}

		@Override
		protected void onDeleteFile(Path path) {

		}
	}

	private class FileWatcherThread extends Thread {

		private FileWatcher watcher;

		public FileWatcherThread(FileWatcher watcher, String version) {
			super("FileWatcher-" + version);
			this.watcher = watcher;
			this.setDaemon(true);
			this.start();
		}

		@Override
		public void run() {
			try {
				watcher.watch();
			} catch (IOException e) {
//				e.printStackTrace();
			} catch (InterruptedException e) {
				if (watcher != null)
					watcher.close();
				e.printStackTrace();
			} catch (ClosedWatchServiceException e) {
			}
		}

		public void close() {
			if (watcher != null)
				watcher.close();
		}
	}
}
