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
import java.util.List;

@Mixin(GameOptions.class)
public class GameOptionsMixin {

    @Shadow @Mutable public KeyBinding[] keysAll;

    @Inject(method = "<init>",at=@At(value = "INVOKE",target = "Lnet/minecraft/client/options/GameOptions;load()V"))
    public void initCustomHotkey(MinecraftClient client, File optionsFile, CallbackInfo ci){
        WorldPreview.resetKey=new KeyBinding(translate("Reset","key.world_preview.reset"),71, "key.categories.misc");
        keysAll=ArrayUtils.add(keysAll, WorldPreview.resetKey);
        WorldPreview.stopKey=new KeyBinding(translate("Stop","key.world_preview.stop"),73, "key.categories.misc");
        keysAll=ArrayUtils.add(keysAll, WorldPreview.stopKey);
        WorldPreview.cycleChunkMapKey=new KeyBinding(translate("Cycle Chunk Map","key.world_preview.cycle_chunkmap"),72, "key.categories.misc");
        keysAll=ArrayUtils.add(keysAll, WorldPreview.cycleChunkMapKey);
    }

    private String translate(String replacement,String key ){
        TranslatableText t=new TranslatableText(key,replacement);
        if(t.getString().equals(key)){
            return replacement;
        }
        return t.asString();
    }

    @Inject(method = "load",at=@At(value = "INVOKE",target = "Lnet/minecraft/sound/SoundCategory;values()[Lnet/minecraft/sound/SoundCategory;",shift = At.Shift.AFTER),locals = LocalCapture.CAPTURE_FAILSOFT)
    public void loadCustom(CallbackInfo ci, List ignored,CompoundTag compoundTag,  Iterator var22, String string, String string2){
        if ("chunkmapPos".equals(string)) {
            WorldPreview.chunkMapPos=Integer.parseInt(string2);
        }

    }

    @Inject(method = "write",at=@At(value = "INVOKE",target = "Ljava/io/PrintWriter;println(Ljava/lang/String;)V",ordinal = 0),locals = LocalCapture.CAPTURE_FAILSOFT)
    public void loadCustom(CallbackInfo ci, PrintWriter printWriter){
        printWriter.println("chunkmapPos:" + WorldPreview.chunkMapPos);

    }
}
