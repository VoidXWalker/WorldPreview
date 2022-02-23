package me.voidxwalker.worldpreview.mixin;

import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.TranslatableText;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.io.PrintWriter;
import java.util.Iterator;

@Mixin(GameOptions.class)
public class GameOptionsMixin {

    @Shadow @Mutable public KeyBinding[] keysAll;

    @Inject(method = "<init>",at=@At(value = "INVOKE",target = "Lnet/minecraft/client/options/GameOptions;load()V"))
    public void worldpreview_initCustomHotkey(MinecraftClient client, File optionsFile, CallbackInfo ci){
        WorldPreview.resetKey=new KeyBinding("key.world_preview.reset",71, "key.categories.world_preview");
        keysAll=ArrayUtils.add(keysAll, WorldPreview.resetKey);
        WorldPreview.cycleChunkMapKey=new KeyBinding("key.world_preview.cycle_chunkmap",72, "key.categories.world_preview");
        keysAll=ArrayUtils.add(keysAll, WorldPreview.cycleChunkMapKey);
        WorldPreview.freezeKey=new KeyBinding("key.world_preview.freeze",74, "key.categories.world_preview");
        keysAll=ArrayUtils.add(keysAll, WorldPreview.freezeKey);
    }

    @Inject(method = "load",at=@At(value = "INVOKE",target = "Lnet/minecraft/sound/SoundCategory;values()[Lnet/minecraft/sound/SoundCategory;",shift = At.Shift.AFTER),locals = LocalCapture.CAPTURE_FAILSOFT)
    public void worldpreview_loadCustom(CallbackInfo ci, CompoundTag compoundTag,  CompoundTag compoundTag2,Iterator var22,String string,String string2){
        if ("chunkmapPos".equals(string)) {
            WorldPreview.chunkMapPos=Integer.parseInt(string2);
        }

    }

    @Inject(method = "write",at=@At(value = "INVOKE",target = "Ljava/io/PrintWriter;println(Ljava/lang/String;)V",ordinal = 0),locals = LocalCapture.CAPTURE_FAILSOFT)
    public void loadCustom(CallbackInfo ci, PrintWriter printWriter){
        printWriter.println("chunkmapPos:" + WorldPreview.chunkMapPos);

    }
}
