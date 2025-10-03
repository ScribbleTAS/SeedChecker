package fail.scribble.seedchecker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fail.scribble.seedchecker.common.Configuration;
import fail.scribble.seedchecker.common.ConfigurationRegistry;
import fail.scribble.seedchecker.common.ConfigurationRegistry.ConfigOptions;
import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeEvent.EventType;
import io.methvin.watcher.DirectoryChangeListener;
import io.methvin.watcher.DirectoryWatcher;
import net.fabricmc.api.ModInitializer;
import net.minecraft.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;

public class SeedChecker implements ModInitializer {

	public static final Logger LOGGER = LogManager.getFormatterLogger("SeedChecker");
	public static SeedFile seedFile;
	public static DirectoryWatcher watcher;
	public static Configuration config;

	public static enum SeedCheckerConfigOptions implements ConfigOptions {
		GAME_MODE("gameMode", GameType.CREATIVE.toString()),
		DIFFICULTY("difficulty", Difficulty.EASY.toString()),
		WORLD_TYPE("worldType", "normal"),
		HARDCORE("hardcore", "false"),
		ALLOW_COMMANDS("allowCommands", "true"),
		FOLDER_NAME("worldFolderName", "SeedChecker World");

		final String key;
		final String defaultValue;

		private SeedCheckerConfigOptions(String key) {
			this(key, "");
		}

		private SeedCheckerConfigOptions(String key, String defaultValue) {
			this.key = key;
			this.defaultValue = defaultValue;
		}

		@Override
		public String getExtensionName() {
			return "SeedCheckerConfigOptions";
		}

		@Override
		public String getConfigKey() {
			return key;
		}

		@Override
		public String getDefaultValue() {
			return defaultValue;
		}
	}

	@Override
	public void onInitialize() {
		System.setProperty("java.awt.headless", "false");
		seedFile = new SeedFile();

		ConfigurationRegistry configRegistry = new ConfigurationRegistry();
		configRegistry.register(SeedCheckerConfigOptions.values());
		config = new Configuration("SeedChecker configuration", Path.of("config/seedchecker.json"), configRegistry);
		config.load();
		config.save();

		try {
			watcher = DirectoryWatcher.builder().paths(List.of(Path.of("./seedchecker"), Path.of("./config"))).listener(new SeedFileWatcher()).build();
		} catch (IOException e) {
			e.printStackTrace();
		}
		watcher.watchAsync();
	}

	public static void openFile() {
		Util.getPlatform().openFile(seedFile.getFile().toFile());
	}

	class SeedFileWatcher implements DirectoryChangeListener {

		@Override
		public void onEvent(DirectoryChangeEvent event) throws IOException {
			if (event.eventType() == EventType.MODIFY) {
				Path modifiedFile = event.path();
				Path fileName = modifiedFile.getFileName();
				if (fileName.equals(seedFile.getFile().getFileName()))
					seedFile.load();
				else if (fileName.equals(config.getFile().getFileName()))
					config.load();
			}
		}

		@Override
		public void onException(Exception e) {
			LOGGER.catching(e);
		}
	}
}
