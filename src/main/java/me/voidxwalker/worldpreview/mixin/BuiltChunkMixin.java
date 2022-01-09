package me.voidxwalker.worldpreview.mixin;

import me.voidxwalker.worldpreview.Main;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Set;

@Mixin(ChunkBuilder.BuiltChunk.class)
public class BuiltChunkMixin {
    @Redirect(method = "setNoCullingBlockEntities",at=@At(value = "INVOKE",target = "Lnet/minecraft/client/render/WorldRenderer;updateNoCullingBlockEntities(Ljava/util/Collection;Ljava/util/Collection;)V"))
    public void sodiumCompatibility(WorldRenderer instance, Collection<BlockEntity> removed, Collection<BlockEntity> added){
        if(instance==null){
            Main.worldRenderer.updateNoCullingBlockEntities(removed, added);
        }
        else {
            instance.updateNoCullingBlockEntities(removed,added);
        }
    }
}
