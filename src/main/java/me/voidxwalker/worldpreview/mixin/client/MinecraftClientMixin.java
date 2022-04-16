package me.voidxwalker.worldpreview.mixin.client;

import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.integrated.IntegratedServer;
//import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.storage.LevelStorage;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Redirect(method = "startGame", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;sleep(J)V"))
    private void cancelSleep(long l) {

    }

    @Inject(method = "startGame", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/integrated/IntegratedServer;isLoading()Z", shift = At.Shift.AFTER), cancellable = true)
    private void worldpreview_onHotKeyPressed(CallbackInfo ci) {
        int resetKeyCode = WorldPreview.resetKey.getCode();
        int freezeKeyCode = WorldPreview.freezeKey.getCode();
        if (Keyboard.isKeyDown(resetKeyCode)) {
            soundManager.play(PositionedSoundInstance.master(new Identifier("gui.button.press"), 1.0F));
            WorldPreview.log(Level.INFO,"Leaving world generation");
            WorldPreview.kill = 1;
            while(WorldPreview.inPreview){
                Thread.yield();
            }
            this.server.stopServer();
            this.server = null;
            this.connect((ClientWorld) null);
            WorldPreview.kill=0;
            ci.cancel();
        } else if (Keyboard.isKeyDown(freezeKeyCode)) {
            System.out.println("freeze");
        }
    }


    @Inject(method="startGame",at=@At(value = "HEAD"))
    public void isExistingWorld(String name, String displayName, LevelInfo levelInfo, CallbackInfo ci){
        WorldPreview.existingWorld = levelInfo == null;
    }

    @Redirect(method="connect(Lnet/minecraft/client/world/ClientWorld;Ljava/lang/String;)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/sound/SoundManager;stopAll()V"))
    public void smoothTransition(SoundManager instance){
        this.cameraEntity = null;
        //this.skipGameRender = true; // this doesn't work exactly the same as its equivalent in 1.14+, needs further testing
    }


    @Inject(method = "connect(Lnet/minecraft/client/world/ClientWorld;Ljava/lang/String;)V",at=@At(value = "HEAD"))
    public void reset(ClientWorld world, String loadingMessage, CallbackInfo ci){
        synchronized (WorldPreview.lock){
            if (world == null) {
                WorldPreview.world = null;
                WorldPreview.player = null;
                WorldPreview.clientWorld = null;
                //WorldPreview.camera = null; // don't think this is necessary, we'll see when we get to loading screen code
                if (WorldPreview.worldRenderer != null) {
                    WorldPreview.worldRenderer.method_1371((ClientWorld) null);
                }
            }
        }

    }
}
