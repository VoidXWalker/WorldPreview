package me.voidxwalker.worldpreview.mixin;

import me.voidxwalker.worldpreview.Main;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Redirect(method="initWidgets",at=@At(value="INVOKE",target = "Lnet/minecraft/client/gui/screen/GameMenuScreen;addButton(Lnet/minecraft/client/gui/widget/AbstractButtonWidget;)Lnet/minecraft/client/gui/widget/AbstractButtonWidget;",ordinal = 0))
    public AbstractButtonWidget stopUnpause(GameMenuScreen instance, AbstractButtonWidget abstractButtonWidget){

        return this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 24 + -16, 204, 20, new TranslatableText("menu.returnToGame"), buttonWidget -> {
            if(!Main.forcedPaused){
                this.client.openScreen(null);
                this.client.mouse.lockCursor();
            }

        }));
    }
}
