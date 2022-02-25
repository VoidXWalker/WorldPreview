package me.voidxwalker.worldpreview.mixin.client.render;

import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.world.ClientWorld;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    @Shadow @Final private MinecraftClient client;

    @Redirect(method = "renderBackground",at = @At(value = "FIELD",target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;",opcode = Opcodes.GETFIELD))
    public ClientWorld getCorrectWorld(MinecraftClient instance){
        if(instance.world==null&&instance.currentScreen instanceof LevelLoadingScreen){
            return WorldPreview.clientWord;
        }
        else {
            return instance.world;
        }
    }
    @Redirect(method = "applyFog",at = @At(value = "FIELD",target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;",opcode = Opcodes.GETFIELD))
    public ClientWorld getCorrectWorld2(MinecraftClient instance){
        if(instance.world==null&&instance.currentScreen instanceof LevelLoadingScreen){
            return WorldPreview.clientWord;
        }
        else {
            return instance.world;
        }
    }
    @Redirect(method = "applyFog",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/hud/InGameHud;getBossBarHud()Lnet/minecraft/client/gui/hud/BossBarHud;"))
    public BossBarHud stopInGameHud(InGameHud instance){
        if(instance==null&&client.currentScreen instanceof LevelLoadingScreen){
            return null;
        }
        else {
            return instance.getBossBarHud();
        }
    }
    @Redirect(method = "applyFog",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/render/GameRenderer;getViewDistance()F"))
    public float getCorrectDistance(GameRenderer instance){
        if(instance==null&&client.currentScreen instanceof LevelLoadingScreen){
            return client.options.viewDistance*16;
        }
        else {
            return instance.getViewDistance();
        }
    }
    @Redirect(method = "renderBackground",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/render/GameRenderer;getSkyDarkness(F)F"))
    public float getCorrectSkyDarkness(GameRenderer instance, float tickDelta){
        if(instance==null&&client.currentScreen instanceof LevelLoadingScreen){
            return 0.5F;
        }
        else {
            return instance.getSkyDarkness(tickDelta);
        }
    }
    @Redirect(method = "applyFog",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/hud/BossBarHud;shouldThickenFog()Z"))
    public boolean stopInGameHud2(BossBarHud instance){
        if(instance==null&&client.currentScreen instanceof LevelLoadingScreen){
            return false;
        }
        else {
            return instance.shouldThickenFog();
        }
    }
}
