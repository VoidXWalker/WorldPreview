package me.voidxwalker.worldpreview.mixin.access;

import net.minecraft.util.collection.LongObjectStorage;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ClientChunkProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ClientChunkProvider.class)
public interface ClientChunkProviderMixin {
    @Accessor
    LongObjectStorage<Chunk> getChunkStorage();
    @Accessor List<Chunk> getChunks();
}
