package fail.scribble.seedchecker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import fail.scribble.seedchecker.util.PathLock;
import net.minecraft.world.level.levelgen.WorldOptions;

public class SeedFile {

	private final Path file = Path.of("seedchecker/seedlist.txt");
	public final Queue<Long> seedList = new ConcurrentLinkedQueue<>();
	private final PathLock lock = new PathLock();

	public SeedFile() {
		if (!Files.exists(file.getParent())) {
			SeedChecker.LOGGER.info("Creating new directories %s", file.getParent());
			try {
				Files.createDirectories(file.getParent());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (Files.exists(file)) {
			load();
		} else {
			createNew();
		}
	}

	public void createNew() {
		SeedChecker.LOGGER.info("Creating new %s", file.getFileName());
		List<String> lines = new ArrayList<>();
		lines.add("// Paste your seeds in this file (each seed in a new line) and save it. Also supports text as seeds!");
		writeFile(file, lines);
	}

	public void load() {
		if (lock.isLocked(file)) {
			return;
		}
		SeedChecker.LOGGER.info("Loading %s", file);
		List<String> in = readFile(file);

		int lineCount = 0;
		seedList.clear();
		for (String line : in) {
			lineCount++;
			if (line.startsWith("//"))
				continue;

			OptionalLong opt = WorldOptions.parseSeed(line);
			if (opt.isPresent()) {
				seedList.add(opt.getAsLong());
			} else {
				SeedChecker.LOGGER.warn("Could not parse seed in line %s: %s", lineCount, line);
			}
		}
	}

	public void save() {
		SeedChecker.LOGGER.info("Saving seedlist.txt");
		List<String> out = new ArrayList<>();
		out.add("// Paste your seeds in this file (each seed in a new line) and save it. Also supports text as seeds!");
		for (Long seed : seedList) {
			out.add(Long.toString(seed));
		}

		lock.scheduleAndLock(file);
		writeFile(file, out);
	}

	private void writeFile(Path path, List<String> lines) {
		try {
			Files.write(path, lines);
		} catch (IOException e) {
			SeedChecker.LOGGER.catching(e);
		}
	}

	private List<String> readFile(Path path) {
		List<String> out = new ArrayList<>();
		try {
			out = Files.readAllLines(path);
		} catch (IOException e) {
			SeedChecker.LOGGER.catching(e);
		}

		return out;
	}

	public Path getFile() {
		return file;
	}
}
