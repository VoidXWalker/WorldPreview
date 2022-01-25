package me.voidxwalker.worldpreview.mixin;

import me.voidxwalker.worldpreview.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.entity.Entity;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
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

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow protected abstract void render(boolean tick);

    @Shadow private @Nullable IntegratedServer server;

    @Shadow @Nullable public Entity cameraEntity;
    @Shadow private @Nullable ClientConnection connection;
    @Shadow @Final private SoundManager soundManager;
    @Shadow private Profiler profiler;

    @Shadow protected abstract void reset(Screen screen);

    @Shadow public @Nullable abstract IntegratedServer getServer();

    private int cycleCooldown;

    @Inject(method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",at=@At(value = "INVOKE",shift = At.Shift.AFTER,target = "Lnet/minecraft/server/integrated/IntegratedServer;isLoading()Z"),cancellable = true)
    public void onHotKeyPressed( CallbackInfo ci){
        if(Main.inPreview){

            cycleCooldown++;
            if(Main.resetKey.wasPressed()||Main.kill==-1){

                Main.log(Level.INFO,"Leaving world generation");
                Main.kill = 1;
                while(Main.inPreview){
                    Thread.yield();

                }
                this.server.shutdown();
                MinecraftClient.getInstance().disconnect();
                Main.kill=0;
                MinecraftClient.getInstance().openScreen(new TitleScreen());
                ci.cancel();
            }
            if(Main.stopKey.wasPressed()&&!Main.stop){

                Main.inPreview=false;
                Main.stop=true;
                Main.log(Level.INFO,"Stopping Preview");
            }
            if(Main.cycleChunkMapKey.wasPressed()&&cycleCooldown>10&&!Main.stop){
                cycleCooldown=0;
                Main.chunkMapPos=Main.chunkMapPos<5?Main.chunkMapPos+1:1;
            }
        }
    }

    @Inject(method="startIntegratedServer(Ljava/lang/String;)V",at=@At(value = "HEAD"))
    public void isExistingWorld(String worldName, CallbackInfo ci){
        Main.existingWorld=true;
    }
    @Redirect(method="joinWorld",at=@At(value="INVOKE",target="Lnet/minecraft/client/MinecraftClient;reset(Lnet/minecraft/client/gui/screen/Screen;)V"))
    public void smoothTransition(MinecraftClient instance, Screen screen){
        if(!Main.stop){
            this.profiler.push("forcedTick");
            this.soundManager.stopAll();
            this.cameraEntity = null;
            this.connection = null;
            this.render(false);
            this.profiler.pop();
        }
        else {
            this.reset(screen);
        }

    }
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V",at=@At(value = "HEAD"))
    public void reset(Screen screen, CallbackInfo ci){
        synchronized (Main.lock){
            Main.world=null;
            Main.player=null;
            Main.clientWord=null;
            Main.camera=null;
            if(Main.worldRenderer!=null){
                Main.worldRenderer.loadWorld(null);
            }
            cycleCooldown=0;
        }

    }
}
