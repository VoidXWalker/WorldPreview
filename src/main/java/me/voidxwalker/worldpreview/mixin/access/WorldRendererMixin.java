package me.voidxwalker.worldpreview.mixin.access;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.voidxwalker.worldpreview.PreviewRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

@Mixin(WorldRenderer.class)
public interface WorldRendererMixin {
    @Invoker void callDrawBlockOutline(MatrixStack matrixStack, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos, BlockState blockState);
    @Invoker void callRenderClouds(BufferBuilder builder, double x, double y, double z, Vec3d color);
    @Invoker void callMethod_22985(VertexConsumer vertexConsumer, int i, int j, int k, int l, int m, int n, int o);
    @Invoker void callMethod_22984(VertexConsumer vertexConsumer, int i);
    @Invoker void callCheckEmpty(MatrixStack matrices);
    @Invoker void callLoadTransparencyShader();
    @Invoker void callResetTransparencyShader();
    @Invoker void callRenderSkyHalf(BufferBuilder buffer, float y, boolean bottom);
    @Invoker void callCaptureFrustum(Matrix4f modelMatrix, Matrix4f matrix4f, double x, double y, double z, Frustum frustum);
    @Accessor Vector3d getCapturedFrustumPosition();
    @Accessor MinecraftClient getClient();
    @Accessor BufferBuilderStorage getBufferBuilders();
    @Accessor TextureManager getTextureManager();
    @Accessor EntityRenderDispatcher getEntityRenderDispatcher();
    @Accessor Set<BlockEntity> getNoCullingBlockEntities();
    @Accessor float[] getField_20794();
    @Accessor float[] getField_20795();
    @Accessor VertexFormat getSkyVertexFormat();
    @Accessor FpsSmoother getChunkUpdateSmoother();
    @Accessor Framebuffer getTranslucentFramebuffer();
    @Accessor Framebuffer getEntityFramebuffer();
    @Accessor Framebuffer getWeatherFramebuffer();
    @Accessor Framebuffer getParticlesFramebuffer();
    @Accessor Framebuffer getCloudsFramebuffer();
    @Accessor ShaderEffect getTransparencyShader();
    @Accessor Frustum getCapturedFrustum();
}