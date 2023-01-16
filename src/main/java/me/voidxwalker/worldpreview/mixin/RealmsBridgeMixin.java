package me.voidxwalker.worldpreview.mixin;

import net.minecraft.realms.RealmsBridge;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RealmsBridge.class)
/**
 * to get rid of those annoying logs that clog up the console
 */
public class RealmsBridgeMixin {

    @Redirect(method = "switchToRealms", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Throwable;)V"))
    private void stopLog(Logger instance, String s, Throwable throwable) {}

    @Redirect(method = "getNotificationScreen", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Throwable;)V"))
    private void stopLog2(Logger instance, String s, Throwable throwable) {}
}
