package me.voidxwalker.worldpreview.mixin.server;

import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerNetworkIo;
import net.minecraft.server.ServerTask;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.snooper.Snooper;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.world.GameMode;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelInfo;
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

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin  extends ReentrantThreadExecutor<ServerTask> {
    public MinecraftServerMixin(String string) {
        super(string);
    }


    @Shadow public abstract Iterable<ServerWorld> getWorlds();

    @Shadow @Final private Snooper snooper;

    @Shadow @Final private static Logger LOGGER;

    @Shadow public abstract @Nullable ServerNetworkIo getNetworkIo();

    @Shadow public abstract Thread getThread();

    @Shadow public abstract int getSpawnRadius(@Nullable ServerWorld world);

    @Shadow public abstract ServerWorld getWorld(DimensionType dimensionType);

    @Inject(method = "prepareStartRegion", at = @At(value = "HEAD"))

    public void worldpreview_getWorld(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci){
        WorldPreview.calculatedSpawn=false;
        synchronized (WorldPreview.lock){
            if(!WorldPreview.existingWorld){
                ServerWorld serverWorld = this.getWorld(DimensionType.OVERWORLD);
                WorldPreview.spawnPos= serverWorld.getSpawnPos();
                WorldPreview.freezePreview=false;
                WorldPreview.world= this.getWorld(DimensionType.OVERWORLD);
                LevelInfo properties = new LevelInfo(WorldPreview.world.getLevelProperties().getSeed(), GameMode.SURVIVAL, false, WorldPreview.world.getLevelProperties().isHardcore(), WorldPreview.world.getLevelProperties().getGeneratorOptions());
                WorldPreview.clientWord = new ClientWorld(null,properties, DimensionType.OVERWORLD,16 ,MinecraftClient.getInstance()::getProfiler,null);
                WorldPreview.player=new ClientPlayerEntity(MinecraftClient.getInstance(),WorldPreview.clientWord,new ClientPlayNetworkHandler(MinecraftClient.getInstance(),null,null,MinecraftClient.getInstance().getSession().getProfile()),null,null);
                worldpreview_calculateSpawn(serverWorld);
                WorldPreview.calculatedSpawn=true;
            }
            WorldPreview.existingWorld=false;
        }
    }

    private void worldpreview_calculateSpawn(ServerWorld serverWorld) {
        BlockPos blockPos = serverWorld.getSpawnPos();
        int i = Math.max(0, getSpawnRadius(serverWorld));
        int j = MathHelper.floor(serverWorld.getWorldBorder().getDistanceInsideBorder((double)blockPos.getX(), (double)blockPos.getZ()));
        if (j < i) {
            i = j;
        }

        if (j <= 1) {
            i = 1;
        }

        long l = (long)(i * 2 + 1);
        long m = l * l;
        int k = m > 2147483647L ? Integer.MAX_VALUE : (int)m;
        int n = k <= 16 ? k - 1 : 17;
        int o = (new Random()).nextInt(k);
        WorldPreview.playerSpawn=o;
        for(int p = 0; p < k; ++p) {
            int q = (o + n * p) % k;
            int r = q % (i * 2 + 1);
            int s = q / (i * 2 + 1);
            BlockPos blockPos2 = serverWorld.getDimension().getTopSpawningBlockPosition(blockPos.getX() + r - i, blockPos.getZ() + s - i, false);
            if (blockPos2 != null) {
                WorldPreview.player.refreshPositionAndAngles(blockPos2, 0.0F, 0.0F);
                if (serverWorld.doesNotCollide(WorldPreview.player)) {
                    break;
                }
            }
        }
    }
    @Inject(method = "shutdown",at=@At(value = "HEAD"),cancellable = true)
    public void kill(CallbackInfo ci){
        if(MinecraftClient.getInstance().currentScreen instanceof LevelLoadingScreen&&Thread.currentThread().getId()!=this.getThread().getId()) {
            shutdownWithoutSave();
            ci.cancel();
        }
    }

    @Inject(method="run",at=@At(value="INVOKE",target="Lnet/minecraft/server/MinecraftServer;setupServer()Z",shift = At.Shift.AFTER), cancellable = true)
    public void kill2(CallbackInfo ci){
        WorldPreview.inPreview=false;
        if(WorldPreview.kill==1){
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

    }

    @Inject(method = "prepareStartRegion",at=@At(value = "INVOKE",target = "Lnet/minecraft/server/world/ServerChunkManager;getTotalChunksLoadedCount()I",shift = At.Shift.AFTER),cancellable = true)
    public void kill(WorldGenerationProgressListener worldGenerationProgressListener, CallbackInfo ci){
        if(WorldPreview.kill==1){
            ci.cancel();
        }
    }
}