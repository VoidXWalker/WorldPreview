package me.voidxwalker.worldpreview.mixin.access;

import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BuiltChunkStorage.class)
public interface BuiltChunkStorageMixin {
    @Invoker
    ChunkRenderer callGetChunkRenderer(BlockPos pos);
}
