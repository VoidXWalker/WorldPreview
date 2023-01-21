package me.voidxwalker.worldpreview.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameMenuScreen.class, priority = 900)
public class GameMenuScreenMixin {


    /**
     * With the way we handle mouse events and limited fps, sometimes the display is considered inactive when preview ends.
     * This causes the game to pause, and a click input can occur on the save and quit button before the client's world
     * is initialized. This causes a crash.
     */
    @Inject(method = "buttonClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;isIntegratedServerRunning()Z", shift = At.Shift.BEFORE),
            cancellable = true)
    private void cancelIfWorldNull(ButtonWidget button, CallbackInfo ci) {
        if (MinecraftClient.getInstance().world == null) {
            ci.cancel();
        }
    }
}
