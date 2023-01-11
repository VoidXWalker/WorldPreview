package me.voidxwalker.worldpreview.mixin.access;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldRenderer.ChunkInfo.class)
public interface ChunkInfoMixin {
    @Accessor ChunkBuilder.BuiltChunk getChunk();
    @Accessor Direction getDirection();
    @Accessor byte getCullingState();
    @Accessor int getPropagationLevel();
}
