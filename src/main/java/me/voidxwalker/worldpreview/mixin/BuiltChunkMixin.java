package me.voidxwalker.worldpreview.mixin;

import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;

@Mixin(ChunkBuilder.BuiltChunk.class)
public class BuiltChunkMixin {
    @Redirect(method = "setNoCullingBlockEntities",at=@At(value = "INVOKE",target = "Lnet/minecraft/client/render/WorldRenderer;updateNoCullingBlockEntities(Ljava/util/Collection;Ljava/util/Collection;)V"))
    public void sodiumCompatibility(WorldRenderer instance, Collection<BlockEntity> removed, Collection<BlockEntity> added){
        if(instance==null){
            WorldPreview.worldRenderer.updateNoCullingBlockEntities(removed, added);
        }
        else {
            instance.updateNoCullingBlockEntities(removed,added);
        }
    }
    @Redirect(method = "getSquaredCameraDistance",at=@At(value = "INVOKE",target = "Lnet/minecraft/client/render/GameRenderer;getCamera()Lnet/minecraft/client/render/Camera;"))
    public Camera getCorrectPos(GameRenderer instance){
        if(MinecraftClient.getInstance().currentScreen instanceof LevelLoadingScreen){
            return WorldPreview.camera;
        }
        return instance.getCamera();
    }
}
