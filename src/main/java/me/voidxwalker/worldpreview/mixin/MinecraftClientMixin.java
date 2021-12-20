package me.voidxwalker.worldpreview.mixin;

import me.voidxwalker.worldpreview.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin<V> {

    @Shadow private boolean paused;

    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;paused:Z", opcode = Opcodes.GETFIELD,ordinal = 2))
    private boolean renderWhilePaused(MinecraftClient instance) {
        if(Main.forcedPaused){
            return false;
        }
        else {
            return paused;
        }
    }
    @Redirect(method = "openScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Mouse;lockCursor()V"))
    private void preventMouseMovement(Mouse instance) {
        if(!Main.forcedPaused){
            instance.lockCursor();
        }
    }

}
