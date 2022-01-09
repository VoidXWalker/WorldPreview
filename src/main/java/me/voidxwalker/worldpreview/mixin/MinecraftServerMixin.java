package me.voidxwalker.worldpreview.mixin;

import me.voidxwalker.worldpreview.CustomPlayerEntity;
import me.voidxwalker.worldpreview.Main;
import me.voidxwalker.worldpreview.mixin.access.MinecraftClientMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.*;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.snooper.Snooper;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelStorage;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Supplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin  extends ReentrantThreadExecutor<ServerTask> {
    public MinecraftServerMixin(String string) {
        super(string);
    }

    @Shadow public abstract @Nullable ServerWorld getWorld(RegistryKey<World> key);

    @Shadow public abstract ServerWorld getOverworld();

    @Shadow private volatile boolean running;

    @Shadow public abstract Iterable<ServerWorld> getWorlds();

    @Shadow @Final private Snooper snooper;

    @Shadow private ServerResourceManager serverResourceManager;

    @Shadow @Final protected LevelStorage.Session session;

    @Shadow @Final private static Logger LOGGER;

    @Shadow public abstract @Nullable ServerNetworkIo getNetworkIo();

    @Shadow public abstract boolean isRunning();

    @Inject(method = "prepareStartRegion", at = @At(value = "HEAD"))

    public void getWorld(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci){
        if(!Main.existingWorld&&((MinecraftClientMixin)MinecraftClient.getInstance()).getWorldGenProgressTracker().get().getProgressPercentage()<50){
            ServerWorld serverWorld = this.getOverworld();
            Main.spawnPos= serverWorld.getSpawnPos();
            Main.world=this.getWorld(World.OVERWORLD);
            RegistryKey<DimensionType> registryKey = DimensionType.OVERWORLD_REGISTRY_KEY;
            RegistryKey<World> registryKey2 = World.OVERWORLD;
            DimensionType dimensionType = DimensionType.getOverworldDimensionType();
            ClientWorld.Properties properties = new ClientWorld.Properties(Difficulty.NORMAL, Main.world.getLevelProperties().isHardcore(), false);
            Main.player=new CustomPlayerEntity(EntityType.PLAYER,Main.world,Main.spawnPos,0,0);
            Supplier<Profiler>s=MinecraftClient.getInstance()::getProfiler;
            long seed = BiomeAccess.hashSeed(((ServerWorld)(Main.world)).getSeed());
            Main.clientWord = new ClientWorld(null,properties, registryKey2, registryKey, dimensionType,16 , s,null,false, seed);
        }
        Main.existingWorld=false;
    }

    @Inject(method = "shutdown",at=@At(value = "HEAD"),cancellable = true)
    public void kill(CallbackInfo ci){
        if(Main.kill==1){
            if(this.isRunning()){
                this.shutdownWithoutSave();
                this.running=false;
            }
            else {
                Main.kill=0;
            }
            ci.cancel();
        }
    }

    @Inject(method="runServer",at=@At(value="INVOKE",target="Lnet/minecraft/server/ServerMetadata;setVersion(Lnet/minecraft/server/ServerMetadata$Version;)V"), cancellable = true)
    public void kill2(CallbackInfo ci){
        if(Main.kill==1){
            Main.kill=0;
            ci.cancel();
        }
    }

    public void shutdownWithoutSave(){
        LOGGER.info("Stopping server");
        if (this.getNetworkIo() != null) {
            this.getNetworkIo().stop();
        }
        Iterator var1 = this.getWorlds().iterator();
        ServerWorld serverWorld2;
        while(var1.hasNext()) {
            serverWorld2 = (ServerWorld)var1.next();
            if (serverWorld2 != null) {
                serverWorld2.savingDisabled = false;
            }
        }
        Iterator<ServerWorld> var2 = this.getWorlds().iterator();
        while(var2.hasNext()) {
            serverWorld2 = var2.next();
            if (serverWorld2 != null) {
                try {
                    serverWorld2.getChunkManager().threadedAnvilChunkStorage.close();
                } catch (IOException var5) {
                }
            }
        }
        if (this.snooper.isActive()) {
            this.snooper.cancel();
        }
        this.serverResourceManager.close();
        try {
            this.session.close();
        } catch (IOException var4) {
            LOGGER.error("Failed to unlock level {}", this.session.getDirectoryName(), var4);
        }
    }

    @Redirect(method = "prepareStartRegion",at=@At(value = "INVOKE",target = "Lnet/minecraft/server/world/ServerChunkManager;getTotalChunksLoadedCount()I"))
    public int kill(ServerChunkManager instance){
        if(Main.kill==1){
            return 441;
        }
        return  instance.getTotalChunksLoadedCount();
    }
}
