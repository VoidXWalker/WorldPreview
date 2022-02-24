package me.voidxwalker.worldpreview.mixin.access;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(KeyBinding.class)
public interface KeyBindingMixin {
    @Accessor
    static Map<InputUtil.Key, KeyBinding> getKEY_TO_BINDINGS() {
        throw new AssertionError();
    }
    @Accessor("CATEGORY_ORDER_MAP")
    static Map<String, Integer> invokeGetCategoryMap() {
        throw new AssertionError();
    }
}