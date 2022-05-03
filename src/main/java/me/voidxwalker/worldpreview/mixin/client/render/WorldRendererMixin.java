package me.voidxwalker.worldpreview.mixin.client.render;


import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
//import net.minecraft.client.gui.screen.LevelLoadingScreen;
//import net.minecraft.client.render.Camera;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.WorldRenderer;
//import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.world.BuiltChunk;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.Dimension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow @Final private MinecraftClient client;

    @Redirect(method = "method_9906",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/world/BuiltChunk;method_10162(Z)V"))
    public void stopCancel(BuiltChunk instance, boolean bl){
        if (client.currentScreen instanceof TitleScreen){
            return;
        }
        instance.method_10162(false);
    }

    @Redirect(method = "method_9891", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getCameraPosVec(F)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d getWorldPreviewPlayerCameraPosVec(ClientPlayerEntity instance, float tickDelta) {
        if (client.currentScreen instanceof TitleScreen) {
            return WorldPreview.player.getCameraPosVec(tickDelta);
        }
        return instance.getCameraPosVec(tickDelta);
    }

    @Redirect(method = "method_9891", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/dimension/Dimension;canPlayersSleep()Z"))
    private boolean returnTrueIfInPreview(Dimension instance) {
        if (client.currentScreen instanceof TitleScreen) {
            return true;
        }
        return instance.canPlayersSleep();
    }

    @Redirect(method = "method_9910", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/dimension/Dimension;canPlayersSleep()Z"))
    private boolean returnTrueIfInPreview2(Dimension instance) {
        if (client.currentScreen instanceof TitleScreen) {
            return true;
        }
        return instance.canPlayersSleep();
    }

//    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;", opcode = Opcodes.GETFIELD))
//    public ClientWorld worldpreview_getCorrectWorld5(MinecraftClient instance){
//        if(instance.currentScreen instanceof TitleScreen){
//            return this.world;
//        }
//        return instance.world;
//    }
//
//    @Redirect(method = "renderSky", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;", opcode = Opcodes.GETFIELD))
//    public ClientWorld worldpreview_getCorrectWorld4(MinecraftClient instance){
//        if(instance.world==null&&client.currentScreen instanceof LevelLoadingScreen){
//            return this.world;
//        }
//        return instance.world;
//
//    }
//
//    @Redirect(method = "renderSky", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;player:Lnet/minecraft/client/network/ClientPlayerEntity;", opcode = Opcodes.GETFIELD))
//    public ClientPlayerEntity worldpreview_getCorrectPlayer3(MinecraftClient instance){
//        if(instance.player==null&&client.currentScreen instanceof LevelLoadingScreen){
//            return WorldPreview.player;
//        }
//        return instance.player ;
//    }
//    @Redirect(method = "renderEntities", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;player:Lnet/minecraft/client/network/ClientPlayerEntity;", opcode = Opcodes.GETFIELD))
//    public ClientPlayerEntity worldpreview_getCorrectPlayer(MinecraftClient instance){
//        if(instance.player==null&&client.currentScreen instanceof LevelLoadingScreen){
//            return WorldPreview.player;
//        }
//        return instance.player ;
//    }
//    @Redirect(method = "setUpTerrain", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;player:Lnet/minecraft/client/network/ClientPlayerEntity;", opcode = Opcodes.GETFIELD))
//    public ClientPlayerEntity worldpreview_getCorrectPlayer2(MinecraftClient instance){
//        if(instance.player==null&&client.currentScreen instanceof LevelLoadingScreen){
//            return WorldPreview.player;
//        }
//        return instance.player ;
//    }
}
