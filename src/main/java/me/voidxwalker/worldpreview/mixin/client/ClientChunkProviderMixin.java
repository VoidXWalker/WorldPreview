package me.voidxwalker.worldpreview.mixin.client;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ClientChunkProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mixin(ClientChunkProvider.class)
public class ClientChunkProviderMixin {
    @Shadow private List<Chunk> chunks;

    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/world/chunk/ClientChunkProvider;chunks:Ljava/util/List;"))
    private void makeCOWAL(ClientChunkProvider instance, List<Chunk> value) {
        this.chunks = new CopyOnWriteArrayList<>();
    }
}
