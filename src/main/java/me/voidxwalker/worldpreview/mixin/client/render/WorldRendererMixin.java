package me.voidxwalker.worldpreview.mixin.client.render;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import me.voidxwalker.worldpreview.OldSodiumCompatibility;
import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.mixin.access.BuiltChunkStorageMixin;
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
import net.minecraft.client.option.Option;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
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

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin<E> implements OldSodiumCompatibility {
    public void setPreviewRenderer(){
        this.previewRenderer=true;
    }
    public boolean previewRenderer;
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

    @Shadow private boolean needsTerrainUpdate;

    @Shadow private double lastCameraX;

    @Shadow private Set<ChunkBuilder.BuiltChunk> chunksToRebuild;

    @Shadow private double lastCameraPitch;

    @Shadow private double lastCameraY;

    @Shadow private int lastCloudsBlockZ;

    @Shadow private int lastCloudsBlockX;

    @Shadow private double lastCameraYaw;

    @Shadow private double lastCameraZ;

    @Shadow public abstract void reload();

    @Shadow @Final public static Direction[] DIRECTIONS;

    @Shadow @Nullable protected abstract ChunkBuilder.BuiltChunk getAdjacentChunk(BlockPos pos, ChunkBuilder.BuiltChunk chunk, Direction direction);

    @Shadow protected abstract void setupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, int frame, boolean spectator);

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

    @Shadow @Final private BlockEntityRenderDispatcher blockEntityRenderDispatcher;

    @Shadow public abstract void renderClouds(MatrixStack matrices, Matrix4f matrix4f, float f, double d, double e, double g);

    @Shadow protected abstract void method_34808(Frustum frustum, int i, boolean bl, Vec3d vec3d, BlockPos blockPos, ChunkBuilder.BuiltChunk builtChunk, int j, BlockPos blockPos2);

    @Shadow private int viewDistance;

    @Shadow @Final private ObjectArrayList<WorldRenderer.ChunkInfo> visibleChunks;

    @Shadow private @Nullable VertexBuffer lightSkyBuffer;

    @Shadow @Final private static Identifier SUN;

    @Shadow @Final private static Identifier MOON_PHASES;

    @Shadow private @Nullable VertexBuffer starsBuffer;

    @Shadow private @Nullable VertexBuffer darkSkyBuffer;

    @Shadow public abstract void renderSky(MatrixStack matrices, Matrix4f matrix4f, float f, Runnable runnable);

    @Shadow private int blockEntityCount;

    @Shadow private @Nullable Framebuffer entityFramebuffer;

    @Shadow private @Nullable Framebuffer weatherFramebuffer;

    @Shadow private @Nullable Framebuffer entityOutlinesFramebuffer;

    @Shadow protected abstract void updateChunks(long limitTime);

    @Shadow private int frame;

    @Shadow private boolean shouldCaptureFrustum;

    @Shadow public abstract void captureFrustum();

    @Shadow protected abstract void captureFrustum(Matrix4f modelMatrix, Matrix4f matrix4f, double x, double y, double z, Frustum frustum);

    @Shadow private Frustum frustum;

    @Shadow private WorldRenderer.ChunkInfoList chunkInfos;

    @Override
    public void worldpreview_renderSafe(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix) {
        int l;
        BlockPos blockPos;
        Frustum frustum;
        boolean bl2;
        RenderSystem.setShaderGameTime(this.world.getTime(), tickDelta);
        this.blockEntityRenderDispatcher.configure(this.world, camera, this.client.crosshairTarget);
        this.entityRenderDispatcher.configure(this.world, camera, this.client.targetedEntity);
        Profiler profiler = this.world.getProfiler();
        profiler.swap("light_update_queue");
        this.world.runQueuedChunkUpdates();
        profiler.swap("light_updates");
        boolean bl = this.world.hasNoChunkUpdaters();
        this.world.getChunkManager().getLightingProvider().doLightUpdates(Integer.MAX_VALUE, bl, true);
        Vec3d vec3d = camera.getPos();
        double d = vec3d.getX();
        double e = vec3d.getY();
        double f = vec3d.getZ();
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        profiler.swap("culling");
        boolean bl3 = bl2 = this.capturedFrustum != null;
        if (bl2) {
            frustum = this.capturedFrustum;
            frustum.setPosition(this.capturedFrustumPosition.x, this.capturedFrustumPosition.y, this.capturedFrustumPosition.z);
        } else {
            frustum = this.frustum;
        }
        this.client.getProfiler().swap("captureFrustum");
        if (this.shouldCaptureFrustum) {
            this.captureFrustum(matrix4f, positionMatrix, vec3d.x, vec3d.y, vec3d.z, bl2 ? new Frustum(matrix4f, positionMatrix) : frustum);
            this.shouldCaptureFrustum = false;
        }
        profiler.swap("clear");
        BackgroundRenderer.render(camera, tickDelta, this.client.world, this.client.options.getClampedViewDistance(), gameRenderer.getSkyDarkness(tickDelta));
        BackgroundRenderer.setFogBlack();
        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT | GlConst.GL_COLOR_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
        float g = gameRenderer.getViewDistance();
        boolean bl32 = this.client.world.getDimensionEffects().useThickFog(MathHelper.floor(d), MathHelper.floor(e)) || this.client.inGameHud.getBossBarHud().shouldThickenFog();
        profiler.swap("sky");
        RenderSystem.setShader(GameRenderer::getPositionShader);
        this.renderSky(matrices, positionMatrix, tickDelta, camera, bl32, () -> BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_SKY, g, bl32, tickDelta));
        profiler.swap("fog");
        BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_TERRAIN, Math.max(g, 32.0f), bl32, tickDelta);
        profiler.swap("terrain_setup");
        this.worldpreview_setupTerrain(camera, frustum, bl2, this.client.player.isSpectator());
        this.setupTerrain(camera, frustum, bl2, this.client.player.isSpectator());
        profiler.swap("compilechunks");
        this.updateChunks(camera);
        profiler.swap("terrain");
        this.worldpreview_renderLayerSafe(RenderLayer.getSolid(), matrices, d, e, f, positionMatrix);
        this.worldpreview_renderLayerSafe(RenderLayer.getCutoutMipped(), matrices, d, e, f, positionMatrix);
        this.worldpreview_renderLayerSafe(RenderLayer.getCutout(), matrices, d, e, f, positionMatrix);
        if (this.world.getDimensionEffects().isDarkened()) {
            DiffuseLighting.enableForLevel(matrices.peek().getPositionMatrix());
        } else {
            DiffuseLighting.disableForLevel(matrices.peek().getPositionMatrix());
        }
        profiler.swap("entities");
        this.regularEntityCount = 0;
        this.blockEntityCount = 0;
        if (this.entityFramebuffer != null) {
            this.entityFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
            this.entityFramebuffer.copyDepthFrom(this.client.getFramebuffer());
            this.client.getFramebuffer().beginWrite(false);
        }
        if (this.weatherFramebuffer != null) {
            this.weatherFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
        }
        if (this.canDrawEntityOutlines()) {
            this.entityOutlinesFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
            this.client.getFramebuffer().beginWrite(false);
        }
        boolean bl4 = false;
        VertexConsumerProvider.Immediate immediate = this.bufferBuilders.getEntityVertexConsumers();
        for (Entity entity : this.world.getEntities()) {
            Object vertexConsumerProvider;
            if (!this.entityRenderDispatcher.shouldRender(entity, frustum, d, e, f) && !entity.hasPassengerDeep(this.client.player) || !this.world.isOutOfHeightLimit((blockPos = entity.getBlockPos()).getY()) && !this.isRenderingReady(blockPos) || entity == camera.getFocusedEntity() && !camera.isThirdPerson() && (!(camera.getFocusedEntity() instanceof LivingEntity) || !((LivingEntity)camera.getFocusedEntity()).isSleeping()) || entity instanceof ClientPlayerEntity && camera.getFocusedEntity() != entity) continue;
            ++this.regularEntityCount;
            if (entity.age == 0) {
                entity.lastRenderX = entity.getX();
                entity.lastRenderY = entity.getY();
                entity.lastRenderZ = entity.getZ();
            }
            if (this.canDrawEntityOutlines() && this.client.hasOutline(entity)) {
                bl4 = true;
                OutlineVertexConsumerProvider outlineVertexConsumerProvider = this.bufferBuilders.getOutlineVertexConsumers();
                vertexConsumerProvider = outlineVertexConsumerProvider;
                int i = entity.getTeamColorValue();
                int j = 255;
                int k = i >> 16 & 0xFF;
                l = i >> 8 & 0xFF;
                int m = i & 0xFF;
                outlineVertexConsumerProvider.setColor(k, l, m, 255);
            } else {
                vertexConsumerProvider = immediate;
            }
            this.renderEntity(entity, d, e, f, tickDelta, matrices, (VertexConsumerProvider)vertexConsumerProvider);
        }
        immediate.drawCurrentLayer();
        this.checkEmpty(matrices);
        immediate.draw(RenderLayer.getEntitySolid(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        immediate.draw(RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        immediate.draw(RenderLayer.getEntityCutoutNoCull(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        immediate.draw(RenderLayer.getEntitySmoothCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
        profiler.swap("blockentities");
        for (WorldRenderer.ChunkInfo chunkInfo : this.chunkInfos) {
            List<BlockEntity> list = ((ChunkInfoMixin)chunkInfo).getChunk().getData().getBlockEntities();
            if (list.isEmpty()) continue;
            for (BlockEntity blockEntity : list) {
                BlockPos blockPos2 = blockEntity.getPos();
                VertexConsumerProvider vertexConsumerProvider2 = immediate;
                matrices.push();
                matrices.translate((double)blockPos2.getX() - d, (double)blockPos2.getY() - e, (double)blockPos2.getZ() - f);
                SortedSet sortedSet = (SortedSet)this.blockBreakingProgressions.get(blockPos2.asLong());
                if (sortedSet != null && !sortedSet.isEmpty() && (l = ((BlockBreakingInfo)sortedSet.last()).getStage()) >= 0) {
                    MatrixStack.Entry entry = matrices.peek();
                    OverlayVertexConsumer vertexConsumer = new OverlayVertexConsumer(this.bufferBuilders.getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(l)), entry.getPositionMatrix(), entry.getNormalMatrix());
                    vertexConsumerProvider2 = renderLayer -> {
                        VertexConsumer vertexConsumer2 = immediate.getBuffer(renderLayer);
                        if (renderLayer.hasCrumbling()) {
                            return VertexConsumers.union(vertexConsumer, vertexConsumer2);
                        }
                        return vertexConsumer2;
                    };
                }
                this.blockEntityRenderDispatcher.render(blockEntity, tickDelta, matrices, vertexConsumerProvider2);
                matrices.pop();
            }
        }
        Set<BlockEntity> set = this.noCullingBlockEntities;
        synchronized (set) {
            for (BlockEntity blockEntity2 : this.noCullingBlockEntities) {
                BlockPos blockPos3 = blockEntity2.getPos();
                matrices.push();
                matrices.translate((double)blockPos3.getX() - d, (double)blockPos3.getY() - e, (double)blockPos3.getZ() - f);
                this.blockEntityRenderDispatcher.render(blockEntity2, tickDelta, matrices, immediate);
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
        profiler.swap("destroyProgress");
        for (Long2ObjectMap.Entry entry : this.blockBreakingProgressions.long2ObjectEntrySet()) {
            SortedSet sortedSet2;
            double o;
            double n;
            blockPos = BlockPos.fromLong(entry.getLongKey());
            double h = (double)blockPos.getX() - d;
            if (h * h + (n = (double)blockPos.getY() - e) * n + (o = (double)blockPos.getZ() - f) * o > 1024.0 || (sortedSet2 = (SortedSet)entry.getValue()) == null || sortedSet2.isEmpty()) continue;
            int p = ((BlockBreakingInfo)sortedSet2.last()).getStage();
            matrices.push();
            matrices.translate((double)blockPos.getX() - d, (double)blockPos.getY() - e, (double)blockPos.getZ() - f);
            MatrixStack.Entry entry3 = matrices.peek();
            OverlayVertexConsumer vertexConsumer2 = new OverlayVertexConsumer(this.bufferBuilders.getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(p)), entry3.getPositionMatrix(), entry3.getNormalMatrix());
            this.client.getBlockRenderManager().renderDamage(this.world.getBlockState(blockPos), blockPos, this.world, matrices, vertexConsumer2);
            matrices.pop();
        }
        this.checkEmpty(matrices);
        HitResult hitResult = this.client.crosshairTarget;
        if (renderBlockOutline && hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
            profiler.swap("outline");
            BlockPos blockPos2 = ((BlockHitResult)hitResult).getBlockPos();
            BlockState blockState = this.world.getBlockState(blockPos2);
            if (!blockState.isAir() && this.world.getWorldBorder().contains(blockPos2)) {
                VertexConsumer vertexConsumer3 = immediate.getBuffer(RenderLayer.getLines());
                this.drawBlockOutline(matrices, vertexConsumer3, camera.getFocusedEntity(), d, e, f, blockPos2, blockState);
            }
        }
        MatrixStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.push();
        matrixStack.multiplyPositionMatrix(matrices.peek().getPositionMatrix());
        RenderSystem.applyModelViewMatrix();
        this.client.debugRenderer.render(matrices, immediate, d, e, f);
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
            this.worldpreview_renderLayerSafe(RenderLayer.getTranslucent(), matrices, d, e, f, positionMatrix);
            profiler.swap("string");
            this.worldpreview_renderLayerSafe(RenderLayer.getTripwire(), matrices, d, e, f, positionMatrix);
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
            this.worldpreview_renderLayerSafe(RenderLayer.getTranslucent(), matrices, d, e, f, positionMatrix);
            immediate.draw(RenderLayer.getLines());
            immediate.draw();
            profiler.swap("string");
            this.worldpreview_renderLayerSafe(RenderLayer.getTripwire(), matrices, d, e, f, positionMatrix);
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
    @Redirect(method = "renderWeather", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;", opcode = Opcodes.GETFIELD))
    public ClientWorld worldpreview_getCorrectWorld(MinecraftClient instance){
        if(client.currentScreen instanceof LevelLoadingScreen&&this.previewRenderer){
            return this.world;
        }
        return  instance.world;
    }
    @Redirect(method = "tickRainSplashing", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;", opcode = Opcodes.GETFIELD))
    public ClientWorld worldpreview_getCorrectWorld2(MinecraftClient instance){
        if(client.currentScreen instanceof LevelLoadingScreen&&this.previewRenderer){
            return this.world;
        }
        return  instance.world;
    }
    @Redirect(method = "reload()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getCameraEntity()Lnet/minecraft/entity/Entity;"))
    public Entity worldpreview_getCameraEntity(MinecraftClient instance){
        if(instance.getCameraEntity()==null&&client.currentScreen instanceof LevelLoadingScreen&&this.previewRenderer){
            return WorldPreview.player;
        }
        return  instance.getCameraEntity();
    }

    @Inject(method = "reload()V",at = @At(value = "TAIL"))
    public void worldpreview_reload(CallbackInfo ci){
        if(this.world!=null&&client.currentScreen instanceof LevelLoadingScreen&&this.previewRenderer){
            this.chunks = new BuiltChunkStorage(this.chunkBuilder, this.world, this.client.options.viewDistance, ((WorldRenderer) (Object)this));
            this.chunkInfos = new WorldRenderer.ChunkInfoList(this.chunks.chunks.length);
            if (this.world != null) {
                Entity entity = WorldPreview.player;
                if (entity != null) {
                    this.chunks.updateCameraPosition(entity.getX(), entity.getZ());
                }
            }
        }
    }

    private void worldpreview_setupTerrain( Camera camera, Frustum frustum, boolean hasForcedFrustum, int frame, boolean spectator)  {
       Vec3d vec3d = camera.getPos();
       if (this.client.options.viewDistance != this.viewDistance) {
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
       int l = ChunkSectionPos.getSectionCoord(f);
       if (this.cameraChunkX != j || this.cameraChunkY != k || this.cameraChunkZ != l || g * g + h * h + i * i > 16.0D) {
           this.lastCameraChunkUpdateX = d;
           this.lastCameraChunkUpdateY = e;
           this.lastCameraChunkUpdateZ = f;
           this.cameraChunkX = j;
           this.cameraChunkY = k;
           this.cameraChunkZ = l;
           this.chunks.updateCameraPosition(d, f);
       }

       this.chunkBuilder.setCameraPosition(vec3d);
       this.world.getProfiler().swap("cull");
       this.client.getProfiler().swap("culling");
       BlockPos blockPos = camera.getBlockPos();
       ChunkBuilder.BuiltChunk builtChunk = ((BuiltChunkStorageMixin)this.chunks).callGetRenderedChunk(blockPos);
       BlockPos blockPos2 = new BlockPos(MathHelper.floor(vec3d.x / 16.0D) * 16, MathHelper.floor(vec3d.y / 16.0D) * 16, MathHelper.floor(vec3d.z / 16.0D) * 16);
       float n = camera.getPitch();
       float o = camera.getYaw();
       this.needsTerrainUpdate = this.needsTerrainUpdate || !this.chunksToRebuild.isEmpty() || vec3d.x != this.lastCameraX || vec3d.y != this.lastCameraY || vec3d.z != this.lastCameraZ || (double)n != this.lastCameraPitch || (double)o != this.lastCameraYaw;
       this.lastCameraX = vec3d.x;
       this.lastCameraY = vec3d.y;
       this.lastCameraZ = vec3d.z;
       this.lastCameraPitch = (double)n;
       this.lastCameraYaw = (double)o;
       this.client.getProfiler().swap("update");
       if (!hasForcedFrustum && this.needsTerrainUpdate) {
           this.needsTerrainUpdate = false;
           this.method_34808(frustum, frame, spectator, vec3d, blockPos, builtChunk, 16, blockPos2);
       }

       this.client.getProfiler().swap("rebuildNear");
       Set<ChunkBuilder.BuiltChunk> set = this.chunksToRebuild;
       this.chunksToRebuild = Sets.newLinkedHashSet();
       ObjectListIterator var29 = this.visibleChunks.iterator();

       while(true) {
           while(true) {
               ChunkBuilder.BuiltChunk builtChunk2;
               do {
                   if (!var29.hasNext()) {
                       this.chunksToRebuild.addAll(set);
                       this.client.getProfiler().pop();
                       return;
                   }

                   WorldRenderer.ChunkInfo chunkInfo = (WorldRenderer.ChunkInfo)var29.next();
                   builtChunk2 = ((ChunkInfoMixin)chunkInfo).getChunk();
               } while(!builtChunk2.needsRebuild() && !set.contains(builtChunk2));

               this.needsTerrainUpdate = true;
               BlockPos blockPos3 = builtChunk2.getOrigin().add(8, 8, 8);
               boolean bl = blockPos3.getSquaredDistance(blockPos) < 768.0D;
               if (!builtChunk2.needsImportantRebuild() && !bl) {
                   this.chunksToRebuild.add(builtChunk2);
               } else {
                   this.client.getProfiler().push("build near");
                   this.chunkBuilder.rebuild(builtChunk2);
                  // builtChunk2.cancelRebuild();
                   this.client.getProfiler().pop();
               }
           }
       }
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
                this.visibleChunks.ensureCapacity(4356 * world.countVerticalSections());
                this.reload();
            } else {
                this.chunksToRebuild.clear();
                this.visibleChunks.clear();
                if (this.chunks != null) {
                    this.chunks.clear();
                    this.chunks = null;
                }

                if (this.chunkBuilder != null) {
                    this.chunkBuilder.stop();
                }

                this.chunkBuilder = null;
                this.noCullingBlockEntities.clear();
            }


    }
    public void renderSkySafe(MatrixStack matrices, Matrix4f matrix4f, float f, Runnable runnable) {
            runnable.run();
            if (WorldPreview.clientWord.getDimensionEffects().getSkyType() == DimensionEffects.SkyType.NORMAL) {
                RenderSystem.disableTexture();
                Vec3d vec3d = this.world.getSkyColor(WorldPreview.camera.getPos(), f);
                float g = (float)vec3d.x;
                float h = (float)vec3d.y;
                float i = (float)vec3d.z;
                BackgroundRenderer.setFogBlack();
                BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
                RenderSystem.depthMask(false);
                RenderSystem.setShaderColor(g, h, i, 1.0F);
                Shader shader = RenderSystem.getShader();
                this.lightSkyBuffer.setShader(matrices.peek().getModel(), matrix4f, shader);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                float[] fs = this.world.getDimensionEffects().getFogColorOverride(this.world.getSkyAngle(f), f);
                float s;
                float t;
                float p;
                float q;
                float r;
                if (fs != null) {
                    RenderSystem.setShader(GameRenderer::getPositionColorShader);
                    RenderSystem.disableTexture();
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    matrices.push();
                    matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0F));
                    s = MathHelper.sin(this.world.getSkyAngleRadians(f)) < 0.0F ? 180.0F : 0.0F;
                    matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(s));
                    matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(90.0F));
                    float k = fs[0];
                    t = fs[1];
                    float m = fs[2];
                    Matrix4f matrix4f2 = matrices.peek().getModel();
                    bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
                    bufferBuilder.vertex(matrix4f2, 0.0F, 100.0F, 0.0F).color(k, t, m, fs[3]).next();

                    for(int o = 0; o <= 16; ++o) {
                        p = (float)o * 6.2831855F / 16.0F;
                        q = MathHelper.sin(p);
                        r = MathHelper.cos(p);
                        bufferBuilder.vertex(matrix4f2, q * 120.0F, r * 120.0F, -r * 40.0F * fs[3]).color(fs[0], fs[1], fs[2], 0.0F).next();
                    }

                    bufferBuilder.end();
                    BufferRenderer.draw(bufferBuilder);
                    matrices.pop();
                }

                RenderSystem.enableTexture();
                RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
                matrices.push();
                s = 1.0F - this.world.getRainGradient(f);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, s);
                matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-90.0F));
                matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(this.world.getSkyAngle(f) * 360.0F));
                Matrix4f matrix4f3 = matrices.peek().getModel();
                t = 30.0F;
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture(0, SUN);
                bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
                bufferBuilder.vertex(matrix4f3, -t, 100.0F, -t).texture(0.0F, 0.0F).next();
                bufferBuilder.vertex(matrix4f3, t, 100.0F, -t).texture(1.0F, 0.0F).next();
                bufferBuilder.vertex(matrix4f3, t, 100.0F, t).texture(1.0F, 1.0F).next();
                bufferBuilder.vertex(matrix4f3, -t, 100.0F, t).texture(0.0F, 1.0F).next();
                bufferBuilder.end();
                BufferRenderer.draw(bufferBuilder);
                t = 20.0F;
                RenderSystem.setShaderTexture(0, MOON_PHASES);
                int u = this.world.getMoonPhase();
                int v = u % 4;
                int w = u / 4 % 2;
                float x = (float)(v + 0) / 4.0F;
                p = (float)(w + 0) / 2.0F;
                q = (float)(v + 1) / 4.0F;
                r = (float)(w + 1) / 2.0F;
                bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
                bufferBuilder.vertex(matrix4f3, -t, -100.0F, t).texture(q, r).next();
                bufferBuilder.vertex(matrix4f3, t, -100.0F, t).texture(x, r).next();
                bufferBuilder.vertex(matrix4f3, t, -100.0F, -t).texture(x, p).next();
                bufferBuilder.vertex(matrix4f3, -t, -100.0F, -t).texture(q, p).next();
                bufferBuilder.end();
                BufferRenderer.draw(bufferBuilder);
                RenderSystem.disableTexture();
                float ab = this.world.method_23787(f) * s;
                if (ab > 0.0F) {
                    RenderSystem.setShaderColor(ab, ab, ab, ab);
                    BackgroundRenderer.method_23792();
                    this.starsBuffer.setShader(matrices.peek().getModel(), matrix4f, GameRenderer.getPositionShader());
                    runnable.run();
                }

                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.disableBlend();
                matrices.pop();
                RenderSystem.disableTexture();
                RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
                double d = WorldPreview.player.getCameraPosVec(f).y - this.world.getLevelProperties().getSkyDarknessHeight(this.world);
                if (d < 0.0D) {
                    matrices.push();
                    matrices.translate(0.0D, 12.0D, 0.0D);
                    this.darkSkyBuffer.setShader(matrices.peek().getModel(), matrix4f, shader);
                    matrices.pop();
                }

                if (this.world.getDimensionEffects().isAlternateSkyColor()) {
                    RenderSystem.setShaderColor(g * 0.2F + 0.04F, h * 0.2F + 0.04F, i * 0.6F + 0.1F, 1.0F);
                } else {
                    RenderSystem.setShaderColor(g, h, i, 1.0F);
                }

                RenderSystem.enableTexture();
                RenderSystem.depthMask(true);
            }
        }


    private void worldpreview_renderLayerSafe(RenderLayer renderLayer, MatrixStack matrices, double d, double e, double f, Matrix4f matrix4f) {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        renderLayer.startDrawing();
        if (renderLayer == RenderLayer.getTranslucent()) {
            this.client.getProfiler().push("translucent_sort");
            double g = d - this.lastTranslucentSortX;
            double h = e - this.lastTranslucentSortY;
            double i = f - this.lastTranslucentSortZ;
            if (g * g + h * h + i * i > 1.0D) {
                this.lastTranslucentSortX = d;
                this.lastTranslucentSortY = e;
                this.lastTranslucentSortZ = f;
                int j = 0;
                ObjectListIterator var17 = this.visibleChunks.iterator();

                while(var17.hasNext()) {
                    WorldRenderer.ChunkInfo chunkInfo = (WorldRenderer.ChunkInfo)var17.next();
                    if (j < 15 && ((ChunkInfoMixin)chunkInfo).getChunk().scheduleSort(renderLayer, this.chunkBuilder)) {
                        ++j;
                    }
                }
            }

            this.client.getProfiler().pop();
        }

        this.client.getProfiler().push("filterempty");
        this.client.getProfiler().swap(() -> {
            return "render_" + renderLayer;
        });
        boolean bl = renderLayer != RenderLayer.getTranslucent();
        ObjectListIterator<WorldRenderer.ChunkInfo> objectListIterator = this.visibleChunks.listIterator(bl ? 0 : this.visibleChunks.size());
        VertexFormat vertexFormat = renderLayer.getVertexFormat();
        Shader shader = RenderSystem.getShader();
        BufferRenderer.unbindAll();

        for(int k = 0; k < 12; ++k) {
            int l = RenderSystem.getShaderTexture(k);
            shader.addSampler("Sampler" + k, l);
        }

        if (shader.modelViewMat != null) {
            shader.modelViewMat.set(matrices.peek().getModel());
        }

        if (shader.projectionMat != null) {
            shader.projectionMat.set(matrix4f);
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
        GlUniform glUniform = shader.chunkOffset;
        boolean bl2 = false;

        while(true) {
            if (bl) {
                if (!objectListIterator.hasNext()) {
                    break;
                }
            } else if (!objectListIterator.hasPrevious()) {
                break;
            }

            WorldRenderer.ChunkInfo chunkInfo2 = bl ? (WorldRenderer.ChunkInfo)objectListIterator.next() : (WorldRenderer.ChunkInfo)objectListIterator.previous();
            ChunkBuilder.BuiltChunk builtChunk = ((ChunkInfoMixin)chunkInfo2).getChunk();
            if (!builtChunk.getData().isEmpty(renderLayer)) {
                VertexBuffer vertexBuffer = builtChunk.getBuffer(renderLayer);
                BlockPos blockPos = builtChunk.getOrigin();
                if (glUniform != null) {
                    glUniform.set((float)((double)blockPos.getX() - d), (float)((double)blockPos.getY() - e), (float)((double)blockPos.getZ() - f));
                    glUniform.upload();
                }

                vertexBuffer.drawVertices();
                bl2 = true;
            }
        }

        if (glUniform != null) {
            glUniform.set(Vec3f.ZERO);
        }

        shader.unbind();
        if (bl2) {
            vertexFormat.endDrawing();
        }
    }

}