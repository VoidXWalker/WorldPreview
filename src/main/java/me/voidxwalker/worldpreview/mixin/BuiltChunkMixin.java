package me.voidxwalker.worldpreview.mixin;

import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChunkBuilder.class)
public class BuiltChunkMixin {
    /*@Redirect(method = "setNoCullingBlockEntities",at=@At(value = "INVOKE",target = "Lnet/minecraft/client/render/WorldRenderer;updateNoCullingBlockEntities(Ljava/util/Collection;Ljava/util/Collection;)V"))
    public void sodiumCompatibility(WorldRenderer instance, Collection<BlockEntity> removed, Collection<BlockEntity> added){
        if(instance==null){
            WorldPreview.worldRenderer.updateNoCullingBlockEntities(removed, added);
        }
        else {
            instance.updateNoCullingBlockEntities(removed,added);
        }
    }*/
}
