package me.voidxwalker.worldpreview.mixin.access;

import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientChunkManager.ClientChunkMap.class)
public interface ClientChunkMapMixin {

    @Invoker int callIndex(int chunkX, int chunkZ);
    @Invoker WorldChunk callGetChunk(int index);
}
