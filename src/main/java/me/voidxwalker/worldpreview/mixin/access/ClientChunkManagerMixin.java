package me.voidxwalker.worldpreview.mixin.access;

import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientChunkManager.class)
public interface ClientChunkManagerMixin {
    @Accessor ClientChunkManager.ClientChunkMap getChunks();
    @Accessor ClientWorld getWorld();
}
