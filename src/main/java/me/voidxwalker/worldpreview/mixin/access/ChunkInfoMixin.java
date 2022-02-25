package me.voidxwalker.worldpreview.mixin.access;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldRenderer.ChunkInfo.class)
public interface ChunkInfoMixin {
    @Accessor
    ChunkRenderer getRenderer();
    @Accessor Direction getField_4125();
    @Accessor byte getField_4126();
    @Accessor int getField_4122();
}