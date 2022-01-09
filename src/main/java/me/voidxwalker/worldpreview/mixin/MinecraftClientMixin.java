package me.voidxwalker.worldpreview.mixin;

import me.voidxwalker.worldpreview.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.server.integrated.IntegratedServer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow protected abstract void render(boolean tick);

    @Shadow private @Nullable IntegratedServer server;

    private int cycleCooldown;

    @Inject(method = "startIntegratedServer(Ljava/lang/String;Lnet/minecraft/util/registry/RegistryTracker$Modifiable;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V",at=@At(value = "INVOKE",shift = At.Shift.AFTER,target = "Lnet/minecraft/server/integrated/IntegratedServer;isLoading()Z"),cancellable = true)
    public void onHotKeyPressed( CallbackInfo ci){
        if(Main.camera!=null){
            cycleCooldown++;
            if(Main.resetKey.wasPressed()||Main.kill==-1){
                Main.kill = 1;
                this.server.shutdown();
                MinecraftClient.getInstance().disconnect();
                MinecraftClient.getInstance().openScreen(new TitleScreen());
                ci.cancel();
            }
            if(Main.cycleChunkMapKey.wasPressed()&&cycleCooldown>10){
                cycleCooldown=0;
                Main.chunkMapPos=Main.chunkMapPos<5?Main.chunkMapPos+1:1;
            }
        }
    }

    @Inject(method="startIntegratedServer(Ljava/lang/String;)V",at=@At(value = "HEAD"))
    public void isExistingWorld(String worldName, CallbackInfo ci){
        Main.existingWorld=true;
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V",at=@At(value = "HEAD"))
    public void reset(Screen screen, CallbackInfo ci){
        Main.world=null;
        Main.player=null;
        Main.clientWord=null;
        Main.camera=null;
        Main.worldRenderer=null;
        cycleCooldown=0;
    }
}
