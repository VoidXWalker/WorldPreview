package me.voidxwalker.worldpreview.mixin;

import me.voidxwalker.worldpreview.Main;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

  @ModifyConstant(method = "prepareStartRegion", constant = @Constant(intValue = 441))
  private int lowerChunksToWaitFor(int value) {
    return 1;
  }

  @Redirect(method = "prepareStartRegion", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkManager;getTotalChunksLoadedCount()I"))
  private int skipInitialChunkLoads(ServerChunkManager serverChunkManager) {
    Main.forcedPaused=true;
    return 1;
  }

  @Inject(method = "prepareStartRegion", at = @At("TAIL"))
  private void save(CallbackInfo info) {
    ((MinecraftServer) (Object) this).save(false,false,false);
  }
}
