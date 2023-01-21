package me.voidxwalker.worldpreview.mixin.access;

import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.world.ChunkRenderFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BuiltChunkStorage.class)
public interface BuiltChunkStorageMixin {
    @Invoker
    void invokeCreateChunks(ChunkRenderFactory chunkRenderFactory);
}
