package me.voidxwalker.worldpreview.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.registry.RegistryTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Redirect(method = "<init>",at=@At(value="INVOKE",target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;getRegistryTracker()Lnet/minecraft/util/registry/RegistryTracker;"))
    public RegistryTracker stopLag(ClientPlayNetworkHandler instance){
        if(instance==null){
            return RegistryTracker.create();
        }
        return instance.getRegistryTracker();
    }
}
