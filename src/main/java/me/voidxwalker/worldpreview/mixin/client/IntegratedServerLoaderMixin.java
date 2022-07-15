package me.voidxwalker.worldpreview.mixin.client;

import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.server.integrated.IntegratedServerLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IntegratedServerLoader.class)
public class IntegratedServerLoaderMixin {
    @Inject(method = "start(Lnet/minecraft/client/gui/screen/Screen;Ljava/lang/String;)V",at = @At("HEAD"))
    public void isExistingWorld(Screen parent, String levelName, CallbackInfo ci){
        WorldPreview.existingWorld=true;
    }
}
