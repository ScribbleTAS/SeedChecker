package fail.scribble.seedchecker;

import org.lwjgl.glfw.GLFW;

import fail.scribble.seedchecker.common.EventClientGameLoop;
import fail.scribble.seedchecker.common.KeybindManager;
import fail.scribble.seedchecker.common.KeybindManager.Keybind;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

public class SeedCheckerClient implements ClientModInitializer {

	private KeybindManager keybindManager = new KeybindManager(KeybindManager::isKeyDownExceptTextField);

	@Override
	public void onInitializeClient() {
		registerKeybindings();
	}

	private void registerKeybindings() {
		String category = "SeedChanger";
		keybindManager.registerKeybind(new Keybind("Next Seed", category, GLFW.GLFW_KEY_O, this::nextSeed));

		EventClientGameLoop.EVENT.register(keybindManager::onRunClientGameLoop);
	}

	private void nextSeed(Minecraft client) {
		Long nextSeed = SeedChecker.seedFile.seedList.poll();

		if (nextSeed == null) {
			//@formatter:off
			client.gui.getChat().addMessage(
					Component.translatable("The seed list is empty! [%s]", 
							Component.translatable("Open seed list").withStyle(ChatFormatting.YELLOW).withStyle(s->
								s.withClickEvent(new ClickEvent.OpenFile(SeedChecker.seedFile.getFile()))
							))
					.withStyle(ChatFormatting.WHITE));
			//@formatter:on
			return;
		}

		SeedChecker.LOGGER.info("Loading new seed %s", nextSeed);
		SCWorldLoader.loadWorld(nextSeed);
		SeedChecker.seedFile.save();
	}
}
