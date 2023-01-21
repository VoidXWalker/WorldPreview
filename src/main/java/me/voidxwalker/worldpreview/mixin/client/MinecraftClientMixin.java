package me.voidxwalker.worldpreview.mixin.client;

import com.google.common.util.concurrent.ListenableFuture;
import me.voidxwalker.worldpreview.AtumInterface;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.command.Console;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.level.LevelInfo;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

   // @Shadow protected abstract void render(boolean tick);

    @Shadow private @Nullable IntegratedServer server;

    @Shadow @Nullable public Entity cameraEntity;

    @Shadow private SoundManager soundManager;

    @Shadow private ClientConnection clientConnection;

    @Shadow public ClientWorld world;

    @Shadow public abstract void openScreen(Screen screen);

    @Shadow private boolean connectedToRealms;

    @Shadow public abstract void connect(ClientWorld world);

    @Shadow public boolean skipGameRender;

    @Shadow public ClientPlayerInteractionManager interactionManager;

    @Shadow protected abstract void handleBlockBreaking(boolean bl);

    @Shadow public Screen currentScreen;

    @Shadow public GameOptions options;

    @Shadow public boolean focused;

    @Shadow public abstract ListenableFuture<Object> submit(Runnable task);

    @Shadow public abstract void updateDisplay();

    @Redirect(method = "startIntegratedServer", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;sleep(J)V"))
    private void cancelSleep(long l) {

    }

    @Inject(method = "startIntegratedServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/integrated/IntegratedServer;isLoading()Z", shift = At.Shift.AFTER), cancellable = true)
    private void worldpreview_onHotKeyPressed(CallbackInfo ci) {
        int resetKeyCode = WorldPreview.resetKey.getCode();
        int freezeKeyCode = WorldPreview.freezeKey.getCode();
        if (WorldPreview.loadedSpawn && (Keyboard.isKeyDown(resetKeyCode) || WorldPreview.kill == 1)) {
            this.updateDisplay();
            WorldPreview.log("Leaving world generation");
            WorldPreview.kill = 1;
            while(WorldPreview.kill == 1) {
                Thread.yield();
            }
            this.server = null;
            this.connect((ClientWorld) null);
            if (WorldPreview.HAS_ATUM) {
                AtumInterface.atumReset();
                MinecraftClient.getInstance().openScreen(new TitleScreen());
            }
            ci.cancel();
        } else if (Keyboard.isKeyDown(freezeKeyCode) && WorldPreview.inPreview && WorldPreview.loadedSpawn && WorldPreview.canFreeze && !WorldPreview.freezePreview) {
            WorldPreview.log("Preview frozen with hotkey.");
            WorldPreview.freezePreview = true;
        }
    }


    @Inject(method="startIntegratedServer",at=@At(value = "HEAD"))
    public void isExistingWorld(String name, String displayName, LevelInfo levelInfo, CallbackInfo ci){
        WorldPreview.existingWorld = levelInfo == null;
    }

    @Inject(method = "connect(Lnet/minecraft/client/world/ClientWorld;Ljava/lang/String;)V",at=@At(value = "HEAD"))
    public void reset(ClientWorld world, String loadingMessage, CallbackInfo ci){
        synchronized (WorldPreview.lock){
            WorldPreview.init();
        }
    }

    @Inject(method = "getCameraEntity", at = @At("HEAD"), cancellable = true)
    private void getWorldPreviewPlayer(CallbackInfoReturnable<Entity> cir) {
        if (this.currentScreen instanceof TitleScreen) {
            cir.setReturnValue(WorldPreview.player);
        }
    }
}
