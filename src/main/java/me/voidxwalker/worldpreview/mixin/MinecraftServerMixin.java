package me.voidxwalker.worldpreview.mixin;

import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.mixin.access.SpawnLocatingMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerNetworkIo;
import net.minecraft.server.ServerTask;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;
import java.util.function.Supplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin  extends ReentrantThreadExecutor<ServerTask> {
    public MinecraftServerMixin(String string) {
        super(string);
    }

    @Shadow public abstract @Nullable ServerWorld getWorld(RegistryKey<World> key);

    @Shadow public abstract ServerWorld getOverworld();

    @Shadow public abstract Iterable<ServerWorld> getWorlds();

    @Shadow @Final private Snooper snooper;

    @Shadow private ServerResourceManager serverResourceManager;

    @Shadow @Final protected LevelStorage.Session session;

    @Shadow @Final private static Logger LOGGER;

    @Shadow public abstract @Nullable ServerNetworkIo getNetworkIo();

    @Shadow public abstract Thread getThread();

    @Shadow public abstract int getSpawnRadius(@Nullable ServerWorld world);

    @Inject(method = "prepareStartRegion", at = @At(value = "HEAD"))

    public void worldpreview_getWorld(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci){
        synchronized (WorldPreview.lock){
            if(!WorldPreview.existingWorld){
                ServerWorld serverWorld = this.getOverworld();
                WorldPreview.spawnPos= serverWorld.getSpawnPos();
                WorldPreview.stop=false;
                WorldPreview.world=this.getWorld(World.OVERWORLD);
                RegistryKey<DimensionType> registryKey = DimensionType.OVERWORLD_REGISTRY_KEY;
                RegistryKey<World> registryKey2 = World.OVERWORLD;
                DimensionType dimensionType = DimensionType.getOverworldDimensionType();
                ClientWorld.Properties properties = new ClientWorld.Properties(Difficulty.NORMAL, WorldPreview.world.getLevelProperties().isHardcore(), false);
                Supplier<Profiler>s=MinecraftClient.getInstance()::getProfiler;
                long seed = BiomeAccess.hashSeed(((ServerWorld)(WorldPreview.world)).getSeed());
                WorldPreview.clientWord = new ClientWorld(null,properties, registryKey2, registryKey, dimensionType,16 , s,null,false, seed);
                WorldPreview.player=new ClientPlayerEntity(MinecraftClient.getInstance(),WorldPreview.clientWord,new ClientPlayNetworkHandler(MinecraftClient.getInstance(),null,null,MinecraftClient.getInstance().getSession().getProfile()),null,null,false,false);
                worldpreview_calculateSpawn(serverWorld);
                WorldPreview.calculatedSpawn=true;
            }
            WorldPreview.existingWorld=false;
        }
    }

    private void worldpreview_calculateSpawn(ServerWorld serverWorld) {
        BlockPos blockPos = WorldPreview.spawnPos;
        int i = Math.max(0, this.getSpawnRadius((ServerWorld) WorldPreview.world));
        int j = MathHelper.floor(WorldPreview.world.getWorldBorder().getDistanceInsideBorder(blockPos.getX(), blockPos.getZ()));
        if (j < i) {
            i = j;
        }
        if (j <= 1) {
            i = 1;
        }
        long l = i * 2L + 1;
        long m = l * l;
        int k = m > 2147483647L ? Integer.MAX_VALUE : (int) m;
        int n = this.worldpreview_calculateSpawnOffsetMultiplier(k);
        int o = (new Random()).nextInt(k);
        WorldPreview.playerSpawn = o;
        for (int p = 0; p < k; ++p) {
            int q = (o + n * p) % k;
            int r = q % (i * 2 + 1);
            int s = q / (i * 2 + 1);
            BlockPos blockPos2 = SpawnLocatingMixin.callFindOverworldSpawn(serverWorld, blockPos.getX() + r - i, blockPos.getZ() + s - i, false);
            if (blockPos2 != null) {
                WorldPreview.player.refreshPositionAndAngles(blockPos2, 0.0F, 0.0F);
                if (serverWorld.doesNotCollide(WorldPreview.player)) {
                    break;
                }
            }
        }
    }

    private int worldpreview_calculateSpawnOffsetMultiplier(int horizontalSpawnArea) {
        return horizontalSpawnArea <= 16 ? horizontalSpawnArea - 1 : 17;
    }

    @Inject(method = "shutdown",at=@At(value = "HEAD"),cancellable = true)
    public void worldpreview_kill(CallbackInfo ci){
        if(MinecraftClient.getInstance().currentScreen instanceof LevelLoadingScreen&&Thread.currentThread().getId()!=this.getThread().getId()) {
            worldpreview_shutdownWithoutSave();
           ci.cancel();
        }
    }

    @Inject(method="runServer",at=@At(value="INVOKE",target="Lnet/minecraft/server/MinecraftServer;setupServer()Z",shift = At.Shift.AFTER), cancellable = true)
    public void worldpreview_kill2(CallbackInfo ci){
        WorldPreview.inPreview=false;
        if(WorldPreview.kill==1){
            ci.cancel();
        }
    }

    public void worldpreview_shutdownWithoutSave(){
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
                }
                catch (IOException ignored) {}
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

    @Inject(method = "prepareStartRegion",at=@At(value = "INVOKE",target = "Lnet/minecraft/server/world/ServerChunkManager;getTotalChunksLoadedCount()I",shift = At.Shift.AFTER),cancellable = true)
    public void worldpreview_kill(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci){
        if(WorldPreview.kill==1){
           ci.cancel();
        }
    }
}
