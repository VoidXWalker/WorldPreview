package me.voidxwalker.worldpreview.mixin;

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
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.integrated.IntegratedServer;
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

    @Shadow protected abstract void reset(Screen screen);

    @Shadow @Nullable public ClientWorld world;
    @Shadow @Nullable public Screen currentScreen;
    @Shadow @Final public Mouse mouse;

    @Shadow public abstract Window getWindow();

    @Mutable
    @Shadow @Final public WorldRenderer worldRenderer;
    @Shadow @Final private BufferBuilderStorage bufferBuilders;
    private int cycleCooldown;
    @Inject(method = "isFabulousGraphicsOrBetter",at = @At(value = "RETURN"),cancellable = true)
    private static void stopFabulous(CallbackInfoReturnable<Boolean> cir){
        if(MinecraftClient.getInstance().currentScreen instanceof LevelLoadingScreen&&MinecraftClient.getInstance().world==null){
            cir.setReturnValue(false);
        }
    }
    @Inject(method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",at=@At(value = "INVOKE",shift = At.Shift.AFTER,target = "Lnet/minecraft/server/integrated/IntegratedServer;isLoading()Z"),cancellable = true)
    public void onHotKeyPressed( CallbackInfo ci){
        if(WorldPreview.freezePreview){
            int i = (int)(this.mouse.getX() * (double)this.getWindow().getScaledWidth() / (double)this.getWindow().getWidth());
            int j = (int)(this.mouse.getY() * (double)this.getWindow().getScaledHeight() / (double)this.getWindow().getHeight());

            this.currentScreen.render(new MatrixStack(),i,j,1);
        }
        if(WorldPreview.inPreview){

            cycleCooldown++;

            if(WorldPreview.stopKey.wasPressed()&&!WorldPreview.stop){

                WorldPreview.inPreview=false;
                WorldPreview.stop=true;
                WorldPreview.log(Level.INFO,"Hiding Preview");
            }

            if(WorldPreview.cycleChunkMapKey.wasPressed()&&cycleCooldown>10&&!WorldPreview.stop){
                cycleCooldown=0;
                WorldPreview.chunkMapPos= WorldPreview.chunkMapPos<5? WorldPreview.chunkMapPos+1:1;
            }
        }
         if(currentScreen instanceof LevelLoadingScreen){
            if(WorldPreview.resetKey.wasPressed()|| WorldPreview.kill==-1){

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

                WorldPreview.inPreview=!WorldPreview.inPreview;
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
    public void isExistingWorld(String worldName, CallbackInfo ci){
        WorldPreview.existingWorld=true;
    }
    @Redirect(method="joinWorld",at=@At(value="INVOKE",target="Lnet/minecraft/client/MinecraftClient;reset(Lnet/minecraft/client/gui/screen/Screen;)V"))
    public void smoothTransition(MinecraftClient instance, Screen screen){
        if(!WorldPreview.stop){
            this.profiler.push("forcedTick");
            //this.soundManager.stopAll();
            this.cameraEntity = null;
            this.connection = null;
            this.render(false);
            this.profiler.pop();
        }
        else {
            this.reset(screen);
        }

    }
    //sodium
    @Inject(method="<init>",at=@At(value = "TAIL"))
    public void createWorldRenderer(RunArgs args, CallbackInfo ci){
        WorldPreview.worldRenderer=new WorldRenderer(MinecraftClient.getInstance(), new BufferBuilderStorage());
        this.worldRenderer = new WorldRenderer((MinecraftClient) (Object)this, this.bufferBuilders);
    }
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V",at=@At(value = "HEAD"))
    public void reset(Screen screen, CallbackInfo ci){
        synchronized (WorldPreview.lock){
            WorldPreview.world=null;
            WorldPreview.player=null;
            WorldPreview.clientWord=null;
            WorldPreview.camera=null;
            if(WorldPreview.worldRenderer!=null){
                ((OldSodiumCompatibility)WorldPreview.worldRenderer).setWorldSafe(null);
            }
            cycleCooldown=0;
        }

    }
}
