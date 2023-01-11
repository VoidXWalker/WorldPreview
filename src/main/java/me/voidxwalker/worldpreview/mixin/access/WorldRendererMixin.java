package me.voidxwalker.worldpreview.mixin.access;

import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WorldRenderer.class)
public interface WorldRendererMixin
{
    @Accessor
    ClientWorld getWorld();

    @Accessor
    BuiltChunkStorage getChunks();

    @Accessor
    void setField_1889(int i);
}