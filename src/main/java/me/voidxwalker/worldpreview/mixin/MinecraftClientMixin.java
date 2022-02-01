package me.voidxwalker.worldpreview.mixin;

import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.entity.Entity;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.profiler.Profiler;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow protected abstract void render(boolean tick);

    @Shadow private @Nullable IntegratedServer server;

    @Shadow @Nullable public Entity cameraEntity;
    @Shadow @Final private SoundManager soundManager;
    @Shadow private Profiler profiler;

    @Shadow protected abstract void reset(Screen screen);

    @Shadow public @Nullable abstract IntegratedServer getServer();

    @Shadow public abstract void setScreen(@Nullable Screen screen);

    @Shadow private @Nullable ClientConnection integratedServerConnection;
    private int cycleCooldown;
    @Inject(method = "isFabulousGraphicsOrBetter",at = @At(value = "RETURN"),cancellable = true)
    private static void stopFabulous(CallbackInfoReturnable<Boolean> cir){
        if(MinecraftClient.getInstance().currentScreen instanceof LevelLoadingScreen &&MinecraftClient.getInstance().world==null){
            cir.setReturnValue(false);
        }
    }
    @Inject(method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/DynamicRegistryManager$Impl;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",at=@At(value = "INVOKE",shift = At.Shift.AFTER,target = "Lnet/minecraft/server/integrated/IntegratedServer;isLoading()Z"),cancellable = true)
    public void onHotKeyPressed( CallbackInfo ci){
        if(WorldPreview.inPreview){

            cycleCooldown++;
            if(WorldPreview.resetKey.wasPressed()|| WorldPreview.kill==-1){

                WorldPreview.log(Level.INFO,"Leaving world generation");
                WorldPreview.kill = 1;
                while(WorldPreview.inPreview){
                    Thread.yield();

                }
                this.server.shutdown();
                MinecraftClient.getInstance().disconnect();
                WorldPreview.kill=0;
                MinecraftClient.getInstance().setScreen(new TitleScreen());
                ci.cancel();
            }
            if(WorldPreview.stopKey.wasPressed()&&!WorldPreview.stop){

                WorldPreview.inPreview=false;
                WorldPreview.stop=true;
                WorldPreview.log(Level.INFO,"Stopping Preview");
            }
            if(WorldPreview.cycleChunkMapKey.wasPressed()&&cycleCooldown>10&&!WorldPreview.stop){
                cycleCooldown=0;
                WorldPreview.chunkMapPos= WorldPreview.chunkMapPos<5? WorldPreview.chunkMapPos+1:1;
            }
        }
    }

    @Inject(method="startIntegratedServer(Ljava/lang/String;)V",at=@At(value = "HEAD"))
    public void isExistingWorld(String worldName, CallbackInfo ci){
        WorldPreview.existingWorld=true;
    }
    @Redirect(method="joinWorld",at=@At(value="INVOKE",target="Lnet/minecraft/client/MinecraftClient;reset(Lnet/minecraft/client/gui/screen/Screen;)V"))
    public void smoothTransition(MinecraftClient instance, Screen screen){
        if(!WorldPreview.stop){
            this.profiler.push("forcedTick");
            this.soundManager.stopAll();
            this.cameraEntity = null;
            this.integratedServerConnection = null;
            this.render(false);
            this.profiler.pop();
        }
        else {
            this.reset(screen);
        }

    }
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V",at=@At(value = "HEAD"))
    public void reset(Screen screen, CallbackInfo ci){
        synchronized (WorldPreview.lock){
            WorldPreview.world=null;
            WorldPreview.player=null;
            WorldPreview.clientWorld=null;
            WorldPreview.camera=null;
            if(WorldPreview.worldRenderer!=null){
                WorldPreview.worldRenderer.setWorld(null);
            }
            cycleCooldown=0;
        }

    }
}
