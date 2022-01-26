package me.voidxwalker.worldpreview.mixin;

import me.voidxwalker.worldpreview.ChunkSetter;
import me.voidxwalker.worldpreview.mixin.access.ClientChunkManagerMixin;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.atomic.AtomicReferenceArray;

@Mixin(ClientChunkManager.ClientChunkMap.class)
public class ClientChunkMapMixin implements ChunkSetter {
    @Shadow private int field_19143;

    @Shadow @Final private AtomicReferenceArray<WorldChunk> chunks;

    @Override
    public void set(int index, @Nullable WorldChunk chunk) {
        WorldChunk worldChunk = (WorldChunk)this.chunks.getAndSet(index, chunk);
        if (worldChunk != null) {
            --this.field_19143;
            ((ClientChunkManagerMixin)this).getWorld().unloadBlockEntities(worldChunk);
        }

        if (chunk != null) {
            ++this.field_19143;
        }
    }
}
