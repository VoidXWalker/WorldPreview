package me.voidxwalker.worldpreview.mixin;

import com.mojang.authlib.GameProfile;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {
    public ServerPlayerEntityMixin(World world, BlockPos blockPos, GameProfile gameProfile) {
        super(world, blockPos, gameProfile);
    }
    @Redirect(method = "moveToSpawn", at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I"))
    private int worldpreview_setSpawnPos(Random defaultRandom, int k){
        if(WorldPreview.spawnPos != null){
            int value = WorldPreview.playerSpawn;
            WorldPreview.spawnPos=null;
            return value;
        }
        return defaultRandom.nextInt(k);
    }
}