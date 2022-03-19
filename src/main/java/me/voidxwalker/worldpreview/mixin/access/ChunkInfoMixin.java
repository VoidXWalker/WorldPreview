package me.voidxwalker.worldpreview.mixin.access;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.BuiltChunk;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldRenderer.ChunkInfo.class)
public interface ChunkInfoMixin {
    @Accessor BuiltChunk getField_10830();
    @Accessor Direction getField_10831();
    //@Accessor byte getField_4126();
    @Accessor int getField_10833();
}