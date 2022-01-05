package me.voidxwalker.worldpreview.mixin;

import me.voidxwalker.worldpreview.Main;
import me.voidxwalker.worldpreview.mixin.access.ClientChunkManagerMixin;
import me.voidxwalker.worldpreview.mixin.access.ClientChunkMapMixin;
import me.voidxwalker.worldpreview.mixin.access.ThreadedAnvilChunkStorageMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;

@Mixin(ServerChunkManager.class)
public abstract class ServerChunkManagerMixin {
    @Shadow @Final public ThreadedAnvilChunkStorage threadedAnvilChunkStorage;

    @Shadow public @Nullable abstract WorldChunk getWorldChunk(int chunkX, int chunkZ);

    @Inject(method ="tick()Z",at = @At(value = "TAIL"))
    public void getChunks(CallbackInfoReturnable<Boolean> cir){
        if(Main.world!=null&&Main.clientWord!=null&&Main.worldRenderer!=null&& MinecraftClient.getInstance().currentScreen instanceof LevelLoadingScreen){
            Iterator<ChunkHolder> iterator =  ((ThreadedAnvilChunkStorageMixin) this.threadedAnvilChunkStorage).getChunkHolders().values().stream().iterator();
            try{
                while (iterator.hasNext()){
                   // System.out.println(3);

                    ChunkHolder holder = iterator.next();
                    if(holder!=null){
                        ClientChunkManager.ClientChunkMap map = ((((ClientChunkManagerMixin) Main.clientWord.getChunkManager()).getChunks()));
                        int index = ((ClientChunkMapMixin)(Object)(map)).callGetIndex(holder.getPos().x,holder.getPos().z);
                        WorldChunk chunk = this.getWorldChunk(holder.getPos().x,holder.getPos().z);
                        ((ClientChunkMapMixin)(Object)(map)).callSet(index,chunk);
                    }
                }
            }
            catch(Exception x){
            }
        }
    }
}
