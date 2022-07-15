package me.voidxwalker.worldpreview.mixin.access;

import net.minecraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderPhase.class)
public interface RenderPhaseMixin {

    @Accessor  @Mutable
    static RenderPhase.Target getPARTICLES_TARGET() {
        throw new AssertionError();
    }

    @Accessor @Mutable
    static RenderPhase.Target getCLOUDS_TARGET() {
        throw new AssertionError();
    }

    @Accessor @Mutable
    static RenderPhase.Target getWEATHER_TARGET() {
        throw new AssertionError();
    }



}
