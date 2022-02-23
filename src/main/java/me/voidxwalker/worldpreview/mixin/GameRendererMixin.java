package me.voidxwalker.worldpreview.mixin;

import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.render.GameRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow @Final private MinecraftClient client;

    @Redirect(method = "render",at = @At(value = "FIELD",target = "Lnet/minecraft/client/MinecraftClient;skipGameRender:Z",opcode = Opcodes.GETFIELD))
    public boolean freezePreview(MinecraftClient instance){
        if(!(client.currentScreen instanceof LevelLoadingScreen)){
            WorldPreview.freezePreview=false;
        }
            if(WorldPreview.freezePreview){

                return instance.skipGameRender;
            }
            return instance.skipGameRender;
    }
}
