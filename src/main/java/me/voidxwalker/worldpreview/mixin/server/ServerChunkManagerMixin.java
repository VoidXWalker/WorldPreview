package me.voidxwalker.worldpreview.mixin.server;

import me.voidxwalker.worldpreview.ChunkSetter;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.BlockView;
import net.minecraft.world.chunk.WorldChunk;
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


    @Shadow public abstract BlockView getChunk(int chunkX, int chunkZ);

    @Inject(method ="tick()Z",at = @At(value = "TAIL"))
    public void getChunks(CallbackInfoReturnable<Boolean> cir){

        synchronized (WorldPreview.lock){
            if(WorldPreview.player!=null&& WorldPreview.calculatedSpawn&& !WorldPreview.freezePreview&&MinecraftClient.getInstance().currentScreen instanceof LevelLoadingScreen){

                ClientChunkManager.ClientChunkMap map = ((((ClientChunkManagerMixin) WorldPreview.clientWord.getChunkManager()).getChunks()));
                    Iterator<ChunkHolder> iterator =  ((ThreadedAnvilChunkStorageMixin) this.threadedAnvilChunkStorage).getChunkHolders().values().stream().iterator();
                    while (iterator.hasNext()){
                        ChunkHolder holder = iterator.next();
                        if(holder!=null){
                            int index = ((ClientChunkMapMixin)(Object)(map)).callIndex(holder.getPos().x,holder.getPos().z);
                            if(((ClientChunkMapMixin)(Object)(map)).callGetChunk(index)==null) {
                                BlockView chunk = this.getChunk(holder.getPos().x,holder.getPos().z);
                                if(chunk instanceof WorldChunk){
                                    WorldChunk chunk2 = (WorldChunk) chunk;
                                    if(chunk!=null){
                                        ((ChunkSetter)(Object)(map)).set(index,chunk2);
                                    }
                                }


                            }

                        }
                    }

            }
        }
    }
}
