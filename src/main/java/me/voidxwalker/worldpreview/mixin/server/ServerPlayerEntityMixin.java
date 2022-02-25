package me.voidxwalker.worldpreview.mixin.server;

import com.mojang.authlib.GameProfile;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {

    public ServerPlayerEntityMixin(World world, GameProfile profile) {
        super(world, profile);
    }

    @Redirect(method = "method_14245", at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I"))
    private int setSpawnPos(Random defaultRandom, int k){
        if(MinecraftClient.getInstance().currentScreen instanceof LevelLoadingScreen){
            System.out.println(1);
            int value = WorldPreview.playerSpawn;
            WorldPreview.spawnPos=null;
            return value;
        }
        return defaultRandom.nextInt(k);
    }
}