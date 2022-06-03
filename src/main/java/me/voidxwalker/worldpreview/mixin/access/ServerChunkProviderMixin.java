package me.voidxwalker.worldpreview.mixin.access;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ServerChunkProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ServerChunkProvider.class)
public interface ServerChunkProviderMixin {

    @Accessor
    List<Chunk> getChunks();
}
