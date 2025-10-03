package fail.scribble.seedchecker.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fail.scribble.seedchecker.SeedChecker;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {

	private MixinTitleScreen(Component component) {
		super(component);
	}

	@Inject(method = "init", at = @At(value = "RETURN"))
	public void inject_titleScreen(CallbackInfo ci) {
		this.addRenderableWidget(Button.builder(Component.literal("Open seed file"), button -> {
			SeedChecker.openFile();
		}).build());
	}
}
