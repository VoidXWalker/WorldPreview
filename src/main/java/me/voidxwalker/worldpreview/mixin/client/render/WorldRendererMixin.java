package me.voidxwalker.worldpreview.mixin.client.render;

import me.voidxwalker.worldpreview.ChunkSetter;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin<E> implements ChunkSetter {
    @Shadow private ClientWorld world;

    @Shadow @Final private MinecraftClient client;
    public boolean previewRenderer;
    public void setPreviewRenderer(){
        this.previewRenderer=true;
    }

    @Redirect(method = "reload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getCameraEntity()Lnet/minecraft/entity/Entity;"))
    public Entity worldpreview_getCameraEntity(MinecraftClient instance){
        if(instance.getCameraEntity()==null&&client.currentScreen instanceof LevelLoadingScreen&&this.previewRenderer){
            return WorldPreview.player;
        }
        return  instance.getCameraEntity();
    }
    @Redirect(method = "setUpTerrain",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/render/chunk/ChunkRenderer;unscheduleRebuild()V"))
    public void stopCancel(ChunkRenderer instance){
        if(client.currentScreen instanceof LevelLoadingScreen&&this.previewRenderer){
            return;
        }
        instance.unscheduleRebuild();

    }

    @Redirect(method = "renderClouds(FDDD)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;", opcode = Opcodes.GETFIELD))
    public ClientWorld worldpreview_getCorrectWorld5(MinecraftClient instance){
        if(instance.currentScreen instanceof LevelLoadingScreen){
            return this.world;
        }
        return  instance.world;
    }



    @Redirect(method = "renderSky", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;", opcode = Opcodes.GETFIELD))
    public ClientWorld worldpreview_getCorrectWorld4(MinecraftClient instance){
        if(instance.world==null&&client.currentScreen instanceof LevelLoadingScreen&&this.previewRenderer){
            return this.world;
        }
        return instance.world;

    }

    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;getCamera()Lnet/minecraft/client/render/Camera;"))
    public Camera worldpreview_getCamera(GameRenderer instance){
        if(instance.getCamera()==null&&client.currentScreen instanceof LevelLoadingScreen&&this.previewRenderer){
            return WorldPreview.camera;
        }
        return  instance.getCamera();
    }
    @Redirect(method = "renderSky", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;player:Lnet/minecraft/client/network/ClientPlayerEntity;", opcode = Opcodes.GETFIELD))
    public ClientPlayerEntity worldpreview_getCorrectPlayer3(MinecraftClient instance){
        if(instance.player==null&&client.currentScreen instanceof LevelLoadingScreen&&this.previewRenderer){
            return WorldPreview.player;
        }
        return instance.player ;
    }
    @Redirect(method = "renderEntities", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;player:Lnet/minecraft/client/network/ClientPlayerEntity;", opcode = Opcodes.GETFIELD))
    public ClientPlayerEntity worldpreview_getCorrectPlayer(MinecraftClient instance){
        if(instance.player==null&&client.currentScreen instanceof LevelLoadingScreen&&this.previewRenderer){
            return WorldPreview.player;
        }
        return instance.player ;
    }
    @Redirect(method = "setUpTerrain", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;player:Lnet/minecraft/client/network/ClientPlayerEntity;", opcode = Opcodes.GETFIELD))
    public ClientPlayerEntity worldpreview_getCorrectPlayer2(MinecraftClient instance){
        if(instance.player==null&&client.currentScreen instanceof LevelLoadingScreen&&this.previewRenderer){
            return WorldPreview.player;
        }
        return instance.player ;
    }
}
