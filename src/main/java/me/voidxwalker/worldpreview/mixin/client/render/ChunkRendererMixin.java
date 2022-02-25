package me.voidxwalker.worldpreview.mixin.client.render;

import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;

@Mixin(ChunkRenderer.class)
public class ChunkRendererMixin {
    @Redirect(method = "rebuildChunk",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/render/WorldRenderer;updateNoCullingBlockEntities(Ljava/util/Collection;Ljava/util/Collection;)V"))
    public void preventException(WorldRenderer instance, Collection<BlockEntity> removed, Collection<BlockEntity> added){
        if (instance==null){
            WorldPreview.worldRenderer.updateNoCullingBlockEntities(removed,added);
        }
        else {
            instance.updateNoCullingBlockEntities(removed,added);
        }

    }
}
