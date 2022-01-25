package me.voidxwalker.worldpreview.mixin;

import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.registry.RegistryTracker;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Mutable @Shadow @Final private ClientChunkManager chunkManager;

    @Redirect(method = "<init>",at=@At(value="INVOKE",target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;getRegistryTracker()Lnet/minecraft/util/registry/RegistryTracker;"))
    public RegistryTracker stopLag(ClientPlayNetworkHandler instance){
        if(instance==null){
            return RegistryTracker.create();
        }
        return instance.getRegistryTracker();
    }

    @Inject(method ="<init>",at=@At("TAIL"))
    public void oldSodiumCompatibility(ClientPlayNetworkHandler clientPlayNetworkHandler, ClientWorld.Properties properties, RegistryKey registryKey, RegistryKey registryKey2, DimensionType dimensionType, int i, Supplier supplier, WorldRenderer worldRenderer, boolean bl, long l, CallbackInfo ci){
        if(WorldPreview.camera==null&& WorldPreview.world!=null&& WorldPreview.spawnPos!=null){
            this.chunkManager=getChunkManager(i);
        }

    }

    private ClientChunkManager getChunkManager(int i){
        return new ClientChunkManager((ClientWorld) (Object)this, i);
    }
}
