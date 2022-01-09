package me.voidxwalker.worldpreview.mixin.access;

import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientChunkManager.ClientChunkMap.class)
public interface ClientChunkMapMixin {
    @Invoker void callSet(int index, @Nullable WorldChunk chunk);
    @Invoker int callGetIndex(int chunkX, int chunkZ);
    @Invoker WorldChunk callGetChunk(int index);
}
