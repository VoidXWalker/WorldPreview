package me.voidxwalker.worldpreview.mixin.client.render;

import com.google.common.collect.Sets;
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

    @Shadow protected abstract void renderLayer(RenderLayer renderLayer, MatrixStack matrices, double d, double e, double f, Matrix4f matrix4f);

    @Shadow @Final private BlockEntityRenderDispatcher blockEntityRenderDispatcher;

    @Shadow public abstract void renderClouds(MatrixStack matrices, Matrix4f matrix4f, float f, double d, double e, double g);

    @Shadow protected abstract void method_34808(Frustum frustum, int i, boolean bl, Vec3d vec3d, BlockPos blockPos, ChunkBuilder.BuiltChunk builtChunk, int j, BlockPos blockPos2);

    @Shadow private int viewDistance;

    @Shadow @Final private ObjectArrayList<WorldRenderer.ChunkInfo> visibleChunks;

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
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZIZ)V"))
    private void worldpreview_setupTerrain(WorldRenderer instance, Camera camera, Frustum frustum, boolean hasForcedFrustum, int frame, boolean spectator)  {
        if(!(client.currentScreen instanceof LevelLoadingScreen)){
            this.setupTerrain(camera,frustum,hasForcedFrustum,frame,spectator);
            return;
        }
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
                    builtChunk2 =((ChunkInfoMixin)chunkInfo).getChunk();
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
    @Inject(method = "render",at=@At(value = "INVOKE",target = "Lnet/minecraft/client/render/BufferBuilderStorage;getEntityVertexConsumers()Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;"),cancellable = true)
    public void worldpreview_render(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci){
        if(client.currentScreen instanceof  LevelLoadingScreen) {
            worldpreview_renderSafe(matrices,tickDelta,limitTime,renderBlockOutline,camera,gameRenderer,lightmapTextureManager,matrix4f);
            ci.cancel();
        }
    }
    public void worldpreview_renderSafe(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f){
        Profiler profiler = this.world.getProfiler();
        Vec3d vec3d = camera.getPos();
        double d = vec3d.getX();
        double e = vec3d.getY();
        double f = vec3d.getZ();
        int j = this.client.options.maxFps;
        long l = 33333333L;
        long n;
        if ((double)j == Option.FRAMERATE_LIMIT.getMax()) {
            n = 0L;
        } else {
            n = (long)(1000000000 / j);
        }
        Frustum frustum2;
        Matrix4f matrix4f2 = matrices.peek().getModel();

        boolean bl = this.capturedFrustum != null;
        if (bl) {
            frustum2 = this.capturedFrustum;
            frustum2.setPosition(this.capturedFrustumPosition.x, this.capturedFrustumPosition.y, this.capturedFrustumPosition.z);
        } else {
            frustum2 = new Frustum(matrix4f2, matrix4f);
            frustum2.setPosition(d, e, f);
        }
        long o = Util.getMeasuringTimeNano() - limitTime;
        long p = this.chunkUpdateSmoother.getTargetUsedTime(o);
        long q = p * 3L / 2L;
        long r = MathHelper.clamp(q, n, 33333333L);
        boolean bl3 = false;
        VertexConsumerProvider.Immediate immediate = this.bufferBuilders.getEntityVertexConsumers();
        Iterator var39 = this.world.getEntities().iterator();

        while(true) {
            Entity entity;
            int w;
            do {
                do {
                    do {
                        if (!var39.hasNext()) {
                            immediate.drawCurrentLayer();
                            this.checkEmpty(matrices);
                            immediate.draw(RenderLayer.getEntitySolid(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
                            immediate.draw(RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
                            immediate.draw(RenderLayer.getEntityCutoutNoCull(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
                            immediate.draw(RenderLayer.getEntitySmoothCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
                            profiler.swap("blockentities");
                            ObjectListIterator var53 = this.visibleChunks.iterator();

                            while(true) {
                                List list;
                                do {
                                    if (!var53.hasNext()) {
                                        synchronized(this.noCullingBlockEntities) {
                                            Iterator var57 = this.noCullingBlockEntities.iterator();

                                            while(var57.hasNext()) {
                                                BlockEntity blockEntity2 = (BlockEntity)var57.next();
                                                BlockPos blockPos2 = blockEntity2.getPos();
                                                matrices.push();
                                                matrices.translate((double)blockPos2.getX() - d, (double)blockPos2.getY() - e, (double)blockPos2.getZ() - f);
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
                                        if (bl3) {
                                            this.entityOutlineShader.render(tickDelta);
                                            this.client.getFramebuffer().beginWrite(false);
                                        }

                                        profiler.swap("destroyProgress");
                                        ObjectIterator var54 = this.blockBreakingProgressions.long2ObjectEntrySet().iterator();

                                        while(var54.hasNext()) {
                                            Long2ObjectMap.Entry<SortedSet<BlockBreakingInfo>> entry2 = (Long2ObjectMap.Entry)var54.next();
                                            BlockPos blockPos3 = BlockPos.fromLong(entry2.getLongKey());
                                            double h = (double)blockPos3.getX() - d;
                                            double x = (double)blockPos3.getY() - e;
                                            double y = (double)blockPos3.getZ() - f;
                                            if (!(h * h + x * x + y * y > 1024.0D)) {
                                                SortedSet<BlockBreakingInfo> sortedSet2 = (SortedSet)entry2.getValue();
                                                if (sortedSet2 != null && !sortedSet2.isEmpty()) {
                                                    int z = ((BlockBreakingInfo)sortedSet2.last()).getStage();
                                                    matrices.push();
                                                    matrices.translate((double)blockPos3.getX() - d, (double)blockPos3.getY() - e, (double)blockPos3.getZ() - f);
                                                    MatrixStack.Entry entry3 = matrices.peek();
                                                    VertexConsumer vertexConsumer2 = new OverlayVertexConsumer(this.bufferBuilders.getEffectVertexConsumers().getBuffer((RenderLayer)ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(z)), entry3.getModel(), entry3.getNormal());
                                                    this.client.getBlockRenderManager().renderDamage(this.world.getBlockState(blockPos3), blockPos3, this.world, matrices, vertexConsumer2);
                                                    matrices.pop();
                                                }
                                            }
                                        }

                                        this.checkEmpty(matrices);
                                        HitResult hitResult = this.client.crosshairTarget;
                                        if (renderBlockOutline && hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                                            profiler.swap("outline");
                                            BlockPos blockPos4 = ((BlockHitResult)hitResult).getBlockPos();
                                            BlockState blockState = this.world.getBlockState(blockPos4);
                                            if (!blockState.isAir() && this.world.getWorldBorder().contains(blockPos4)) {
                                                VertexConsumer vertexConsumer3 = immediate.getBuffer(RenderLayer.getLines());
                                                this.drawBlockOutline(matrices, vertexConsumer3, camera.getFocusedEntity(), d, e, f, blockPos4, blockState);
                                            }
                                        }

                                        MatrixStack matrixStack = RenderSystem.getModelViewStack();
                                        matrixStack.push();
                                        matrixStack.method_34425(matrices.peek().getModel());
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
                                            this.renderLayer(RenderLayer.getTranslucent(), matrices, d, e, f, matrix4f);
                                            profiler.swap("string");
                                            this.renderLayer(RenderLayer.getTripwire(), matrices, d, e, f, matrix4f);
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

                                            this.renderLayer(RenderLayer.getTranslucent(), matrices, d, e, f, matrix4f);
                                            immediate.draw(RenderLayer.getLines());
                                            immediate.draw();
                                            profiler.swap("string");
                                            this.renderLayer(RenderLayer.getTripwire(), matrices, d, e, f, matrix4f);
                                            profiler.swap("particles");
                                            this.client.particleManager.renderParticles(matrices, immediate, lightmapTextureManager, camera, tickDelta);
                                        }

                                        matrixStack.push();
                                        matrixStack.method_34425(matrices.peek().getModel());
                                        RenderSystem.applyModelViewMatrix();
                                        if (this.client.options.getCloudRenderMode() != CloudRenderMode.OFF) {
                                            if (this.transparencyShader != null) {
                                                this.cloudsFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
                                                RenderPhaseMixin.getCLOUDS_TARGET().startDrawing();
                                                profiler.swap("clouds");
                                                this.renderClouds(matrices, matrix4f, tickDelta, d, e, f);
                                                RenderPhaseMixin.getCLOUDS_TARGET().endDrawing();
                                            } else {
                                                profiler.swap("clouds");
                                                RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
                                                this.renderClouds(matrices, matrix4f, tickDelta, d, e, f);
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
                                        BackgroundRenderer.method_23792();
                                        return;
                                    }

                                    WorldRenderer.ChunkInfo chunkInfo = (WorldRenderer.ChunkInfo)var53.next();
                                    list = ((ChunkInfoMixin)chunkInfo).getChunk().getData().getBlockEntities();
                                } while(list.isEmpty());

                                Iterator var62 = list.iterator();

                                while(var62.hasNext()) {
                                    BlockEntity blockEntity = (BlockEntity)var62.next();
                                    BlockPos blockPos = blockEntity.getPos();
                                    VertexConsumerProvider vertexConsumerProvider3 = immediate;
                                    matrices.push();
                                    matrices.translate((double)blockPos.getX() - d, (double)blockPos.getY() - e, (double)blockPos.getZ() - f);
                                    SortedSet<BlockBreakingInfo> sortedSet = (SortedSet)this.blockBreakingProgressions.get(blockPos.asLong());
                                    if (sortedSet != null && !sortedSet.isEmpty()) {
                                        w = ((BlockBreakingInfo)sortedSet.last()).getStage();
                                        if (w >= 0) {
                                            MatrixStack.Entry entry = matrices.peek();
                                            VertexConsumer vertexConsumer = new OverlayVertexConsumer(this.bufferBuilders.getEffectVertexConsumers().getBuffer((RenderLayer)ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(w)), entry.getModel(), entry.getNormal());
                                            vertexConsumerProvider3 = (renderLayer) -> {
                                                VertexConsumer vertexConsumer2 = immediate.getBuffer(renderLayer);
                                                return renderLayer.hasCrumbling() ? VertexConsumers.union(vertexConsumer, vertexConsumer2) : vertexConsumer2;
                                            };
                                        }
                                    }
                                    this.blockEntityRenderDispatcher.render(blockEntity, tickDelta, matrices, (VertexConsumerProvider)vertexConsumerProvider3);
                                    matrices.pop();
                                }
                            }
                        }

                        entity = (Entity)var39.next();
                    } while(!this.entityRenderDispatcher.shouldRender(entity, frustum2, d, e, f) && !entity.hasPassengerDeep(WorldPreview.player));
                } while(entity == camera.getFocusedEntity() && !camera.isThirdPerson() && (!(camera.getFocusedEntity() instanceof LivingEntity) || !((LivingEntity)camera.getFocusedEntity()).isSleeping()));
            } while(entity instanceof ClientPlayerEntity && camera.getFocusedEntity() != entity);

            ++this.regularEntityCount;
            if (entity.age == 0) {
                entity.lastRenderX = entity.getX();
                entity.lastRenderY = entity.getY();
                entity.lastRenderZ = entity.getZ();
            }

            Object vertexConsumerProvider2;
            if (this.canDrawEntityOutlines() && this.client.hasOutline(entity)) {
                bl3 = true;
                OutlineVertexConsumerProvider outlineVertexConsumerProvider = this.bufferBuilders.getOutlineVertexConsumers();
                vertexConsumerProvider2 = outlineVertexConsumerProvider;
                int k = entity.getTeamColorValue();
                int t = k >> 16 & 255;
                int u = k >> 8 & 255;
                w = k & 255;
                outlineVertexConsumerProvider.setColor(t, u, w, 255);
            } else {
                vertexConsumerProvider2 = immediate;
            }

            this.renderEntity(entity, d, e, f, tickDelta, matrices, (VertexConsumerProvider)vertexConsumerProvider2);
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
        this.entityRenderDispatcher.setWorld(clientWorld);
        this.world = clientWorld;
        if (clientWorld != null) {
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
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderLayer(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/util/math/MatrixStack;DDDLnet/minecraft/util/math/Matrix4f;)V"))
    private void worldpreview_renderLayer(WorldRenderer instance, RenderLayer renderLayer, MatrixStack matrices, double d, double e, double f, Matrix4f matrix4f) {
        if (!(client.currentScreen instanceof LevelLoadingScreen)) {
            this.renderLayer(renderLayer, matrices, d, e, f,matrix4f);
            return;
        }
        this.worldpreview_renderLayerSafe(renderLayer,matrices,d,e,f,matrix4f);
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

    @Redirect(method = "renderSky", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;world:Lnet/minecraft/client/world/ClientWorld;", opcode = Opcodes.GETFIELD))
    public ClientWorld worldpreview_getCorrectWorld4(MinecraftClient instance){
        if(instance.world==null&&client.currentScreen instanceof LevelLoadingScreen){
            return this.world;
        }
        return instance.world;

    }

    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;getCamera()Lnet/minecraft/client/render/Camera;"))
    public Camera worldpreview_getCamera(GameRenderer instance){
        if(instance.getCamera()==null&&client.currentScreen instanceof LevelLoadingScreen){
            return WorldPreview.camera;
        }
        return  instance.getCamera();
    }

    @Redirect(method = "renderSky", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;player:Lnet/minecraft/client/network/ClientPlayerEntity;", opcode = Opcodes.GETFIELD))
    public ClientPlayerEntity worldpreview_getCorrectPlayer3(MinecraftClient instance){
        if(instance.player==null&&client.currentScreen instanceof LevelLoadingScreen){
            return WorldPreview.player;
        }
        return instance.player ;
    }
}