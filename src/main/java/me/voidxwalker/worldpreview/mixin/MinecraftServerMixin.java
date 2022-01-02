package me.voidxwalker.worldpreview.mixin;

import me.voidxwalker.worldpreview.CustomPlayerEntity;
import me.voidxwalker.worldpreview.Main;
import me.voidxwalker.worldpreview.PreviewRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow public abstract @Nullable ServerWorld getWorld(RegistryKey<World> key);

    @Shadow public abstract ServerWorld getOverworld();

    @Shadow private volatile boolean running;

    @Inject(method = "prepareStartRegion", at = @At(value = "HEAD"))

    public void getWorld(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci){
        ServerWorld serverWorld = this.getOverworld();
        Main.spawnPos= serverWorld.getSpawnPos();
        Main.world=this.getWorld(World.OVERWORLD);
        RegistryKey<DimensionType> registryKey = DimensionType.OVERWORLD_REGISTRY_KEY;
        RegistryKey<World> registryKey2 = World.OVERWORLD;
        DimensionType dimensionType = DimensionType.getOverworldDimensionType();
        ClientWorld.Properties properties = new ClientWorld.Properties(Difficulty.NORMAL, Main.world.getLevelProperties().isHardcore(), false);
        Main.player=new CustomPlayerEntity(EntityType.ARMOR_STAND,Main.world,Main.spawnPos,0,0);
        Main.clientWord = new ClientWorld(new ClientPlayNetworkHandler(MinecraftClient.getInstance(),null,null,null),properties, registryKey2, registryKey, dimensionType, 16, MinecraftClient.getInstance()::getProfiler, Main.worldRenderer,false, BiomeAccess.hashSeed(((ServerWorld)(Main.world)).getSeed()));
        Main.worldRenderer=new PreviewRenderer(MinecraftClient.getInstance(), new BufferBuilderStorage());
        Main.worldRenderer.setWorld(Main.clientWord);


    }

    @Redirect(method = "shutdown",at=@At(value = "INVOKE",target = "Lnet/minecraft/server/MinecraftServer;save(ZZZ)Z"))
    public boolean kill(MinecraftServer instance, boolean bl, boolean bl2, boolean bl3){
        if(Main.kill){
            this.running=false;
            return true;
        }
        return  instance.save(bl,bl2,bl3);
    }
    @Redirect(method = "prepareStartRegion",at=@At(value = "INVOKE",target = "Lnet/minecraft/server/world/ServerChunkManager;getTotalChunksLoadedCount()I"))
    public int kill(ServerChunkManager instance){
        if(Main.kill){
            this.running=false;
            return 441;
        }
        return  instance.getTotalChunksLoadedCount();
    }
}
