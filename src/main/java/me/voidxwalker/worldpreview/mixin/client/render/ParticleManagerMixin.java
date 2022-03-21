package me.voidxwalker.worldpreview.mixin.client.render;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    @Shadow @Final private Queue<Particle> newParticles;

    @Inject(method = "setWorld", at = @At("TAIL"))
    public void onChangeWorld(ClientWorld world, CallbackInfo ci) {
        this.newParticles.clear();
    }
}
