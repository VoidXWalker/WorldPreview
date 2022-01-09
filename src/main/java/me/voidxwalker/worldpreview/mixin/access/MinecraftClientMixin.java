package me.voidxwalker.worldpreview.mixin.access;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(MinecraftClient.class)
public interface MinecraftClientMixin {
    @Accessor
    AtomicReference<WorldGenerationProgressTracker> getWorldGenProgressTracker();
}
