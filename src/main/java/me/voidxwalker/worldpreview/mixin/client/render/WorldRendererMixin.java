package me.voidxwalker.worldpreview.mixin.client.render;

import com.google.common.collect.Queues;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.voidxwalker.worldpreview.OldSodiumCompatibility;
import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.mixin.access.ChunkInfoMixin;
import me.voidxwalker.worldpreview.mixin.access.RenderPhaseMixin;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin<E> implements OldSodiumCompatibility {

    @Shadow private ClientWorld world;

    @Shadow @Final private MinecraftClient client;


    @Shadow private BuiltChunkStorage chunks;

    @Shadow private ChunkBuilder chunkBuilder;



    @Shadow private double lastTranslucentSortY;

    @Shadow private double lastTranslucentSortX;

    @Shadow private double lastTranslucentSortZ;


    @Shadow private double lastCameraChunkUpdateX;

    @Shadow private double lastCameraChunkUpdateY;

    @Shadow private double lastCameraChunkUpdateZ;

    @Shadow private int cameraChunkX;

    @Shadow private int cameraChunkY;

    @Shadow private int cameraChunkZ;


    @Shadow private double lastCameraX;

    @Shadow private double lastCameraPitch;

    @Shadow private double lastCameraY;

    @Shadow private int lastCloudsBlockZ;

    @Shadow private int lastCloudsBlockX;

    @Shadow private double lastCameraYaw;

    @Shadow private double lastCameraZ;

    @Shadow public abstract void reload();

    @Shadow @Final public static Direction[] DIRECTIONS;

    @Shadow @Nullable protected abstract ChunkBuilder.BuiltChunk getAdjacentChunk(BlockPos pos, ChunkBuilder.BuiltChunk chunk, Direction direction);

    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;

    @Shadow @Final private Set<BlockEntity> noCullingBlockEntities;

    @Shadow protected abstract void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers);

    @Shadow protected abstract boolean canDrawEntityOutlines();

    @Shadow @Final private BufferBuilderStorage bufferBuilders;

    @Shadow private int regularEntityCount;

    @Shadow @Final private Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions;

    @Shadow protected abstract void renderChunkDebugInfo(Camera camera);

    @Shadow protected abstract void renderWorldBorder(Camera camera);

    @Shadow protected abstract void renderWeather(LightmapTextureManager manager, float f, double d, double e, double g);

    @Shadow private @Nullable ShaderEffect transparencyShader;

    @Shadow private @Nullable Framebuffer cloudsFramebuffer;


    @Shadow private @Nullable Framebuffer particlesFramebuffer;

    @Shadow private @Nullable Framebuffer translucentFramebuffer;

    @Shadow protected abstract void drawBlockOutline(MatrixStack matrixStack, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos, BlockState blockState);

    @Shadow protected abstract void checkEmpty(MatrixStack matrices);

    @Shadow private @Nullable ShaderEffect entityOutlineShader;

    @Shadow @Final private FpsSmoother chunkUpdateSmoother;

    @Shadow private @Nullable Frustum capturedFrustum;

    @Shadow @Final private Vector3d capturedFrustumPosition;

    @Shadow protected abstract void renderLayer(RenderLayer renderLayer, MatrixStack matrices, double d, double e, double f, Matrix4f matrix4f);

    @Shadow @Final private BlockEntityRenderDispatcher blockEntityRenderDispatcher;

    @Shadow public abstract void renderClouds(MatrixStack matrices, Matrix4f matrix4f, float f, double d, double e, double g);


    @Shadow private int viewDistance;


    @Shadow @Final private ObjectArrayList<WorldRenderer.ChunkInfo> chunkInfos;

    @Shadow private Frustum frustum;

    @Shadow protected abstract void applyFrustum(Frustum frustum);

    @Shadow @Final private AtomicBoolean field_34809;

    @Shadow @Final private BlockingQueue<ChunkBuilder.BuiltChunk> builtChunks;

    @Shadow @Final private AtomicReference<WorldRenderer.class_6600> field_34817;

    @Shadow protected abstract void method_38549(Camera camera, Queue<WorldRenderer.ChunkInfo> queue);

    @Shadow private @Nullable Future<?> field_34808;

    @Shadow private boolean field_34810;

    @Shadow @Final private AtomicLong field_34811;

    @Shadow protected abstract void setupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator);

    @Shadow protected abstract void method_34808(LinkedHashSet<WorldRenderer.ChunkInfo> linkedHashSet, WorldRenderer.ChunkInfoList chunkInfoList, Vec3d vec3d, Queue<WorldRenderer.ChunkInfo> queue, boolean bl);

    @Redirect(method = "renderWeather", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;", opcode = Opcodes.GETFIELD))
    public ClientWorld worldpreview_getCorrectWorld(MinecraftClient instance){
        if(client.currentScreen instanceof LevelLoadingScreen){
            return this.world;
        }
        return  instance.world;
    }
    @Redirect(method = "tickRainSplashing", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;", opcode = Opcodes.GETFIELD))
    public ClientWorld worldpreview_getCorrectWorld2(MinecraftClient instance){
        if(client.currentScreen instanceof LevelLoadingScreen){
            return this.world;
        }
        return  instance.world;
    }
    @Redirect(method = "reload()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getCameraEntity()Lnet/minecraft/entity/Entity;"))
    public Entity worldpreview_getCameraEntity(MinecraftClient instance){
        if(instance.getCameraEntity()==null&&client.currentScreen instanceof LevelLoadingScreen){
            return WorldPreview.player;
        }
        return  instance.getCameraEntity();
    }

    @Inject(method = "reload()V",at = @At(value = "TAIL"))
    public void worldpreview_reload(CallbackInfo ci){
        if(this.world!=null&&client.currentScreen instanceof LevelLoadingScreen){
            this.chunks = new BuiltChunkStorage(this.chunkBuilder, this.world, this.client.options.viewDistance, (WorldRenderer) (Object)this);
        }
    }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"))
    private void worldpreview_setupTerrain(WorldRenderer instance, Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator)  {
        if(!(client.currentScreen instanceof LevelLoadingScreen)){
            this.setupTerrain(camera,frustum,hasForcedFrustum,spectator);
            return;
        }
        Vec3d vec3d = camera.getPos();
        if (this.client.options.getViewDistance() != this.viewDistance) {
            this.reload();
        }
        this.world.getProfiler().push("camera");
        double d = WorldPreview.player.getX();
        double e = WorldPreview.player.getY();
        double f = WorldPreview.player.getZ();
        double g = d - this.lastCameraChunkUpdateX;
        double h = e - this.lastCameraChunkUpdateY;
        double i = f - this.lastCameraChunkUpdateZ;
        int j = ChunkSectionPos.getSectionCoord(d);
        int k = ChunkSectionPos.getSectionCoord(e);
        int l2 = ChunkSectionPos.getSectionCoord(f);
        if (this.cameraChunkX != j || this.cameraChunkY != k || this.cameraChunkZ != l2 || g * g + h * h + i * i > 16.0) {
            this.lastCameraChunkUpdateX = d;
            this.lastCameraChunkUpdateY = e;
            this.lastCameraChunkUpdateZ = f;
            this.cameraChunkX = j;
            this.cameraChunkY = k;
            this.cameraChunkZ = l2;
            this.chunks.updateCameraPosition(d, f);
        }
        this.chunkBuilder.setCameraPosition(vec3d);
        this.world.getProfiler().swap("cull");
        this.client.getProfiler().swap("culling");
        BlockPos blockPos = camera.getBlockPos();
        double m = Math.floor(vec3d.x / 8.0);
        double n = Math.floor(vec3d.y / 8.0);
        double o = Math.floor(vec3d.z / 8.0);
        this.field_34810 = this.field_34810 || m != this.lastCameraX || n != this.lastCameraY || o != this.lastCameraZ;
        this.field_34811.updateAndGet(l -> {
            if (l > 0L && System.currentTimeMillis() > l) {
                this.field_34810 = true;
                return 0L;
            }
            return l;
        });
        this.lastCameraX = m;
        this.lastCameraY = n;
        this.lastCameraZ = o;
        this.client.getProfiler().swap("update");
        boolean bl = this.client.chunkCullingEnabled;
        if (spectator && this.world.getBlockState(blockPos).isOpaqueFullCube(this.world, blockPos)) {
            bl = false;
        }
        if (!hasForcedFrustum) {
            if (this.field_34810 && (this.field_34808 == null || this.field_34808.isDone())) {
                this.client.getProfiler().push("full_update_schedule");
                this.field_34810 = false;
                boolean bl2 = bl;
                this.field_34808 = Util.getMainWorkerExecutor().submit(() -> {
                    ArrayDeque<WorldRenderer.ChunkInfo> queue = Queues.newArrayDeque();
                    this.method_38549(camera, queue);
                    WorldRenderer.class_6600 lv = new WorldRenderer.class_6600(this.chunks.chunks.length);
                    this.method_34808(lv.field_34819, lv.field_34818, vec3d, queue, bl2);
                    this.field_34817.set(lv);
                    this.field_34809.set(true);
                });
                this.client.getProfiler().pop();
            }
            WorldRenderer.class_6600 bl2 = this.field_34817.get();
            if (!this.builtChunks.isEmpty()) {
                this.client.getProfiler().push("partial_update");
                ArrayDeque<WorldRenderer.ChunkInfo> queue = Queues.newArrayDeque();
                while (!this.builtChunks.isEmpty()) {
                    ChunkBuilder.BuiltChunk builtChunk = (ChunkBuilder.BuiltChunk)this.builtChunks.poll();
                    WorldRenderer.ChunkInfo chunkInfo = bl2.field_34818.getInfo(builtChunk);
                    if (chunkInfo == null || ((ChunkInfoMixin)chunkInfo).getChunk() != builtChunk) continue;
                    queue.add(chunkInfo);
                }
                this.method_34808(bl2.field_34819, bl2.field_34818, vec3d, queue, bl);
                this.field_34809.set(true);
                this.client.getProfiler().pop();
            }
            double queue = Math.floor(camera.getPitch() / 2.0f);
            double chunkInfo = Math.floor(camera.getYaw() / 2.0f);
            if (this.field_34809.compareAndSet(true, false) || queue != this.lastCameraPitch || chunkInfo != this.lastCameraYaw) {
                this.applyFrustum(new Frustum(frustum).method_38557(8));
                this.lastCameraPitch = queue;
                this.lastCameraYaw = chunkInfo;
            }
        }
        this.client.getProfiler().pop();
    }
    @Inject(method = "render",at=@At(value = "INVOKE",target = "Lnet/minecraft/client/render/BufferBuilderStorage;getEntityVertexConsumers()Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;"),cancellable = true)
    public void worldpreview_render(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci){
        if(client.currentScreen instanceof  LevelLoadingScreen) {
            worldpreview_renderSafe(matrices,tickDelta,limitTime,renderBlockOutline,camera,gameRenderer,lightmapTextureManager,matrix4f);
            ci.cancel();
        }
    }
    public void worldpreview_renderSafe(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix){
        Profiler profiler = this.world.getProfiler();
        Vec3d vec3d = camera.getPos();
        double d = vec3d.getX();
        double e = vec3d.getY();
        double f = vec3d.getZ();
        Object entry;

        Frustum frustum2;
        Matrix4f matrix4f2 = matrices.peek().getPositionMatrix();

        boolean bl = this.capturedFrustum != null;
        if (bl) {
            frustum2 = this.capturedFrustum;
            frustum2.setPosition(this.capturedFrustumPosition.x, this.capturedFrustumPosition.y, this.capturedFrustumPosition.z);
        } else {
             frustum2 = this.frustum;
        }
        long o = Util.getMeasuringTimeNano() - limitTime;
        long p = this.chunkUpdateSmoother.getTargetUsedTime(o);
        long q = p * 3L / 2L;
        boolean bl3 = false;
        Object outlineVertexConsumerProvider;
        VertexConsumerProvider.Immediate immediate = this.bufferBuilders.getEntityVertexConsumers();
        Set<BlockEntity> set = this.noCullingBlockEntities;
        boolean bl4 = false;
        synchronized (set) {
            for (BlockEntity blockEntity : this.noCullingBlockEntities) {
                outlineVertexConsumerProvider = blockEntity.getPos();
                matrices.push();
                matrices.translate((double)((Vec3i)outlineVertexConsumerProvider).getX() - d, (double)((Vec3i)outlineVertexConsumerProvider).getY() - e, (double)((Vec3i)outlineVertexConsumerProvider).getZ() - f);
                this.blockEntityRenderDispatcher.render(blockEntity, tickDelta, matrices, immediate);
                matrices.pop();
            }
        }
        this.checkEmpty(matrices);
        immediate.draw(RenderLayer.getSolid());
        immediate.draw(RenderLayer.getEndPortal());
        immediate.draw(RenderLayer.getEndGateway());
        immediate.draw(TexturedRenderLayers.getEntitySolid());
        immediate.draw(TexturedRenderLayers.getEntityCutout());
        immediate.draw(TexturedRenderLayers.getBeds());
        immediate.draw(TexturedRenderLayers.getShulkerBoxes());
        immediate.draw(TexturedRenderLayers.getSign());
        immediate.draw(TexturedRenderLayers.getChest());
        this.bufferBuilders.getOutlineVertexConsumers().draw();
        if (bl4) {
            this.entityOutlineShader.render(tickDelta);
            this.client.getFramebuffer().beginWrite(false);
        }


        this.checkEmpty(matrices);
        HitResult hitResult = this.client.crosshairTarget;
        if (renderBlockOutline && hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            profiler.swap("outline");
            BlockPos blockPos = ((BlockHitResult)hitResult).getBlockPos();
            BlockState blockState = this.world.getBlockState(blockPos);
            if (!blockState.isAir() && this.world.getWorldBorder().contains(blockPos)) {
                VertexConsumer outlineVertexConsumerProvider3 = immediate.getBuffer(RenderLayer.getLines());
                this.drawBlockOutline(matrices, outlineVertexConsumerProvider3, camera.getFocusedEntity(), d, e, f, blockPos, blockState);
            }
        }
        MatrixStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.push();
        matrixStack.multiplyPositionMatrix(matrices.peek().getPositionMatrix());
        RenderSystem.applyModelViewMatrix();
        matrixStack.pop();
        RenderSystem.applyModelViewMatrix();
        immediate.draw(TexturedRenderLayers.getEntityTranslucentCull());
        immediate.draw(TexturedRenderLayers.getBannerPatterns());
        immediate.draw(TexturedRenderLayers.getShieldPatterns());
        immediate.draw(RenderLayer.getArmorGlint());
        immediate.draw(RenderLayer.getArmorEntityGlint());
        immediate.draw(RenderLayer.getGlint());
        immediate.draw(RenderLayer.getDirectGlint());
        immediate.draw(RenderLayer.getGlintTranslucent());
        immediate.draw(RenderLayer.getEntityGlint());
        immediate.draw(RenderLayer.getDirectEntityGlint());
        immediate.draw(RenderLayer.getWaterMask());
        this.bufferBuilders.getEffectVertexConsumers().draw();
        if (this.transparencyShader != null) {
            immediate.draw(RenderLayer.getLines());
            immediate.draw();
            this.translucentFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
            this.translucentFramebuffer.copyDepthFrom(this.client.getFramebuffer());
            profiler.swap("translucent");
            this.renderLayer(RenderLayer.getTranslucent(), matrices, d, e, f, positionMatrix);
            profiler.swap("string");
            this.renderLayer(RenderLayer.getTripwire(), matrices, d, e, f, positionMatrix);
            this.particlesFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
            this.particlesFramebuffer.copyDepthFrom(this.client.getFramebuffer());
            RenderPhaseMixin.getPARTICLES_TARGET().startDrawing();
            profiler.swap("particles");
            this.client.particleManager.renderParticles(matrices, immediate, lightmapTextureManager, camera, tickDelta);
            RenderPhaseMixin.getPARTICLES_TARGET().endDrawing();
        } else {
            profiler.swap("translucent");
            if (this.translucentFramebuffer != null) {
                this.translucentFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
            }
            this.renderLayer(RenderLayer.getTranslucent(), matrices, d, e, f, positionMatrix);
            immediate.draw(RenderLayer.getLines());
            immediate.draw();
            profiler.swap("string");
            this.renderLayer(RenderLayer.getTripwire(), matrices, d, e, f, positionMatrix);
            profiler.swap("particles");
            this.client.particleManager.renderParticles(matrices, immediate, lightmapTextureManager, camera, tickDelta);
        }
        matrixStack.push();
        matrixStack.multiplyPositionMatrix(matrices.peek().getPositionMatrix());
        RenderSystem.applyModelViewMatrix();
        if (this.client.options.getCloudRenderMode() != CloudRenderMode.OFF) {
            if (this.transparencyShader != null) {
                this.cloudsFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
                RenderPhaseMixin.getCLOUDS_TARGET().startDrawing();
                profiler.swap("clouds");
                this.renderClouds(matrices, positionMatrix, tickDelta, d, e, f);
                RenderPhaseMixin.getCLOUDS_TARGET().endDrawing();
            } else {
                profiler.swap("clouds");
                RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
                this.renderClouds(matrices, positionMatrix, tickDelta, d, e, f);
            }
        }
        if (this.transparencyShader != null) {
            RenderPhaseMixin.getWEATHER_TARGET().startDrawing();
            profiler.swap("weather");
            this.renderWeather(lightmapTextureManager, tickDelta, d, e, f);
            this.renderWorldBorder(camera);
            RenderPhaseMixin.getWEATHER_TARGET().endDrawing();
            this.transparencyShader.render(tickDelta);
            this.client.getFramebuffer().beginWrite(false);
        } else {
            RenderSystem.depthMask(false);
            profiler.swap("weather");
            this.renderWeather(lightmapTextureManager, tickDelta, d, e, f);
            this.renderWorldBorder(camera);
            RenderSystem.depthMask(true);
        }
        this.renderChunkDebugInfo(camera);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        matrixStack.pop();
        RenderSystem.applyModelViewMatrix();
        BackgroundRenderer.clearFog();

    }
    @Override
    public void worldpreview_setWorldSafe(@Nullable ClientWorld clientWorld) {
        this.lastCameraChunkUpdateX = Double.MIN_VALUE;
        this.lastCameraChunkUpdateY = Double.MIN_VALUE;
        this.lastCameraChunkUpdateZ = Double.MIN_VALUE;
        this.cameraChunkX = Integer.MIN_VALUE;
        this.cameraChunkY = Integer.MIN_VALUE;
        this.cameraChunkZ = Integer.MIN_VALUE;
        this.entityRenderDispatcher.setWorld(world);
        this.world = clientWorld;
        if (world != null) {
            this.reload();
        } else {
            if (this.chunks != null) {
                this.chunks.clear();
                this.chunks = null;
            }
            if (this.chunkBuilder != null) {
                this.chunkBuilder.stop();
            }
            this.chunkBuilder = null;
            this.noCullingBlockEntities.clear();
            this.field_34817.set(null);
            this.chunkInfos.clear();
        }

    }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderLayer(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/util/math/MatrixStack;DDDLnet/minecraft/util/math/Matrix4f;)V"))
    private void worldpreview_renderLayer(WorldRenderer instance, RenderLayer renderLayer, MatrixStack matrices, double d, double e, double f, Matrix4f matrix4f) {
        if (!(client.currentScreen instanceof LevelLoadingScreen)) {
            this.renderLayer(renderLayer, matrices, d, e, f,matrix4f);
            return;
        }
        this.worldpreview_renderLayerSafe(renderLayer,matrices,d,e,f,matrix4f);
    }
    private void worldpreview_renderLayerSafe(RenderLayer renderLayer, MatrixStack matrices, double d, double e, double f, Matrix4f positionMatrix) {
        int k;
        RenderSystem.assertOnRenderThread();
        renderLayer.startDrawing();
        if (renderLayer == RenderLayer.getTranslucent()) {
            this.client.getProfiler().push("translucent_sort");
            double g = d - this.lastTranslucentSortX;
            double h = e - this.lastTranslucentSortY;
            double i = f - this.lastTranslucentSortZ;
            if (g * g + h * h + i * i > 1.0) {
                this.lastTranslucentSortX = d;
                this.lastTranslucentSortY = e;
                this.lastTranslucentSortZ = f;
                int j = 0;
                for (WorldRenderer.ChunkInfo chunkInfo : this.chunkInfos) {
                    if (j >= 15 || !((ChunkInfoMixin)chunkInfo).getChunk().scheduleSort(renderLayer, this.chunkBuilder)) continue;
                    ++j;
                }
            }
            this.client.getProfiler().pop();
        }
        this.client.getProfiler().push("filterempty");
        this.client.getProfiler().swap(() -> "render_" + renderLayer);
        boolean g = renderLayer != RenderLayer.getTranslucent();
        ListIterator objectListIterator = this.chunkInfos.listIterator(g ? 0 : this.chunkInfos.size());
        VertexFormat h = renderLayer.getVertexFormat();
        Shader shader = RenderSystem.getShader();
        BufferRenderer.unbindAll();
        for (int i = 0; i < 12; ++i) {
            k = RenderSystem.getShaderTexture(i);
            shader.addSampler("Sampler" + i, k);
        }
        if (shader.modelViewMat != null) {
            shader.modelViewMat.set(matrices.peek().getPositionMatrix());
        }
        if (shader.projectionMat != null) {
            shader.projectionMat.set(positionMatrix);
        }
        if (shader.colorModulator != null) {
            shader.colorModulator.set(RenderSystem.getShaderColor());
        }
        if (shader.fogStart != null) {
            shader.fogStart.set(RenderSystem.getShaderFogStart());
        }
        if (shader.fogEnd != null) {
            shader.fogEnd.set(RenderSystem.getShaderFogEnd());
        }
        if (shader.fogColor != null) {
            shader.fogColor.set(RenderSystem.getShaderFogColor());
        }
        if (shader.textureMat != null) {
            shader.textureMat.set(RenderSystem.getTextureMatrix());
        }
        if (shader.gameTime != null) {
            shader.gameTime.set(RenderSystem.getShaderGameTime());
        }
        RenderSystem.setupShaderLights(shader);
        shader.bind();
        GlUniform i = shader.chunkOffset;
        k = 0;
        while (g ? objectListIterator.hasNext() : objectListIterator.hasPrevious()) {
            WorldRenderer.ChunkInfo j = g ? (WorldRenderer.ChunkInfo)objectListIterator.next() : (WorldRenderer.ChunkInfo)objectListIterator.previous();
            ChunkBuilder.BuiltChunk builtChunk = ((ChunkInfoMixin)j).getChunk();
            if (builtChunk.getData().isEmpty(renderLayer)) continue;
            VertexBuffer vertexBuffer = builtChunk.getBuffer(renderLayer);
            BlockPos blockPos = builtChunk.getOrigin();
            if (i != null) {
                i.set((float)((double)blockPos.getX() - d), (float)((double)blockPos.getY() - e), (float)((double)blockPos.getZ() - f));
                i.upload();
            }
            vertexBuffer.drawVertices();
            k = 1;
        }
        if (i != null) {
            i.set(Vec3f.ZERO);
        }
        shader.unbind();
        if (k != 0) {
            h.endDrawing();
        }
        VertexBuffer.unbind();
        VertexBuffer.unbindVertexArray();
        this.client.getProfiler().pop();
        renderLayer.endDrawing();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;getViewDistance()F"))
    public float worldpreview_getViewDistance(GameRenderer instance){
        if(client.currentScreen instanceof LevelLoadingScreen){
            return client.options.viewDistance*16;
        }

        return instance.getViewDistance();
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;targetedEntity:Lnet/minecraft/entity/Entity;", opcode = Opcodes.GETFIELD))
    public Entity worldpreview_getCorrectTargetedPlayerEntity(MinecraftClient instance){
        if(instance.player==null&&client.currentScreen instanceof LevelLoadingScreen){
            return WorldPreview.player;
        }
        return instance.targetedEntity ;
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;", opcode = Opcodes.GETFIELD))
    public ClientWorld worldpreview_getCorrectWorld3(MinecraftClient instance){
        if(instance.currentScreen instanceof LevelLoadingScreen){
            return this.world;
        }
        return  instance.world;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/DimensionEffects;useThickFog(II)Z"))
    public boolean worldpreview_shouldThickenFog(DimensionEffects instance, int i, int j){
        if(client.gameRenderer==null&&client.currentScreen instanceof LevelLoadingScreen){
            return false;
        }

        return  instance.useThickFog(i, j);
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;player:Lnet/minecraft/client/network/ClientPlayerEntity;", ordinal =1, opcode = Opcodes.GETFIELD))
    public ClientPlayerEntity worldpreview_getCorrectPlayer2(MinecraftClient instance){
        if(instance.player==null&&client.currentScreen instanceof LevelLoadingScreen){
            return WorldPreview.player;
        }
        return instance.player ;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSpectator()Z"))
    public boolean worldpreview_spectator(ClientPlayerEntity instance){
        if(client.currentScreen instanceof LevelLoadingScreen&&instance==null){
            return false;
        }

        return instance.isSpectator();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/debug/DebugRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;DDD)V"))
    public void worldpreview_stopDebugRenderer(DebugRenderer instance, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, double cameraX, double cameraY, double cameraZ){
        if(client.currentScreen instanceof LevelLoadingScreen){
            return;
        }

        instance.render(matrices, vertexConsumers, cameraX, cameraY, cameraZ);
    }

    @Redirect(method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Matrix4f;FLjava/lang/Runnable;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;", opcode = Opcodes.GETFIELD))
    public ClientWorld worldpreview_getCorrectWorld4(MinecraftClient instance){
        if(instance.world==null&&client.currentScreen instanceof LevelLoadingScreen){
            return this.world;
        }
        return instance.world;

    }

    @Redirect(method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Matrix4f;FLjava/lang/Runnable;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;getCamera()Lnet/minecraft/client/render/Camera;"))
    public Camera worldpreview_getCamera(GameRenderer instance){
        if(instance.getCamera()==null&&client.currentScreen instanceof LevelLoadingScreen){
            return WorldPreview.camera;
        }
        return  instance.getCamera();
    }

    @Redirect(method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Matrix4f;FLjava/lang/Runnable;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;player:Lnet/minecraft/client/network/ClientPlayerEntity;", opcode = Opcodes.GETFIELD))
    public ClientPlayerEntity worldpreview_getCorrectPlayer3(MinecraftClient instance){
        if(instance.player==null&&client.currentScreen instanceof LevelLoadingScreen){
            return WorldPreview.player;
        }
        return instance.player ;
    }
}