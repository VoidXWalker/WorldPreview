package me.voidxwalker.worldpreview.mixin.access;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.ArrayList;
import java.util.List;

@Mixin(Screen.class)
public interface ScreenMixin {
    @Accessor
    List<ButtonWidget> getButtons();
}
