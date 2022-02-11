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

    @Shadow protected abstract boolean processF3(int key);

    @Inject(method = "onKey",at=@At("HEAD"))
    public void getF3ESCKey(long window, int key, int scancode, int i, int j, CallbackInfo ci){
        if( WorldPreview.inPreview&&window == this.client.getWindow().getHandle()){
            if(i!=0) {
                InputUtil.KeyCode key2 = InputUtil.getKeyCode(key, scancode);
                if (key == 256&& InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 292)&&WorldPreview.showMenu) {
                    WorldPreview.showMenu= false;
                }
                else if (!WorldPreview.showMenu&&key == 256){
                    WorldPreview.showMenu= true;
                }
                boolean bl2 = InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 292) && this.processF3(key);
                if (bl2) {
                    KeyBinding.setKeyPressed(key2, false);
                }
                else {
                    KeyBinding.setKeyPressed(key2, true);
                    KeyBinding.onKeyPressed(key2);
                }
            }
        }
    }
}
