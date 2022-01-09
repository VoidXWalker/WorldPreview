package me.voidxwalker.worldpreview.mixin;

import me.voidxwalker.worldpreview.Main;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
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
        if(client.currentScreen instanceof LevelLoadingScreen&& Main.camera!=null&&window == this.client.getWindow().getHandle()){
            if(i!=0) {
                if (key == 256) {
                    boolean bl3 = InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 292);
                    if(bl3){
                        Main.showMenu=false;
                    }
                }
                InputUtil.Key key2 = InputUtil.fromKeyCode(key, scancode);
                if (key == 256&& InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 292)) {
                    Main.showMenu=false;
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
