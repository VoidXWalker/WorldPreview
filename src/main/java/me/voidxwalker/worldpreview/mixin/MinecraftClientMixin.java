package me.voidxwalker.worldpreview.mixin;

import com.mojang.datafixers.util.Function4;
import me.voidxwalker.worldpreview.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.entity.Entity;
import net.minecraft.network.ClientConnection;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryTracker;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow private @Nullable ClientConnection connection;

    @Shadow protected abstract void render(boolean tick);

    @Shadow private Profiler profiler;

    @Shadow @Nullable public Entity cameraEntity;

    @Shadow @Final private SoundManager soundManager;

    @Shadow private @Nullable IntegratedServer server;

    @Redirect(method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",at=@At(value = "INVOKE",target = "Lnet/minecraft/server/integrated/IntegratedServer;isLoading()Z"))
    public boolean kill(IntegratedServer instance){
        if(instance==null){
            return true;
        }
        return instance.isLoading();
    }
    @Inject(method="startIntegratedServer(Ljava/lang/String;)V",at=@At(value = "HEAD"))
    public void isExistingWorld(String worldName, CallbackInfo ci){
        Main.existingWorld=true;
    }
    @Inject(method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",at=@At(value = "INVOKE",target = "Lnet/minecraft/util/profiler/Profiler;pop()V"),cancellable = true)
    public void kill2(CallbackInfo ci){
        if(this.server==null){
            ci.cancel();
        }
    }
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V",at=@At(value = "HEAD"))
    public void reset(Screen screen, CallbackInfo ci){
        Main.world=null;
        Main.player=null;
        Main.clientWord=null;
        Main.camera=null;
        Main.worldRenderer=null;
    }
    @Redirect(method="joinWorld",at=@At(value="INVOKE",target="Lnet/minecraft/client/MinecraftClient;reset(Lnet/minecraft/client/gui/screen/Screen;)V"))
    public void smoothTransition(MinecraftClient instance, Screen screen){
        Main.stopButton=true;
        this.profiler.push("forcedTick");
        this.soundManager.stopAll();
        this.cameraEntity = null;
        this.connection = null;
        this.render(false);
        this.profiler.pop();
    }
}
