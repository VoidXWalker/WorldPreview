package me.voidxwalker.worldpreview.mixin.server;

import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
//import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerNetworkIo;
//import net.minecraft.server.ServerTask;
//import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.network.DemoServerPlayerInteractionManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.snooper.Snooper;
//import net.minecraft.util.thread.ReentrantThreadExecutor;
//import net.minecraft.world.GameMode;
//import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.Difficulty;
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
public abstract class MinecraftServerMixin {//  extends ReentrantThreadExecutor<ServerTask> {

    @Shadow public abstract ServerWorld getWorld(int id);

    @Shadow public abstract int getSpawnProtectionRadius();

    @Shadow public abstract boolean isLoading();

    @Shadow private boolean shouldResetWorld;

    @Shadow @Final private static Logger LOGGER;

    @Shadow public abstract ServerNetworkIo getNetworkIo();

    @Shadow private PlayerManager playerManager;

    @Shadow public ServerWorld[] worlds;

    @Shadow @Final private Snooper snooper;

    @Shadow private Thread serverThread;

    @Inject(method = "prepareWorlds", at = @At(value = "HEAD"))
    public void worldpreview_getWorld(CallbackInfo ci){
        synchronized (WorldPreview.lock){
            if(!WorldPreview.existingWorld){
                ServerWorld serverWorld = this.getWorld(0);
                WorldPreview.spawnPos= serverWorld.getSpawnPos();
                WorldPreview.freezePreview=false;
                WorldPreview.world= this.getWorld(0);
                LevelInfo properties = new LevelInfo(WorldPreview.world.getLevelProperties().getSeed(), LevelInfo.GameMode.SURVIVAL, false, WorldPreview.world.getLevelProperties().isHardcore(), WorldPreview.world.getLevelProperties().getGeneratorType());
                WorldPreview.clientWorld = new ClientWorld(null, properties, 0, Difficulty.NORMAL , MinecraftClient.getInstance().profiler);
                ClientPlayNetworkHandler networkHandler = new ClientPlayNetworkHandler(MinecraftClient.getInstance(), null, null, MinecraftClient.getInstance().getSession().getProfile());
                WorldPreview.player = new ClientPlayerEntity(MinecraftClient.getInstance(), WorldPreview.clientWorld, networkHandler,null);
                worldpreview_calculateSpawn(serverWorld);
                WorldPreview.calculatedSpawn=true;
            }
            WorldPreview.existingWorld=false;
        }
    }

    private void worldpreview_calculateSpawn(ServerWorld serverWorld) {
        BlockPos blockPos = WorldPreview.spawnPos;
        int i = Math.max(5, this.getSpawnProtectionRadius() - 6);
        int j = MathHelper.floor(WorldPreview.world.getWorldBorder().getDistanceInsideBorder((double)blockPos.getX(), (double)blockPos.getZ()));
        if (j < i) {
            i = j;
        }
        if (j <= 1) {
            i = 1;
        }
        Random random = new Random();
        blockPos = WorldPreview.world.getTopPosition(blockPos.add(random.nextInt(i * 2) - i, 0, random.nextInt(i * 2) - i));
        WorldPreview.player.refreshPositionAndAngles(blockPos, 0.0F, 0.0F);
        while(!WorldPreview.world.doesBoxCollide(WorldPreview.player, WorldPreview.player.getBoundingBox()).isEmpty() && WorldPreview.player.y < 255.0D) {
            WorldPreview.player.updatePosition(WorldPreview.player.x, WorldPreview.player.y + 1.0D, WorldPreview.player.z);
        }
        WorldPreview.spawnPos = new BlockPos(WorldPreview.player.x, WorldPreview.player.y, WorldPreview.player.z);
    }

    @Inject(method = "stopServer", at=@At(value = "HEAD"), cancellable = true)
    public void kill(CallbackInfo ci) {
        if (!this.isLoading() && Thread.currentThread().equals(this.serverThread)) {
            worldpreview_shutdownWithoutSave();
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

    @Inject(method = "prepareWorlds",at=@At(value = "INVOKE",target = "Lnet/minecraft/server/MinecraftServer;getTimeMillis()J", shift = At.Shift.AFTER), cancellable = true)
    public void kill3(CallbackInfo ci){
        if(WorldPreview.kill==1){
            ci.cancel();
        }
    }

    public void worldpreview_shutdownWithoutSave() {
        if (!this.shouldResetWorld) {
            LOGGER.info("Stopping server");
            if (this.getNetworkIo() != null) {
                this.getNetworkIo().stop();
            }

            if (this.worlds != null) {
                for(int i = 0; i < this.worlds.length; ++i) {
                    ServerWorld serverWorld = this.worlds[i];
                    serverWorld.close();
                }
            }
            if (this.snooper.isActive()) {
                this.snooper.concel();
            }
        }
    }
}
