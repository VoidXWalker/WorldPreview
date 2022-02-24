package me.voidxwalker.worldpreview.mixin.client;

import me.voidxwalker.worldpreview.OldSodiumCompatibility;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.profiler.Profiler;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
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
    @Shadow private @Nullable ClientConnection connection;
    @Shadow @Final private SoundManager soundManager;
    @Shadow private Profiler profiler;

    @Shadow @Nullable public ClientWorld world;
    @Shadow @Nullable public Screen currentScreen;
    @Shadow @Final public Mouse mouse;

    @Shadow public abstract Window getWindow();

    @Mutable
    @Shadow @Final public WorldRenderer worldRenderer;
    @Shadow @Final private BufferBuilderStorage bufferBuilders;
    private int worldpreview_cycleCooldown;
    @Inject(method = "isFabulousGraphicsOrBetter",at = @At(value = "RETURN"),cancellable = true)
    private static void worldpreview_stopFabulous(CallbackInfoReturnable<Boolean> cir){
        if(MinecraftClient.getInstance().currentScreen instanceof LevelLoadingScreen&&MinecraftClient.getInstance().world==null){
            cir.setReturnValue(false);
        }
    }
    @Inject(method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",at=@At(value = "INVOKE",shift = At.Shift.AFTER,target = "Lnet/minecraft/server/integrated/IntegratedServer;isLoading()Z"),cancellable = true)
    public void worldpreview_onHotKeyPressed( CallbackInfo ci){
        if(WorldPreview.inPreview){
            worldpreview_cycleCooldown++;
            if(WorldPreview.cycleChunkMapKey.wasPressed()&&worldpreview_cycleCooldown>10&&!WorldPreview.freezePreview){
                worldpreview_cycleCooldown=0;
                WorldPreview.chunkMapPos= WorldPreview.chunkMapPos<5? WorldPreview.chunkMapPos+1:1;
            }
            if(WorldPreview.resetKey.wasPressed()|| WorldPreview.kill==-1){
                if(WorldPreview.resetKey.wasPressed()){
                    soundManager.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                WorldPreview.log(Level.INFO,"Leaving world generation");
                WorldPreview.kill = 1;
                while(WorldPreview.inPreview){
                    Thread.yield();
                }
                this.server.shutdown();
                MinecraftClient.getInstance().disconnect();
                WorldPreview.kill=0;
                MinecraftClient.getInstance().openScreen(new TitleScreen());
                ci.cancel();
            }
            if(WorldPreview.freezeKey.wasPressed()){
                WorldPreview.freezePreview=!WorldPreview.freezePreview;
                if(WorldPreview.freezePreview){
                    WorldPreview.log(Level.INFO,"Freezing Preview"); // insert anchiale joke
                }
                else {
                    WorldPreview.log(Level.INFO,"Unfreezing Preview");
                }
            }
        }
    }

    @Inject(method="startIntegratedServer(Ljava/lang/String;)V",at=@At(value = "HEAD"))
    public void worldpreview_isExistingWorld(String worldName, CallbackInfo ci){
        WorldPreview.existingWorld=true;
    }

    @Redirect(method="joinWorld",at=@At(value="INVOKE",target="Lnet/minecraft/client/MinecraftClient;reset(Lnet/minecraft/client/gui/screen/Screen;)V"))
    public void worldpreview_smoothTransition(MinecraftClient instance, Screen screen){
            this.profiler.push("forcedTick");
            //this.soundManager.stopAll();
            this.cameraEntity = null;
            this.connection = null;
            this.render(false);
            this.profiler.pop();

    }
    @Redirect(method = "reset",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/sound/SoundManager;stopAll()V"))
    public void worldpreview_stopStopSound(SoundManager instance){
        if(this.currentScreen instanceof LevelLoadingScreen){
            return;
        }
        instance.stopAll();
    }
    //sodium
    @Inject(method="<init>",at=@At(value = "TAIL"))
    public void worldpreview_createWorldRenderer(RunArgs args, CallbackInfo ci){
        WorldPreview.worldRenderer=new WorldRenderer(MinecraftClient.getInstance(), new BufferBuilderStorage());
        this.worldRenderer = new WorldRenderer((MinecraftClient) (Object)this, this.bufferBuilders);
    }
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V",at=@At(value = "HEAD"))
    public void worldpreview_reset(Screen screen, CallbackInfo ci){
        synchronized (WorldPreview.lock){
            WorldPreview.world=null;
            WorldPreview.player=null;
            WorldPreview.clientWord=null;
            WorldPreview.camera=null;
            if(WorldPreview.worldRenderer!=null){
                ((OldSodiumCompatibility)WorldPreview.worldRenderer).worldpreview_setWorldSafe(null);
            }
            worldpreview_cycleCooldown=0;
        }
    }
}
