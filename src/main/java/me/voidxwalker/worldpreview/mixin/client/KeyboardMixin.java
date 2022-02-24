package me.voidxwalker.worldpreview.mixin.client;

import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.mixin.access.KeyBindingMixin;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.option.KeyBinding;
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
    public void worldpreview_getF3ESCKey(long window, int key, int scancode, int i, int j, CallbackInfo ci){
        if( client.currentScreen instanceof LevelLoadingScreen &&window == this.client.getWindow().getHandle()){
            if(i!=0) {
                InputUtil.Key key2 = InputUtil.fromKeyCode(key, scancode);
                if (key == 256&& InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 292)&&WorldPreview.showMenu) {
                    WorldPreview.showMenu= false;
                }
                else if (!WorldPreview.showMenu&&key == 256){
                    WorldPreview.showMenu= true;
                }
                boolean bl2 = InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 292);
                KeyBinding k = KeyBindingMixin.getKEY_TO_BINDINGS().get(key2);
                if(k!=null&&(WorldPreview.resetKey.compareTo(k)==0||WorldPreview.cycleChunkMapKey.compareTo(k)==0||WorldPreview.freezeKey.compareTo(k)==0)){
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
}
