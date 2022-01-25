package me.voidxwalker.worldpreview.mixin.access;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererMixin {
    @Accessor
    LightmapTextureManager getLightmapTextureManager();
}
