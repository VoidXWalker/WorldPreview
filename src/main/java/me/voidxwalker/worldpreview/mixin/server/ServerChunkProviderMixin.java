package me.voidxwalker.worldpreview.mixin.server;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ServerChunkProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mixin(ServerChunkProvider.class)
public class ServerChunkProviderMixin {
    @Shadow private List<Chunk> chunks;

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/world/chunk/ServerChunkProvider;chunks:Ljava/util/List;"))
    private void makeCOWAL(ServerChunkProvider instance, List<Chunk> value) {
        this.chunks = new CopyOnWriteArrayList<>();
    }
}
