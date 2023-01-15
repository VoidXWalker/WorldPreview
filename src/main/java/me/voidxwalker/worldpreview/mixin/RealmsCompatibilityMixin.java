package me.voidxwalker.worldpreview.mixin;

import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;

@Mixin(RealmsNotificationsScreen.class)
public class RealmsCompatibilityMixin {
    /**
    Just some fabric silliness, the game is looking for a method that doesn't exist so we define a stub
     **/
    private void buttonsClear() {

    }
}
