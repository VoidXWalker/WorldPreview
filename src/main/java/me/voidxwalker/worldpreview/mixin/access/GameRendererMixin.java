package me.voidxwalker.worldpreview.mixin.access;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererMixin {

    @Invoker float callGetFov(float tickDelta, boolean changingFov);
}
