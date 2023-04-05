package me.voidxwalker.worldpreview.mixin.access;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftClientMixin {
    @Invoker
    public Thread invokeGetThread();
}
