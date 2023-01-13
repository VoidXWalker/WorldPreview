package me.voidxwalker.worldpreview.mixin.client.render;


import me.voidxwalker.worldpreview.ChunkSetter;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.BuiltChunk;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin implements ChunkSetter {
    @Shadow private ClientWorld world;

    public boolean previewRenderer;
    public void setPreviewRenderer(){
        this.previewRenderer=true;
    }
    @Redirect(method = "reload()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getCameraEntity()Lnet/minecraft/entity/Entity;"))
    public Entity worldpreview_getCameraEntity(MinecraftClient instance){
        if(instance.getCameraEntity()==null&&this.previewRenderer){
            return WorldPreview.player;
        }
        return  instance.getCameraEntity();
    }
    @Redirect(method = "method_9908", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getCameraEntity()Lnet/minecraft/entity/Entity;"))
    public Entity worldpreview_getCameraEntity2(MinecraftClient instance){
        if(instance.getCameraEntity()==null&&this.previewRenderer){
            return WorldPreview.player;
        }
        return  instance.getCameraEntity();
    }
    @Redirect(method = "method_9908", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;player:Lnet/minecraft/client/network/ClientPlayerEntity;", opcode = Opcodes.GETFIELD))
    public ClientPlayerEntity worldpreview_getCorrectPlayer(MinecraftClient instance){
        if(instance.player==null&&this.previewRenderer){
            return WorldPreview.player;
        }
        return instance.player ;
    }
    @Redirect(method = "method_9891", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;", opcode = Opcodes.GETFIELD))
    public ClientWorld worldpreview_getCorrectWorld(MinecraftClient instance){
        if(instance.world==null&&this.previewRenderer){
            return this.world;
        }
        return instance.world;

    }
    @Redirect(method = "method_9891", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getCameraEntity()Lnet/minecraft/entity/Entity;"))
    public Entity worldpreview_getCameraEntity3(MinecraftClient instance){
        if(instance.getCameraEntity()==null&&this.previewRenderer){
            return WorldPreview.player;
        }
        return  instance.getCameraEntity();
    }
    @Redirect(method = "method_9891", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;player:Lnet/minecraft/client/network/ClientPlayerEntity;", opcode = Opcodes.GETFIELD))
    public ClientPlayerEntity worldpreview_getCorrectPlayer2(MinecraftClient instance){
        if(instance.player==null&&this.previewRenderer){
            return WorldPreview.player;
        }
        return instance.player ;
    }
    @Redirect(method = "method_9910", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;", opcode = Opcodes.GETFIELD))
    public ClientWorld worldpreview_getCorrectWorld2(MinecraftClient instance){
        if(instance.world==null&&this.previewRenderer){
            return this.world;
        }
        return instance.world;

    }
    @Redirect(method = "method_9910", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getCameraEntity()Lnet/minecraft/entity/Entity;"))
    public Entity worldpreview_getCameraEntity4(MinecraftClient instance){
        if(instance.getCameraEntity()==null&&this.previewRenderer){
            return WorldPreview.player;
        }
        return  instance.getCameraEntity();
    }
    @Redirect(method = "method_9913", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getCameraEntity()Lnet/minecraft/entity/Entity;"))
    public Entity worldpreview_getCameraEntity5(MinecraftClient instance){
        if(instance.getCameraEntity()==null&&this.previewRenderer){
            return WorldPreview.player;
        }
        return  instance.getCameraEntity();
    }
    @Redirect(method = "method_9911", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getCameraEntity()Lnet/minecraft/entity/Entity;"))
    public Entity worldpreview_getCameraEntity6(MinecraftClient instance){
        if(instance.getCameraEntity()==null&&this.previewRenderer){
            return WorldPreview.player;
        }
        return  instance.getCameraEntity();
    }
    @Redirect(method = "method_4692", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getCameraEntity()Lnet/minecraft/entity/Entity;"))
    public Entity worldpreview_getCameraEntity7(MinecraftClient instance){
        if(instance.getCameraEntity()==null&&this.previewRenderer){
            return WorldPreview.player;
        }
        return  instance.getCameraEntity();
    }
    @Redirect(method = "method_9908",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/render/GameRenderer;enableLightmap()V"))
    public void stopLightMap1(GameRenderer instance){
        if(this.previewRenderer){
            return;
        }
        instance.enableLightmap();
    }
    @Redirect(method = "method_9908",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/render/GameRenderer;disableLightmap()V"))
    public void stopLightMap2(GameRenderer instance){
        if(this.previewRenderer){
            return;
        }
        instance.disableLightmap();
    }
    @Redirect(method = "method_9893",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/render/GameRenderer;enableLightmap()V"))
    public void stopLightMap3(GameRenderer instance){
        if(this.previewRenderer){
            return;
        }
        instance.enableLightmap();
    }
    @Redirect(method = "method_9893",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/render/GameRenderer;disableLightmap()V"))
    public void stopLightMap4(GameRenderer instance){
        if(this.previewRenderer){
            return;
        }
        instance.disableLightmap();
    }
}
