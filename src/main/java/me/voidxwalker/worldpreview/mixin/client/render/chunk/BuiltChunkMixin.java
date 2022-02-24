package me.voidxwalker.worldpreview.mixin.client.render.chunk;

import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkBuilder.BuiltChunk.class)
public class BuiltChunkMixin {
    @Redirect(method = "getSquaredCameraDistance",at=@At(value = "INVOKE",target = "Lnet/minecraft/client/render/GameRenderer;getCamera()Lnet/minecraft/client/render/Camera;"))
    public Camera worldpreview_getCorrectPos(GameRenderer instance){
        if(MinecraftClient.getInstance().currentScreen instanceof LevelLoadingScreen){
            return WorldPreview.camera;
        }
        return instance.getCamera();
    }
}
