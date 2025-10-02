package fail.scribble.seedchecker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SeedFile {

	private final Path file = Path.of("seedlist.txt");
	public final Queue<Long> seedList = new ConcurrentLinkedQueue<>();

	public SeedFile() {
		if (Files.exists(file)) {
			load();
		} else {
			createNew();
		}
	}

	public void createNew() {
		List<String> lines = new ArrayList<>();
		lines.add("// Paste your seeds in this file and save it");
		writeFile(file, lines);
	}

	public void load() {
		List<String> in = readFile(file);

		int lineCount = 0;

		for (String line : in) {
			lineCount++;
			if (line.startsWith("//"))
				continue;

			try {
				seedList.add(Long.parseLong(line));
			} catch (NumberFormatException e) {
				System.out.println(String.format("Line %s", lineCount));
				e.printStackTrace();
			}
		}
	}

	public void save() {
		List<String> out = new ArrayList<>();
		out.add("// Paste your seeds in this file and save it");
		for (Long seed : seedList) {
			out.add(Long.toString(seed));
		}

		writeFile(file, out);
	}

	private void writeFile(Path path, List<String> lines) {
		try {
			Files.write(path, lines);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private List<String> readFile(Path path) {
		List<String> out = new ArrayList<>();
		try {
			out = Files.readAllLines(path);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return out;
	}

	public Path getFile() {
		return file;
	}
}
