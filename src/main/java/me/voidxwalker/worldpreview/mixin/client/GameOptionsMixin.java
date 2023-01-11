package me.voidxwalker.worldpreview.mixin.client;

import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;

@Mixin(GameOptions.class)
public class GameOptionsMixin {
    @Shadow public KeyBinding[] keysAll;
    private KeyBinding freezePreviewKey;
    private KeyBinding leavePreviewKey;

    @Inject(method = "<init>(Lnet/minecraft/client/MinecraftClient;Ljava/io/File;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/options/GameOptions;load()V"))
    private void initInject(CallbackInfo ci) {
        this.freezePreviewKey = new KeyBinding("Freeze Preview", 36, "World Preview");
        this.leavePreviewKey = new KeyBinding("Leave Preview", 37, "World Preview");
        ArrayList<KeyBinding> a = new ArrayList<>(Arrays.asList(this.keysAll));
        a.add(this.freezePreviewKey);
        a.add(this.leavePreviewKey);
        this.keysAll = a.toArray(this.keysAll);
        WorldPreview.freezeKey = this.freezePreviewKey;
        WorldPreview.resetKey = this.leavePreviewKey;
    }
}
