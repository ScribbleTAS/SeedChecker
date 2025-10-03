package fail.scribble.seedchecker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.mojang.serialization.Lifecycle;

import fail.scribble.seedchecker.SeedChecker.SeedCheckerConfigOptions;
import net.minecraft.FileUtil;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.DataPackReloadCookie;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContextMapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldLoader.DataLoadContext;
import net.minecraft.server.WorldLoader.DataLoadOutput;
import net.minecraft.server.WorldLoader.InitConfig;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldDimensions.Complete;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import net.minecraft.world.level.storage.PrimaryLevelData;

public class SCWorldLoader {

	private static GameType getGameType() {
		return GameType.valueOf(SeedChecker.config.get(SeedCheckerConfigOptions.GAME_MODE));
	}

	private static WorldDimensions getGeneratorType(HolderLookup.Provider provider) {
		ResourceKey<WorldPreset> preset = WorldPresets.NORMAL;
		switch (SeedChecker.config.get(SeedCheckerConfigOptions.WORLD_TYPE)) {
			case "amplified":
				preset = WorldPresets.AMPLIFIED;
				break;
			case "flat":
				preset = WorldPresets.FLAT;
				break;
			case "single_biome":
				preset = WorldPresets.SINGLE_BIOME_SURFACE;
				break;
			default:
				preset = WorldPresets.NORMAL;
				break;
		}
		return provider.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(preset).value().createWorldDimensions();
	}

	private static Difficulty getDifficulty() {
		return Difficulty.valueOf(SeedChecker.config.get(SeedCheckerConfigOptions.DIFFICULTY));
	}

	private static String getFolderName() {
		return SeedChecker.config.get(SeedCheckerConfigOptions.FOLDER_NAME);
	}

	private static boolean isHardcore() {
		return SeedChecker.config.getBoolean(SeedCheckerConfigOptions.HARDCORE);
	}

	private static boolean areCommandsEnabled() {
		return SeedChecker.config.getBoolean(SeedCheckerConfigOptions.ALLOW_COMMANDS);
	}

	public static void loadWorld(long nextSeed) {
		Minecraft mc = Minecraft.getInstance();

		if (mc.level != null) {
			mc.level.disconnect(DialogScreen.DISCONNECT);
			mc.disconnectWithSavingScreen();
		}

		String folderName = getFolderName();
		try {
			folderName = FileUtil.findAvailableName(Path.of("saves"), folderName, "");
		} catch (IOException e) {
			e.printStackTrace();
		}

		Optional<LevelStorageAccess> optional = CreateWorldScreen.createNewWorldDirectory(mc, folderName, null);

		WorldCreationContextMapper worldCreationContextMapper = (reloadableServerResources, layeredRegistryAccess, dataPackReloadCookie) -> new WorldCreationContext(dataPackReloadCookie.worldGenSettings(), layeredRegistryAccess, reloadableServerResources, dataPackReloadCookie.dataConfiguration());
		PackRepository packRepository = new PackRepository(new ServerPacksSource(mc.directoryValidator()));
		WorldDataConfiguration worldDataConfiguration = SharedConstants.IS_RUNNING_IN_IDE ? new WorldDataConfiguration(new DataPackConfig(List.of("vanilla", "tests"), List.of()), FeatureFlags.DEFAULT_FLAGS) : WorldDataConfiguration.DEFAULT;
		InitConfig initConfig = CreateWorldScreen.createDefaultLoadConfig(packRepository, worldDataConfiguration);
		Function<DataLoadContext, WorldGenSettings> function = dataLoadContext -> new WorldGenSettings(new WorldOptions(nextSeed, true, false), getGeneratorType(dataLoadContext.datapackWorldgen()));
		CompletableFuture<WorldCreationContext> completableFuture = WorldLoader.load(initConfig, dataLoadContext -> new DataLoadOutput<>(new DataPackReloadCookie((WorldGenSettings) function.apply(dataLoadContext), dataLoadContext.dataConfiguration()), dataLoadContext.datapackDimensions()), (closeableResourceManager, reloadableServerResources, layeredRegistryAccess, dataPackReloadCookie) -> {
			closeableResourceManager.close();
			return worldCreationContextMapper.apply(reloadableServerResources, layeredRegistryAccess, dataPackReloadCookie);
		}, Util.backgroundExecutor(), mc);
		mc.managedBlock(completableFuture::isDone);

		WorldCreationContext worldCreationContext = completableFuture.join();
		Complete complete = worldCreationContext.selectedDimensions().bake(worldCreationContext.datapackDimensions());
		LayeredRegistryAccess<RegistryLayer> layeredRegistryAccess = worldCreationContext.worldgenRegistries().replaceFrom(RegistryLayer.DIMENSIONS, complete.dimensionsRegistryAccess());

		Lifecycle lifecycle = FeatureFlags.isExperimental(worldCreationContext.dataConfiguration().enabledFeatures()) ? Lifecycle.experimental() : Lifecycle.stable();
		Lifecycle lifecycle2 = layeredRegistryAccess.compositeAccess().allRegistriesLifecycle();
		Lifecycle lifecycle3 = lifecycle2.add(lifecycle);

		LevelSettings levelSettings = new LevelSettings(Long.toString(nextSeed), getGameType(), isHardcore(), getDifficulty(), areCommandsEnabled(), new GameRules(FeatureFlags.DEFAULT_FLAGS), worldCreationContext.dataConfiguration());
		PrimaryLevelData primaryLevelData = new PrimaryLevelData(levelSettings, worldCreationContext.options(), complete.specialWorldProperty(), lifecycle3);

		mc.createWorldOpenFlows().createLevelFromExistingSettings((LevelStorageAccess) optional.get(), worldCreationContext.dataPackResources(), layeredRegistryAccess, primaryLevelData);
	}
}
