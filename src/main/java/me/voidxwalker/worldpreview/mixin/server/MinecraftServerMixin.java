package me.voidxwalker.worldpreview.mixin.server;

import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.mixin.access.ClientChunkProviderMixin;
import me.voidxwalker.worldpreview.mixin.access.ServerChunkProviderMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerNetworkIo;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.LongObjectStorage;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.snooper.Snooper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ServerChunkProvider;
import net.minecraft.world.level.LevelInfo;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;
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

    @Shadow public abstract World getWorld();

    private int lastRow = -100;

    @Redirect(method = "prepareWorlds",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/chunk/ServerChunkProvider;getOrGenerateChunk(II)Lnet/minecraft/world/chunk/Chunk;"))
    public Chunk getChunks(ServerChunkProvider instance, int x, int z){
        Chunk ret = instance.getOrGenerateChunk(x,z);
        synchronized (WorldPreview.lock){
            if(WorldPreview.player!=null && !WorldPreview.freezePreview && WorldPreview.inPreview){
                LongObjectStorage<Chunk> chunkStorage=((ClientChunkProviderMixin) WorldPreview.clientWorld.getChunkProvider()).getChunkStorage();
                List<Chunk> chunks=((ClientChunkProviderMixin) WorldPreview.clientWorld.getChunkProvider()).getChunks();
                Iterator<Chunk> iterator =  ((ServerChunkProviderMixin)instance).getChunks().iterator();
                BlockPos spawnPos = WorldPreview.spawnPos;
                int spawnChunkX = spawnPos.getX() >> 4;
                int spawnChunkZ = spawnPos.getZ() >> 4;
                while (iterator.hasNext()){
                    Chunk chunk = iterator.next();
                    long id = ChunkPos.getIdFromCoords(chunk.chunkX, chunk.chunkZ);
                    if(chunkStorage.get(id)==null && chunk.isTerrainPopulated()){
                        chunkStorage.set(ChunkPos.getIdFromCoords(chunk.chunkX, chunk.chunkZ), chunk);
                        chunks.add(chunk);
                        chunk.setChunkLoaded(true);
                        if (spawnChunkX == chunk.chunkX && spawnChunkZ == chunk.chunkZ && !WorldPreview.loadedSpawn) {
                            worldpreview_calculateSpawn((ServerWorld) getWorld());
                            WorldPreview.loadedSpawn = true;
                        }
                        if (WorldPreview.loadedSpawn && chunk.chunkX != lastRow && Math.abs(chunk.chunkX - spawnChunkX) % 6 == 0) {
                            lastRow = chunk.chunkX;
                            WorldPreview.canReload = true;
                        }
                    }
                }
            }
        }
        return ret;
    }

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
            }
            WorldPreview.existingWorld=false;
        }
    }

    @Inject(method = "prepareWorlds", at = @At("TAIL"))
    private void resetLastRow(CallbackInfo ci) {
        lastRow = -100;
    }

    private void worldpreview_calculateSpawn(ServerWorld serverWorld) {
        BlockPos blockPos = WorldPreview.spawnPos;
        if (!serverWorld.dimension.isNether() && serverWorld.getLevelProperties().getGameMode() != LevelInfo.GameMode.ADVENTURE) {
            int i = Math.max(5, this.getSpawnProtectionRadius() - 6);
            int j = MathHelper.floor(WorldPreview.world.getWorldBorder().getDistanceInsideBorder((double) blockPos.getX(), (double) blockPos.getZ()));
            if (j < i) {
                i = j;
            }
            if (j <= 1) {
                i = 1;
            }
            Random random = new Random();
            blockPos = WorldPreview.world.getTopPosition(blockPos.add(random.nextInt(i * 2) - i, 0, random.nextInt(i * 2) - i));
            WorldPreview.player.refreshPositionAndAngles(blockPos, 0.0F, 0.0F);
            while (!WorldPreview.world.doesBoxCollide(WorldPreview.player, WorldPreview.player.getBoundingBox()).isEmpty() && WorldPreview.player.y < 255.0D) {
                WorldPreview.player.updatePosition(WorldPreview.player.x, WorldPreview.player.y + 1.0D, WorldPreview.player.z);
            }
            WorldPreview.spawnPos = new BlockPos(WorldPreview.player.x, WorldPreview.player.y, WorldPreview.player.z);
        }
    }

    @Inject(method = "stopServer", at=@At(value = "HEAD"), cancellable = true)
    public void kill(CallbackInfo ci) {
        if (!this.isLoading() && Thread.currentThread().equals(this.serverThread)) {
            worldpreview_shutdownWithoutSave();
            ci.cancel();
        }
        WorldPreview.inPreview=false;
    }

    @Inject(method="run",at=@At(value="INVOKE",target="Lnet/minecraft/server/MinecraftServer;setupServer()Z",shift = At.Shift.AFTER), cancellable = true)
    public void kill2(CallbackInfo ci){
        if(WorldPreview.kill==1){
            WorldPreview.inPreview=false;
            ci.cancel();
        }
    }

    @Inject(method = "prepareWorlds",at=@At(value = "INVOKE",target = "Lnet/minecraft/server/MinecraftServer;getTimeMillis()J", shift = At.Shift.AFTER), cancellable = true)
    public void kill3(CallbackInfo ci){
        if(WorldPreview.kill==1){
            WorldPreview.inPreview=false;
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
        WorldPreview.inPreview=false;
    }
}
