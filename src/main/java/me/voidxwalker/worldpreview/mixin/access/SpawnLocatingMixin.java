package me.voidxwalker.worldpreview.mixin.access;

import net.minecraft.server.network.SpawnLocating;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SpawnLocating.class)
public interface SpawnLocatingMixin {
    @Invoker
    static BlockPos callFindOverworldSpawn(ServerWorld world, int x, int z) {
       throw new IllegalStateException();
    }
}
