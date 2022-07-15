package me.voidxwalker.worldpreview.mixin.client;

import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
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


    @Shadow @Final private MinecraftClient client;

    @Inject(method ="<init>",at=@At("TAIL"))
    public void oldSodiumCompatibility(ClientPlayNetworkHandler networkHandler, ClientWorld.Properties properties, RegistryKey registryRef, RegistryEntry dimensionTypeEntry, int loadDistance, int simulationDistance, Supplier profiler, WorldRenderer worldRenderer, boolean debugWorld, long seed, CallbackInfo ci){
        if(WorldPreview.camera==null&& WorldPreview.world!=null&& WorldPreview.spawnPos!=null){
            this.chunkManager=worldpreview_getChunkManager(loadDistance);
        }

    }

    private ClientChunkManager worldpreview_getChunkManager(int i){
        return new ClientChunkManager((ClientWorld) (Object)this, i);
    }
    @Redirect(method ="getRegistryManager",at=@At(value = "INVOKE",target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;getRegistryManager()Lnet/minecraft/util/registry/DynamicRegistryManager;"))
    public DynamicRegistryManager worldpreview_getRegistryManager(ClientPlayNetworkHandler instance){

        return client.getServer().getRegistryManager();
    }
}
