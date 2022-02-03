package me.voidxwalker.worldpreview.mixin;

import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey",at=@At("HEAD"))
    public void getF3ESCKey(long window, int key, int scancode, int i, int j, CallbackInfo ci){
        if( WorldPreview.inPreview&&window == this.client.window.getHandle()){
            if(i!=0) {
                if (key == 256&& InputUtil.isKeyPressed(MinecraftClient.getInstance().window.getHandle(), 292)&&WorldPreview.showMenu) {
                    WorldPreview.showMenu= false;
                }
                else if (!WorldPreview.showMenu&&key == 256){
                    WorldPreview.showMenu= true;
                }
            }
        }
    }
}
