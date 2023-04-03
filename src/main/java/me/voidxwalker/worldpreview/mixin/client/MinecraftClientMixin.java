package me.voidxwalker.worldpreview.mixin.client;

import me.voidxwalker.worldpreview.OldSodiumCompatibility;
import me.voidxwalker.worldpreview.StateOutputHelper;
import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.mixin.access.WorldRendererMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.entity.Entity;
import net.minecraft.network.ClientConnection;
import net.minecraft.resource.ReloadableResourceManager;
import net.minecraft.resource.ResourceReloadListener;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.storage.LevelStorage;
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

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow protected abstract void render(boolean tick);

    @Shadow private @Nullable IntegratedServer server;

    @Shadow @Nullable public ClientPlayerEntity player;
    @Shadow @Nullable public Entity cameraEntity;
    @Shadow private @Nullable ClientConnection connection;
    @Shadow @Final private SoundManager soundManager;

    @Shadow protected abstract void reset(Screen screen);

    @Shadow public abstract LevelStorage getLevelStorage();

    @Mutable
    @Shadow @Final public WorldRenderer worldRenderer;
    @Shadow @Final private BufferBuilderStorage bufferBuilders;
    @Shadow @Nullable public Screen currentScreen;
    private int worldpreview_cycleCooldown;

    @Inject(method = "startIntegratedServer",at=@At(value = "INVOKE",shift = At.Shift.AFTER,target = "Lnet/minecraft/server/integrated/IntegratedServer;isLoading()Z"),cancellable = true)
    public void worldpreview_onHotKeyPressed( CallbackInfo ci){
        if(WorldPreview.inPreview){
            worldpreview_cycleCooldown++;
            if(WorldPreview.cycleChunkMapKey.wasPressed()&&worldpreview_cycleCooldown>10&&!WorldPreview.freezePreview){
                worldpreview_cycleCooldown=0;
                WorldPreview.chunkMapPos= WorldPreview.chunkMapPos<5? WorldPreview.chunkMapPos+1:1;
            }
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

    @Inject(method="startIntegratedServer",at=@At(value = "HEAD"))
    public void isExistingWorld(String name, String displayName, LevelInfo levelInfo, CallbackInfo ci){
        WorldPreview.existingWorld=this.getLevelStorage().levelExists(name);
    }
    @Redirect(method="reset",at=@At(value="INVOKE",target="Lnet/minecraft/client/MinecraftClient;openScreen(Lnet/minecraft/client/gui/screen/Screen;)V"))
    public void worldpreview_smoothTransition(MinecraftClient instance, Screen screen){
        if(this.currentScreen instanceof LevelLoadingScreen &&  ((WorldRendererMixin)WorldPreview.worldRenderer).getWorld()!=null&&WorldPreview.world!=null&& WorldPreview.clientWord!=null&&WorldPreview.player!=null){
            return;
        }
        instance.openScreen(screen);

    }
    //sodium
    @Redirect(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/resource/ReloadableResourceManager;registerListener(Lnet/minecraft/resource/ResourceReloadListener;)V",ordinal = 11))
    public void worldpreview_createWorldRenderer(ReloadableResourceManager instance, ResourceReloadListener resourceReloadListener){
        WorldPreview.worldRenderer=new WorldRenderer(MinecraftClient.getInstance(), new BufferBuilderStorage());
        ((OldSodiumCompatibility)WorldPreview.worldRenderer).setPreviewRenderer();
        this.worldRenderer = new WorldRenderer((MinecraftClient) (Object)this, this.bufferBuilders);
        instance.registerListener(worldRenderer);

    }
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V",at=@At(value = "HEAD"))
    public void reset(Screen screen, CallbackInfo ci){
        synchronized (WorldPreview.lock){
            WorldPreview.world=null;
            WorldPreview.player=null;
            WorldPreview.clientWord=null;
            WorldPreview.camera=null;
            WorldPreview.calculatedSpawn=false;
            if(WorldPreview.worldRenderer!=null){
                ((OldSodiumCompatibility)WorldPreview.worldRenderer).worldpreview_setWorldSafe(null);
            }
            worldpreview_cycleCooldown=0;
        }

    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("TAIL"))
    private void worldpreview_outputWaitingState(Screen screen, CallbackInfo info) {
        // We do this inject after this.player is set to null in the disconnect method.
        // This is because the inworld state output depends on the player being non-null,
        // so it makes more sense to set the state for exiting after the player becomes null.

        // While disconnect is intended for leaving a world, it may also occur before the first world creation,
        // hence the output "waiting" as opposed to "exiting"
        StateOutputHelper.outputState("waiting");
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void worldpreview_outputInWorldState(CallbackInfo info) {
        // If there is no player, there is no world to be in
        if (this.player == null) return;
        if (this.currentScreen == null) {
            StateOutputHelper.outputState("inworld,unpaused");
        } else if (this.currentScreen.isPauseScreen()) {
            StateOutputHelper.outputState("inworld,paused");
        } else {
            StateOutputHelper.outputState("inworld,gamescreenopen");
        }
    }
}
