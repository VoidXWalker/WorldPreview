package me.voidxwalker.worldpreview;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;

import me.voidxwalker.worldpreview.mixin.access.BuiltChunkStorageMixin;
import me.voidxwalker.worldpreview.mixin.access.RenderPhaseMixin;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.Option;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.FluidTags;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.Vector4f;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.BlockView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class PreviewRenderer {
	private static final Identifier MOON_PHASES = new Identifier("textures/environment/moon_phases.png");
	private static final Identifier SUN = new Identifier("textures/environment/sun.png");
	private static final Identifier CLOUDS = new Identifier("textures/environment/clouds.png");
	private static final Identifier END_SKY = new Identifier("textures/environment/end_sky.png");
	private static final Identifier RAIN = new Identifier("textures/environment/rain.png");
	private static final Identifier SNOW = new Identifier("textures/environment/snow.png");
	public static final Direction[] DIRECTIONS = Direction.values();
	private final MinecraftClient client;
	private final EntityRenderDispatcher entityRenderDispatcher;
	private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
	private final BufferBuilderStorage bufferBuilders;
	public ClientWorld world;
	private Set<ChunkBuilder.BuiltChunk> chunksToRebuild = Sets.newLinkedHashSet();
	private final ObjectArrayList<PreviewRenderer.ChunkInfo> visibleChunks = new ObjectArrayList();
	private final Set<BlockEntity> noCullingBlockEntities = Sets.newHashSet();
	private BuiltChunkStorage chunks;
	private PreviewRenderer.ChunkInfoList chunkInfos;
	@Nullable
	private VertexBuffer starsBuffer;
	@Nullable
	private VertexBuffer lightSkyBuffer;
	@Nullable
	private VertexBuffer darkSkyBuffer;
	private boolean cloudsDirty = true;
	@Nullable
	private VertexBuffer cloudsBuffer;
	private final FpsSmoother chunkUpdateSmoother = new FpsSmoother(100);
	public int ticks;
	private final Int2ObjectMap<BlockBreakingInfo> blockBreakingInfos = new Int2ObjectOpenHashMap();
	private final Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions = new Long2ObjectOpenHashMap();
	@Nullable
	private Framebuffer translucentFramebuffer;
	@Nullable
	private Framebuffer entityFramebuffer;
	@Nullable
	private Framebuffer particlesFramebuffer;
	@Nullable
	private Framebuffer weatherFramebuffer;
	@Nullable
	private Framebuffer cloudsFramebuffer;
	@Nullable
	private ShaderEffect transparencyShader;
	private double lastCameraChunkUpdateX = Double.MIN_VALUE;
	private double lastCameraChunkUpdateY = Double.MIN_VALUE;
	private double lastCameraChunkUpdateZ = Double.MIN_VALUE;
	private int cameraChunkX = Integer.MIN_VALUE;
	private int cameraChunkY = Integer.MIN_VALUE;
	private int cameraChunkZ = Integer.MIN_VALUE;
	private double lastCameraX = Double.MIN_VALUE;
	private double lastCameraY = Double.MIN_VALUE;
	private double lastCameraZ = Double.MIN_VALUE;
	private double lastCameraPitch = Double.MIN_VALUE;
	private double lastCameraYaw = Double.MIN_VALUE;
	private int lastCloudsBlockX = Integer.MIN_VALUE;
	private int lastCloudsBlockY = Integer.MIN_VALUE;
	private int lastCloudsBlockZ = Integer.MIN_VALUE;
	private Vec3d lastCloudsColor;
	private CloudRenderMode lastCloudsRenderMode;
	private ChunkBuilder chunkBuilder;
	private int viewDistance;
	private int regularEntityCount;
	private int blockEntityCount;
	private Frustum frustum;
	private boolean shouldCaptureFrustum;
	@Nullable
	private Frustum capturedFrustum;
	private final Vector4f[] capturedFrustumOrientation;
	private final Vector3d capturedFrustumPosition;
	private double lastTranslucentSortX;
	private double lastTranslucentSortY;
	private double lastTranslucentSortZ;
	private boolean needsTerrainUpdate;
	private int frame;
	private final float[] field_20794;
	private final float[] field_20795;

	public PreviewRenderer(MinecraftClient client, BufferBuilderStorage bufferBuilders) {
		this.lastCloudsColor = Vec3d.ZERO;
		this.viewDistance = -1;
		this.capturedFrustumOrientation = new Vector4f[8];
		this.capturedFrustumPosition = new Vector3d(0.0D, 0.0D, 0.0D);
		this.needsTerrainUpdate = true;
		this.field_20794 = new float[1024];
		this.field_20795 = new float[1024];
		this.client = client;
		this.entityRenderDispatcher = client.getEntityRenderDispatcher();
		this.blockEntityRenderDispatcher = client.getBlockEntityRenderDispatcher();
		this.bufferBuilders = bufferBuilders;

		for(int i = 0; i < 32; ++i) {
			for(int j = 0; j < 32; ++j) {
				float f = (float)(j - 16);
				float g = (float)(i - 16);
				float h = MathHelper.sqrt(f * f + g * g);
				this.field_20794[i << 5 | j] = -g / h;
				this.field_20795[i << 5 | j] = f / h;
			}
		}

		this.renderLightSky();

	}















	private void renderLightSky() {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		if (this.lightSkyBuffer != null) {
			this.lightSkyBuffer.close();
		}

		this.lightSkyBuffer = new VertexBuffer();
		method_34550(bufferBuilder, 16.0F);
		this.lightSkyBuffer.upload(bufferBuilder);
	}

	private static void method_34550(BufferBuilder bufferBuilder, float f) {
		float g = Math.signum(f) * 512.0F;
		float h = 512.0F;
		RenderSystem.setShader(GameRenderer::getPositionShader);
		bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION);
		bufferBuilder.vertex(0.0D, (double)f, 0.0D).next();

		for(int i = -180; i <= 180; i += 45) {
			bufferBuilder.vertex((double)(g * MathHelper.cos((float)i * 0.017453292F)), (double)f, (double)(512.0F * MathHelper.sin((float)i * 0.017453292F))).next();
		}

		bufferBuilder.end();
	}




	public void setWorld(@Nullable ClientWorld world) {
		this.lastCameraChunkUpdateX = Double.MIN_VALUE;
		this.lastCameraChunkUpdateY = Double.MIN_VALUE;
		this.lastCameraChunkUpdateZ = Double.MIN_VALUE;
		this.cameraChunkX = Integer.MIN_VALUE;
		this.cameraChunkY = Integer.MIN_VALUE;
		this.cameraChunkZ = Integer.MIN_VALUE;
		this.entityRenderDispatcher.setWorld(world);
		this.world = world;
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



	public void reload() {
		if (this.world != null) {
			this.world.reloadColor();
			if (this.chunkBuilder == null) {
				this.chunkBuilder = new ChunkBuilder(this.world, null, Util.getMainWorkerExecutor(), this.client.is64Bit(), this.bufferBuilders.getBlockBufferBuilders());
			} else {
				this.chunkBuilder.setWorld(this.world);
			}

			this.needsTerrainUpdate = true;
			this.cloudsDirty = true;
			RenderLayers.setFancyGraphicsOrBetter(MinecraftClient.isFancyGraphicsOrBetter());
			this.viewDistance = this.client.options.viewDistance*16;
			if (this.chunks != null) {
				this.chunks.clear();
			}

			this.clearChunkRenderers();
			synchronized(this.noCullingBlockEntities) {
				this.noCullingBlockEntities.clear();
			}

			this.chunks = new BuiltChunkStorage(this.chunkBuilder, this.world, this.client.options.viewDistance, null);
			this.chunkInfos = new PreviewRenderer.ChunkInfoList(this.chunks.chunks.length);
			if (this.world != null) {
				Entity entity = WorldPreview.player;
				if (entity != null) {
					this.chunks.updateCameraPosition(entity.getX(), entity.getZ());
				}
			}

		}
	}

	protected void clearChunkRenderers() {
		this.chunksToRebuild.clear();
		this.chunkBuilder.reset();
	}









	private void setupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, int frame, boolean spectator) {
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

					PreviewRenderer.ChunkInfo chunkInfo = (PreviewRenderer.ChunkInfo)var29.next();
					builtChunk2 = chunkInfo.chunk;
				} while(!builtChunk2.needsRebuild() && !set.contains(builtChunk2));

				this.needsTerrainUpdate = true;
				BlockPos blockPos3 = builtChunk2.getOrigin().add(8, 8, 8);
				boolean bl = blockPos3.getSquaredDistance(blockPos) < 768.0D;
				if (!builtChunk2.needsImportantRebuild() && !bl) {
					this.chunksToRebuild.add(builtChunk2);
				} else {
					this.client.getProfiler().push("build near");
					this.chunkBuilder.rebuild(builtChunk2);
					this.client.getProfiler().pop();
				}
			}
		}
	}

	private void method_34808(Frustum frustum, int i, boolean bl, Vec3d vec3d, BlockPos blockPos, ChunkBuilder.BuiltChunk builtChunk, int j, BlockPos blockPos2) {
		this.visibleChunks.clear();
		Queue<PreviewRenderer.ChunkInfo> queue = Queues.newArrayDeque();
		Entity.setRenderDistanceMultiplier(MathHelper.clamp((double)this.client.options.viewDistance / 8.0D, 1.0D, 2.5D) * (double)this.client.options.entityDistanceScaling);
		boolean bl2 = this.client.chunkCullingEnabled;
		int k;
		int n;
		int o;
		if (builtChunk == null) {
			k = blockPos.getY() > this.world.getBottomY() ? this.world.getTopY() - 8 : this.world.getBottomY() + 8;
			int l = MathHelper.floor(vec3d.x / (double)j) * j;
			int m = MathHelper.floor(vec3d.z / (double)j) * j;
			List<PreviewRenderer.ChunkInfo> list = Lists.newArrayList();

			for(n = -this.viewDistance; n <= this.viewDistance; ++n) {
				for(o = -this.viewDistance; o <= this.viewDistance; ++o) {
					ChunkBuilder.BuiltChunk builtChunk2 =((BuiltChunkStorageMixin)this.chunks).callGetRenderedChunk(new BlockPos(l + ChunkSectionPos.getOffsetPos(n, 8), k, m + ChunkSectionPos.getOffsetPos(o, 8)));
					if (builtChunk2 != null && frustum.isVisible(builtChunk2.boundingBox)) {
						builtChunk2.setRebuildFrame(i);
						list.add(new PreviewRenderer.ChunkInfo(builtChunk2, (Direction)null, 0));
					}
				}
			}

			list.sort(Comparator.comparingDouble((chunkInfox) -> {
				return blockPos.getSquaredDistance(chunkInfox.chunk.getOrigin().add(8, 8, 8));
			}));
			queue.addAll(list);
		} else {
			if (bl && this.world.getBlockState(blockPos).isOpaqueFullCube(this.world, blockPos)) {
				bl2 = false;
			}

			builtChunk.setRebuildFrame(i);
			queue.add(new PreviewRenderer.ChunkInfo(builtChunk, (Direction)null, 0));
		}

		this.client.getProfiler().push("iteration");
		k = this.client.options.viewDistance;
		this.chunkInfos.update();

		while(!queue.isEmpty()) {
			PreviewRenderer.ChunkInfo chunkInfo = (PreviewRenderer.ChunkInfo)queue.poll();
			ChunkBuilder.BuiltChunk builtChunk3 = chunkInfo.chunk;
			this.visibleChunks.add(chunkInfo);
			Direction[] var24 = DIRECTIONS;
			n = var24.length;

			for(o = 0; o < n; ++o) {
				Direction direction = var24[o];
				ChunkBuilder.BuiltChunk builtChunk4 = this.getAdjacentChunk(blockPos2, builtChunk3, direction);
				if (!bl2 || !chunkInfo.canCull(direction.getOpposite())) {
					if (bl2 && chunkInfo.hasAnyDirection()) {
						ChunkBuilder.ChunkData chunkData = builtChunk3.getData();
						boolean bl3 = false;

						for(int q = 0; q < DIRECTIONS.length; ++q) {
							if (chunkInfo.hasDirection(q) && chunkData.isVisibleThrough(DIRECTIONS[q].getOpposite(), direction)) {
								bl3 = true;
								break;
							}
						}

						if (!bl3) {
							continue;
						}
					}

					if (builtChunk4 != null && builtChunk4.shouldBuild()) {
						PreviewRenderer.ChunkInfo chunkInfo2;
						if (!builtChunk4.setRebuildFrame(i)) {
							chunkInfo2 = this.chunkInfos.getInfo(builtChunk4);
							if (chunkInfo2 != null) {
								chunkInfo2.addDirection(direction);
							}
						} else if (frustum.isVisible(builtChunk4.boundingBox)) {
							chunkInfo2 = new PreviewRenderer.ChunkInfo(builtChunk4, direction, chunkInfo.propagationLevel + 1);
							chunkInfo2.updateCullingState(chunkInfo.cullingState, direction);
							queue.add(chunkInfo2);
							this.chunkInfos.setInfo(builtChunk4, chunkInfo2);
						}
					}
				}
			}
		}

		this.client.getProfiler().pop();
	}

	@Nullable
	private ChunkBuilder.BuiltChunk getAdjacentChunk(BlockPos pos, ChunkBuilder.BuiltChunk chunk, Direction direction) {
		BlockPos blockPos = chunk.getNeighborPosition(direction);
		if (MathHelper.abs(pos.getX() - blockPos.getX()) > this.viewDistance * 16) {
			return null;
		} else if (blockPos.getY() >= this.world.getBottomY() && blockPos.getY() < this.world.getTopY()) {
			return MathHelper.abs(pos.getZ() - blockPos.getZ()) > this.viewDistance * 16 ? null :((BuiltChunkStorageMixin)this.chunks).callGetRenderedChunk(blockPos);
		} else {
			return null;
		}
	}

	private void captureFrustum(Matrix4f modelMatrix, Matrix4f matrix4f, double x, double y, double z, Frustum frustum) {
		this.capturedFrustum = frustum;
		Matrix4f matrix4f2 = matrix4f.copy();
		matrix4f2.multiply(modelMatrix);
		matrix4f2.invert();
		this.capturedFrustumPosition.x = x;
		this.capturedFrustumPosition.y = y;
		this.capturedFrustumPosition.z = z;
		this.capturedFrustumOrientation[0] = new Vector4f(-1.0F, -1.0F, -1.0F, 1.0F);
		this.capturedFrustumOrientation[1] = new Vector4f(1.0F, -1.0F, -1.0F, 1.0F);
		this.capturedFrustumOrientation[2] = new Vector4f(1.0F, 1.0F, -1.0F, 1.0F);
		this.capturedFrustumOrientation[3] = new Vector4f(-1.0F, 1.0F, -1.0F, 1.0F);
		this.capturedFrustumOrientation[4] = new Vector4f(-1.0F, -1.0F, 1.0F, 1.0F);
		this.capturedFrustumOrientation[5] = new Vector4f(1.0F, -1.0F, 1.0F, 1.0F);
		this.capturedFrustumOrientation[6] = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.capturedFrustumOrientation[7] = new Vector4f(-1.0F, 1.0F, 1.0F, 1.0F);

		for(int i = 0; i < 8; ++i) {
			this.capturedFrustumOrientation[i].transform(matrix4f2);
			this.capturedFrustumOrientation[i].normalizeProjectiveCoordinates();
		}

	}

	public void setupFrustum(MatrixStack matrices, Vec3d pos, Matrix4f projectionMatrix) {
		Matrix4f matrix4f = matrices.peek().getModel();
		double d = pos.getX();
		double e = pos.getY();
		double f = pos.getZ();
		this.frustum = new Frustum(matrix4f, projectionMatrix);
		this.frustum.setPosition(d, e, f);
	}

	public void render(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f) {
		RenderSystem.setShaderGameTime(this.world.getTime(), tickDelta);
		this.blockEntityRenderDispatcher.configure(this.world, camera,null);
		this.entityRenderDispatcher.configure(this.world, camera,null);
		Profiler profiler = this.world.getProfiler();
		profiler.swap("light_updates");
		this.world.getChunkManager().getLightingProvider().doLightUpdates(Integer.MAX_VALUE, true, true);
		Vec3d vec3d = camera.getPos();
		double d = vec3d.getX();
		double e = vec3d.getY();
		double f = vec3d.getZ();
		Matrix4f matrix4f2 = matrices.peek().getModel();
		profiler.swap("culling");
		boolean bl = this.capturedFrustum != null;
		Frustum frustum2;
		if (bl) {
			frustum2 = this.capturedFrustum;
			frustum2.setPosition(this.capturedFrustumPosition.x, this.capturedFrustumPosition.y, this.capturedFrustumPosition.z);
		} else {
			frustum2 = this.frustum;
		}

		this.client.getProfiler().swap("captureFrustum");
		if (this.shouldCaptureFrustum) {
			this.captureFrustum(matrix4f2, matrix4f, vec3d.x, vec3d.y, vec3d.z, bl ? new Frustum(matrix4f2, matrix4f) : frustum2);
			this.shouldCaptureFrustum = false;
		}

		profiler.swap("clear");
		BackgroundRenderer.render(camera, tickDelta, this.world, this.client.options.viewDistance*16, 0F);
		BackgroundRenderer.setFogBlack();
		RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);
		float g = client.options.viewDistance*16;
		boolean bl2 = false;
		profiler.swap("sky");
		RenderSystem.setShader(GameRenderer::getPositionShader);
		this.renderSky(matrices, matrix4f, tickDelta, () -> {
			BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_SKY, g, bl2);
		});
		profiler.swap("fog");
		BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_TERRAIN, Math.max(g - 16.0F, 32.0F), bl2);
		profiler.swap("terrain_setup");
		this.setupTerrain(camera, frustum2, bl, this.frame++,false);
		profiler.swap("updatechunks");
		int j = this.client.options.maxFps;
		long l = 33333333L;
		long n;
		if ((double)j == Option.FRAMERATE_LIMIT.getMax()) {
			n = 0L;
		} else {
			n = (long)(1000000000 / j);
		}

		long o = Util.getMeasuringTimeNano() - limitTime;
		long p = this.chunkUpdateSmoother.getTargetUsedTime(o);
		long q = p * 3L / 2L;
		long r = MathHelper.clamp(q, n, 33333333L);
		this.updateChunks(limitTime + r);
		profiler.swap("terrain");
		this.renderLayer(RenderLayer.getSolid(), matrices, d, e, f, matrix4f);
		this.renderLayer(RenderLayer.getCutoutMipped(), matrices, d, e, f, matrix4f);
		this.renderLayer(RenderLayer.getCutout(), matrices, d, e, f, matrix4f);
		if (this.world.getDimensionEffects().isDarkened()) {
			DiffuseLighting.enableForLevel(matrices.peek().getModel());
		} else {
			DiffuseLighting.disableForLevel(matrices.peek().getModel());
		}

		profiler.swap("entities");
		this.regularEntityCount = 0;
		this.blockEntityCount = 0;


		if (this.weatherFramebuffer != null) {
			this.weatherFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
		}


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




										this.checkEmpty(matrices);

										MatrixStack matrixStack = RenderSystem.getModelViewStack();
										matrixStack.push();
										matrixStack.method_34425(matrices.peek().getModel());
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


										this.renderChunkDebugInfo(camera);
										RenderSystem.depthMask(true);
										RenderSystem.disableBlend();
										matrixStack.pop();
										RenderSystem.applyModelViewMatrix();
										BackgroundRenderer.method_23792();
										return;
									}

									PreviewRenderer.ChunkInfo chunkInfo = (PreviewRenderer.ChunkInfo)var53.next();
									list = chunkInfo.chunk.getData().getBlockEntities();
								} while(list.isEmpty());


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

				vertexConsumerProvider2 = immediate;


			this.renderEntity(entity, d, e, f, tickDelta, matrices, (VertexConsumerProvider)vertexConsumerProvider2);
		}
	}

	private void checkEmpty(MatrixStack matrices) {
		if (!matrices.isEmpty()) {
			throw new IllegalStateException("Pose stack not empty");
		}
	}

	private void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
		double d = MathHelper.lerp((double)tickDelta, entity.lastRenderX, entity.getX());
		double e = MathHelper.lerp((double)tickDelta, entity.lastRenderY, entity.getY());
		double f = MathHelper.lerp((double)tickDelta, entity.lastRenderZ, entity.getZ());
		float g = MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw());
		this.entityRenderDispatcher.render(entity, d - cameraX, e - cameraY, f - cameraZ, g, tickDelta, matrices, vertexConsumers, this.entityRenderDispatcher.getLight(entity, tickDelta));
	}

	private void renderLayer(RenderLayer renderLayer, MatrixStack matrices, double d, double e, double f, Matrix4f matrix4f) {
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
					PreviewRenderer.ChunkInfo chunkInfo = (PreviewRenderer.ChunkInfo)var17.next();
					if (j < 15 && chunkInfo.chunk.scheduleSort(renderLayer, this.chunkBuilder)) {
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
		ObjectListIterator<PreviewRenderer.ChunkInfo> objectListIterator = this.visibleChunks.listIterator(bl ? 0 : this.visibleChunks.size());
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

			PreviewRenderer.ChunkInfo chunkInfo2 = bl ? (PreviewRenderer.ChunkInfo)objectListIterator.next() : (PreviewRenderer.ChunkInfo)objectListIterator.previous();
			ChunkBuilder.BuiltChunk builtChunk = chunkInfo2.chunk;
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

		VertexBuffer.unbind();
		VertexBuffer.unbindVertexArray();
		this.client.getProfiler().pop();
		renderLayer.endDrawing();
	}

	private void renderChunkDebugInfo(Camera camera) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);


		if (this.capturedFrustum != null) {
			RenderSystem.disableCull();
			RenderSystem.disableTexture();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.lineWidth(10.0F);
			MatrixStack matrixStack2 = RenderSystem.getModelViewStack();
			matrixStack2.push();
			matrixStack2.translate((double)((float)(this.capturedFrustumPosition.x - camera.getPos().x)), (double)((float)(this.capturedFrustumPosition.y - camera.getPos().y)), (double)((float)(this.capturedFrustumPosition.z - camera.getPos().z)));
			RenderSystem.applyModelViewMatrix();
			RenderSystem.depthMask(true);
			bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
			this.method_22985(bufferBuilder, 0, 1, 2, 3, 0, 1, 1);
			this.method_22985(bufferBuilder, 4, 5, 6, 7, 1, 0, 0);
			this.method_22985(bufferBuilder, 0, 1, 5, 4, 1, 1, 0);
			this.method_22985(bufferBuilder, 2, 3, 7, 6, 0, 0, 1);
			this.method_22985(bufferBuilder, 0, 4, 7, 3, 0, 1, 0);
			this.method_22985(bufferBuilder, 1, 5, 6, 2, 1, 0, 1);
			tessellator.draw();
			RenderSystem.depthMask(false);
			RenderSystem.setShader(GameRenderer::getPositionShader);
			bufferBuilder.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			this.method_22984(bufferBuilder, 0);
			this.method_22984(bufferBuilder, 1);
			this.method_22984(bufferBuilder, 1);
			this.method_22984(bufferBuilder, 2);
			this.method_22984(bufferBuilder, 2);
			this.method_22984(bufferBuilder, 3);
			this.method_22984(bufferBuilder, 3);
			this.method_22984(bufferBuilder, 0);
			this.method_22984(bufferBuilder, 4);
			this.method_22984(bufferBuilder, 5);
			this.method_22984(bufferBuilder, 5);
			this.method_22984(bufferBuilder, 6);
			this.method_22984(bufferBuilder, 6);
			this.method_22984(bufferBuilder, 7);
			this.method_22984(bufferBuilder, 7);
			this.method_22984(bufferBuilder, 4);
			this.method_22984(bufferBuilder, 0);
			this.method_22984(bufferBuilder, 4);
			this.method_22984(bufferBuilder, 1);
			this.method_22984(bufferBuilder, 5);
			this.method_22984(bufferBuilder, 2);
			this.method_22984(bufferBuilder, 6);
			this.method_22984(bufferBuilder, 3);
			this.method_22984(bufferBuilder, 7);
			tessellator.draw();
			matrixStack2.pop();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.depthMask(true);
			RenderSystem.disableBlend();
			RenderSystem.enableCull();
			RenderSystem.enableTexture();
			RenderSystem.lineWidth(1.0F);
		}

	}

	private void method_22984(VertexConsumer vertexConsumer, int i) {
		vertexConsumer.vertex((double)this.capturedFrustumOrientation[i].getX(), (double)this.capturedFrustumOrientation[i].getY(), (double)this.capturedFrustumOrientation[i].getZ()).next();
	}

	private void method_22985(VertexConsumer vertexConsumer, int i, int j, int k, int l, int m, int n, int o) {
		float f = 0.25F;
		vertexConsumer.vertex((double)this.capturedFrustumOrientation[i].getX(), (double)this.capturedFrustumOrientation[i].getY(), (double)this.capturedFrustumOrientation[i].getZ()).color((float)m, (float)n, (float)o, 0.25F).next();
		vertexConsumer.vertex((double)this.capturedFrustumOrientation[j].getX(), (double)this.capturedFrustumOrientation[j].getY(), (double)this.capturedFrustumOrientation[j].getZ()).color((float)m, (float)n, (float)o, 0.25F).next();
		vertexConsumer.vertex((double)this.capturedFrustumOrientation[k].getX(), (double)this.capturedFrustumOrientation[k].getY(), (double)this.capturedFrustumOrientation[k].getZ()).color((float)m, (float)n, (float)o, 0.25F).next();
		vertexConsumer.vertex((double)this.capturedFrustumOrientation[l].getX(), (double)this.capturedFrustumOrientation[l].getY(), (double)this.capturedFrustumOrientation[l].getZ()).color((float)m, (float)n, (float)o, 0.25F).next();
	}

	public void captureFrustum() {
		this.shouldCaptureFrustum = true;
	}

	public void killFrustum() {
		this.capturedFrustum = null;
	}

	public void tick() {
		++this.ticks;
		if (this.ticks % 20 == 0) {
			ObjectIterator iterator = this.blockBreakingInfos.values().iterator();

			while(iterator.hasNext()) {
				BlockBreakingInfo blockBreakingInfo = (BlockBreakingInfo)iterator.next();
				int i = blockBreakingInfo.getLastUpdateTick();
				if (this.ticks - i > 400) {
					iterator.remove();
					this.removeBlockBreakingInfo(blockBreakingInfo);
				}
			}

		}
	}

	private void removeBlockBreakingInfo(BlockBreakingInfo info) {
		long l = info.getPos().asLong();
		Set<BlockBreakingInfo> set = (Set)this.blockBreakingProgressions.get(l);
		set.remove(info);
		if (set.isEmpty()) {
			this.blockBreakingProgressions.remove(l);
		}

	}

	private void renderEndSky(MatrixStack matrices) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(false);
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, END_SKY);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();

		for(int i = 0; i < 6; ++i) {
			matrices.push();
			if (i == 1) {
				matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0F));
			}

			if (i == 2) {
				matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-90.0F));
			}

			if (i == 3) {
				matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(180.0F));
			}

			if (i == 4) {
				matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(90.0F));
			}

			if (i == 5) {
				matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(-90.0F));
			}

			Matrix4f matrix4f = matrices.peek().getModel();
			bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
			bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, -100.0F).texture(0.0F, 0.0F).color(40, 40, 40, 255).next();
			bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, 100.0F).texture(0.0F, 16.0F).color(40, 40, 40, 255).next();
			bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, 100.0F).texture(16.0F, 16.0F).color(40, 40, 40, 255).next();
			bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, -100.0F).texture(16.0F, 0.0F).color(40, 40, 40, 255).next();
			tessellator.draw();
			matrices.pop();
		}

		RenderSystem.depthMask(true);
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
	}

	public void renderSky(MatrixStack matrices, Matrix4f matrix4f, float f, Runnable runnable) {
		runnable.run();
		if (this.world.getDimensionEffects().getSkyType() == DimensionEffects.SkyType.END) {
			this.renderEndSky(matrices);
		} else if (this.world.getDimensionEffects().getSkyType() == DimensionEffects.SkyType.NORMAL) {
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


			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.disableBlend();
			matrices.pop();
			RenderSystem.disableTexture();
			RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);

			if (this.world.getDimensionEffects().isAlternateSkyColor()) {
				RenderSystem.setShaderColor(g * 0.2F + 0.04F, h * 0.2F + 0.04F, i * 0.6F + 0.1F, 1.0F);
			} else {
				RenderSystem.setShaderColor(g, h, i, 1.0F);
			}

			RenderSystem.enableTexture();
			RenderSystem.depthMask(true);
		}
	}

	public void renderClouds(MatrixStack matrices, Matrix4f matrix4f, float f, double d, double e, double g) {
		float h = this.world.getDimensionEffects().getCloudsHeight();
		if (!Float.isNaN(h)) {
			RenderSystem.disableCull();
			RenderSystem.enableBlend();
			RenderSystem.enableDepthTest();
			RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.depthMask(true);
			float i = 12.0F;
			float j = 4.0F;
			double k = 2.0E-4D;
			double l = (double)(((float)this.ticks + f) * 0.03F);
			double m = (d + l) / 12.0D;
			double n = (double)(h - (float)e + 0.33F);
			double o = g / 12.0D + 0.33000001311302185D;
			m -= (double)(MathHelper.floor(m / 2048.0D) * 2048);
			o -= (double)(MathHelper.floor(o / 2048.0D) * 2048);
			float p = (float)(m - (double)MathHelper.floor(m));
			float q = (float)(n / 4.0D - (double)MathHelper.floor(n / 4.0D)) * 4.0F;
			float r = (float)(o - (double)MathHelper.floor(o));
			Vec3d vec3d = this.world.getCloudsColor(f);
			int s = (int)Math.floor(m);
			int t = (int)Math.floor(n / 4.0D);
			int u = (int)Math.floor(o);
			if (s != this.lastCloudsBlockX || t != this.lastCloudsBlockY || u != this.lastCloudsBlockZ || this.client.options.getCloudRenderMode() != this.lastCloudsRenderMode || this.lastCloudsColor.squaredDistanceTo(vec3d) > 2.0E-4D) {
				this.lastCloudsBlockX = s;
				this.lastCloudsBlockY = t;
				this.lastCloudsBlockZ = u;
				this.lastCloudsColor = vec3d;
				this.lastCloudsRenderMode = this.client.options.getCloudRenderMode();
				this.cloudsDirty = true;
			}

			if (this.cloudsDirty) {
				this.cloudsDirty = false;
				BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
				if (this.cloudsBuffer != null) {
					this.cloudsBuffer.close();
				}

				this.cloudsBuffer = new VertexBuffer();
				this.renderClouds(bufferBuilder, m, n, o, vec3d);
				bufferBuilder.end();
				this.cloudsBuffer.upload(bufferBuilder);
			}

			RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
			RenderSystem.setShaderTexture(0, CLOUDS);
			BackgroundRenderer.setFogBlack();
			matrices.push();
			matrices.scale(12.0F, 1.0F, 12.0F);
			matrices.translate((double)(-p), (double)q, (double)(-r));
			if (this.cloudsBuffer != null) {
				int v = this.lastCloudsRenderMode == CloudRenderMode.FANCY ? 0 : 1;

				for(int w = v; w < 2; ++w) {
					if (w == 0) {
						RenderSystem.colorMask(false, false, false, false);
					} else {
						RenderSystem.colorMask(true, true, true, true);
					}

					Shader shader = RenderSystem.getShader();
					this.cloudsBuffer.setShader(matrices.peek().getModel(), matrix4f, shader);
				}
			}

			matrices.pop();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.enableCull();
			RenderSystem.disableBlend();
		}
	}

	private void renderClouds(BufferBuilder builder, double x, double y, double z, Vec3d color) {
		float k = (float)MathHelper.floor(x) * 0.00390625F;
		float l = (float)MathHelper.floor(z) * 0.00390625F;
		float m = (float)color.x;
		float n = (float)color.y;
		float o = (float)color.z;
		float p = m * 0.9F;
		float q = n * 0.9F;
		float r = o * 0.9F;
		float s = m * 0.7F;
		float t = n * 0.7F;
		float u = o * 0.7F;
		float v = m * 0.8F;
		float w = n * 0.8F;
		float aa = o * 0.8F;
		RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
		builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);
		float ab = (float)Math.floor(y / 4.0D) * 4.0F;
		if (this.lastCloudsRenderMode == CloudRenderMode.FANCY) {
			for(int ac = -3; ac <= 4; ++ac) {
				for(int ad = -3; ad <= 4; ++ad) {
					float ae = (float)(ac * 8);
					float af = (float)(ad * 8);
					if (ab > -5.0F) {
						builder.vertex((double)(ae + 0.0F), (double)(ab + 0.0F), (double)(af + 8.0F)).texture((ae + 0.0F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(s, t, u, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
						builder.vertex((double)(ae + 8.0F), (double)(ab + 0.0F), (double)(af + 8.0F)).texture((ae + 8.0F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(s, t, u, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
						builder.vertex((double)(ae + 8.0F), (double)(ab + 0.0F), (double)(af + 0.0F)).texture((ae + 8.0F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(s, t, u, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
						builder.vertex((double)(ae + 0.0F), (double)(ab + 0.0F), (double)(af + 0.0F)).texture((ae + 0.0F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(s, t, u, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
					}

					if (ab <= 5.0F) {
						builder.vertex((double)(ae + 0.0F), (double)(ab + 4.0F - 9.765625E-4F), (double)(af + 8.0F)).texture((ae + 0.0F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, 1.0F, 0.0F).next();
						builder.vertex((double)(ae + 8.0F), (double)(ab + 4.0F - 9.765625E-4F), (double)(af + 8.0F)).texture((ae + 8.0F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, 1.0F, 0.0F).next();
						builder.vertex((double)(ae + 8.0F), (double)(ab + 4.0F - 9.765625E-4F), (double)(af + 0.0F)).texture((ae + 8.0F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, 1.0F, 0.0F).next();
						builder.vertex((double)(ae + 0.0F), (double)(ab + 4.0F - 9.765625E-4F), (double)(af + 0.0F)).texture((ae + 0.0F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, 1.0F, 0.0F).next();
					}

					int aj;
					if (ac > -1) {
						for(aj = 0; aj < 8; ++aj) {
							builder.vertex((double)(ae + (float)aj + 0.0F), (double)(ab + 0.0F), (double)(af + 8.0F)).texture((ae + (float)aj + 0.5F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(-1.0F, 0.0F, 0.0F).next();
							builder.vertex((double)(ae + (float)aj + 0.0F), (double)(ab + 4.0F), (double)(af + 8.0F)).texture((ae + (float)aj + 0.5F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(-1.0F, 0.0F, 0.0F).next();
							builder.vertex((double)(ae + (float)aj + 0.0F), (double)(ab + 4.0F), (double)(af + 0.0F)).texture((ae + (float)aj + 0.5F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(-1.0F, 0.0F, 0.0F).next();
							builder.vertex((double)(ae + (float)aj + 0.0F), (double)(ab + 0.0F), (double)(af + 0.0F)).texture((ae + (float)aj + 0.5F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(-1.0F, 0.0F, 0.0F).next();
						}
					}

					if (ac <= 1) {
						for(aj = 0; aj < 8; ++aj) {
							builder.vertex((double)(ae + (float)aj + 1.0F - 9.765625E-4F), (double)(ab + 0.0F), (double)(af + 8.0F)).texture((ae + (float)aj + 0.5F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(1.0F, 0.0F, 0.0F).next();
							builder.vertex((double)(ae + (float)aj + 1.0F - 9.765625E-4F), (double)(ab + 4.0F), (double)(af + 8.0F)).texture((ae + (float)aj + 0.5F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(1.0F, 0.0F, 0.0F).next();
							builder.vertex((double)(ae + (float)aj + 1.0F - 9.765625E-4F), (double)(ab + 4.0F), (double)(af + 0.0F)).texture((ae + (float)aj + 0.5F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(1.0F, 0.0F, 0.0F).next();
							builder.vertex((double)(ae + (float)aj + 1.0F - 9.765625E-4F), (double)(ab + 0.0F), (double)(af + 0.0F)).texture((ae + (float)aj + 0.5F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(1.0F, 0.0F, 0.0F).next();
						}
					}

					if (ad > -1) {
						for(aj = 0; aj < 8; ++aj) {
							builder.vertex((double)(ae + 0.0F), (double)(ab + 4.0F), (double)(af + (float)aj + 0.0F)).texture((ae + 0.0F) * 0.00390625F + k, (af + (float)aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, -1.0F).next();
							builder.vertex((double)(ae + 8.0F), (double)(ab + 4.0F), (double)(af + (float)aj + 0.0F)).texture((ae + 8.0F) * 0.00390625F + k, (af + (float)aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, -1.0F).next();
							builder.vertex((double)(ae + 8.0F), (double)(ab + 0.0F), (double)(af + (float)aj + 0.0F)).texture((ae + 8.0F) * 0.00390625F + k, (af + (float)aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, -1.0F).next();
							builder.vertex((double)(ae + 0.0F), (double)(ab + 0.0F), (double)(af + (float)aj + 0.0F)).texture((ae + 0.0F) * 0.00390625F + k, (af + (float)aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, -1.0F).next();
						}
					}

					if (ad <= 1) {
						for(aj = 0; aj < 8; ++aj) {
							builder.vertex((double)(ae + 0.0F), (double)(ab + 4.0F), (double)(af + (float)aj + 1.0F - 9.765625E-4F)).texture((ae + 0.0F) * 0.00390625F + k, (af + (float)aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, 1.0F).next();
							builder.vertex((double)(ae + 8.0F), (double)(ab + 4.0F), (double)(af + (float)aj + 1.0F - 9.765625E-4F)).texture((ae + 8.0F) * 0.00390625F + k, (af + (float)aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, 1.0F).next();
							builder.vertex((double)(ae + 8.0F), (double)(ab + 0.0F), (double)(af + (float)aj + 1.0F - 9.765625E-4F)).texture((ae + 8.0F) * 0.00390625F + k, (af + (float)aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, 1.0F).next();
							builder.vertex((double)(ae + 0.0F), (double)(ab + 0.0F), (double)(af + (float)aj + 1.0F - 9.765625E-4F)).texture((ae + 0.0F) * 0.00390625F + k, (af + (float)aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, 1.0F).next();
						}
					}
				}
			}
		} else {

			for(int am = -32; am < 32; am += 32) {
				for(int an = -32; an < 32; an += 32) {
					builder.vertex((double)(am + 0), (double)ab, (double)(an + 32)).texture((float)(am + 0) * 0.00390625F + k, (float)(an + 32) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
					builder.vertex((double)(am + 32), (double)ab, (double)(an + 32)).texture((float)(am + 32) * 0.00390625F + k, (float)(an + 32) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
					builder.vertex((double)(am + 32), (double)ab, (double)(an + 0)).texture((float)(am + 32) * 0.00390625F + k, (float)(an + 0) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
					builder.vertex((double)(am + 0), (double)ab, (double)(an + 0)).texture((float)(am + 0) * 0.00390625F + k, (float)(an + 0) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
				}
			}
		}

	}

	private void updateChunks(long limitTime) {
		this.needsTerrainUpdate |= this.chunkBuilder.upload();
		long l = Util.getMeasuringTimeNano();
		int i = 0;
		if (!this.chunksToRebuild.isEmpty()) {
			Iterator iterator = this.chunksToRebuild.iterator();

			while(iterator.hasNext()) {
				ChunkBuilder.BuiltChunk builtChunk = (ChunkBuilder.BuiltChunk)iterator.next();
				if (builtChunk.needsImportantRebuild()) {
					this.chunkBuilder.rebuild(builtChunk);
				} else {
					builtChunk.scheduleRebuild(this.chunkBuilder);
				}

				builtChunk.cancelRebuild();
				iterator.remove();
				++i;
				long m = Util.getMeasuringTimeNano();
				long n = m - l;
				long o = n / (long)i;
				long p = limitTime - m;
				if (p < o) {
					break;
				}
			}
		}

	}





	private static void drawShapeOutline(MatrixStack matrices, VertexConsumer vertexConsumer, VoxelShape voxelShape, double d, double e, double f, float g, float h, float i, float j) {
		MatrixStack.Entry entry = matrices.peek();
		voxelShape.forEachEdge((k, l, m, n, o, p) -> {
			float q = (float)(n - k);
			float r = (float)(o - l);
			float s = (float)(p - m);
			float t = MathHelper.sqrt(q * q + r * r + s * s);
			q /= t;
			r /= t;
			s /= t;
			vertexConsumer.vertex(entry.getModel(), (float)(k + d), (float)(l + e), (float)(m + f)).color(g, h, i, j).normal(entry.getNormal(), q, r, s).next();
			vertexConsumer.vertex(entry.getModel(), (float)(n + d), (float)(o + e), (float)(p + f)).color(g, h, i, j).normal(entry.getNormal(), q, r, s).next();
		});
	}










	public void scheduleBlockRender(int x, int y, int z) {
		this.scheduleChunkRender(x, y, z, false);
	}

	private void scheduleChunkRender(int x, int y, int z, boolean important) {
		this.chunks.scheduleRebuild(x, y, z, important);
	}












	public void scheduleTerrainUpdate() {
		this.needsTerrainUpdate = true;
		this.cloudsDirty = true;
	}

	public void updateNoCullingBlockEntities(Collection<BlockEntity> removed, Collection<BlockEntity> added) {
		synchronized(this.noCullingBlockEntities) {
			this.noCullingBlockEntities.removeAll(removed);
			this.noCullingBlockEntities.addAll(added);
		}
	}

	public static int getLightmapCoordinates(BlockRenderView world, BlockPos pos) {
		return getLightmapCoordinates(world, world.getBlockState(pos), pos);
	}

	public static int getLightmapCoordinates(BlockRenderView world, BlockState state, BlockPos pos) {
		if (state.hasEmissiveLighting(world, pos)) {
			return 15728880;
		} else {
			int i = world.getLightLevel(LightType.SKY, pos);
			int j = world.getLightLevel(LightType.BLOCK, pos);
			int k = state.getLuminance();
			if (j < k) {
				j = k;
			}

			return i << 20 | j << 4;
		}
	}



	@Environment(EnvType.CLIENT)
	public static class ShaderException extends RuntimeException {
		public ShaderException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	@Environment(EnvType.CLIENT)
	private static class ChunkInfoList {
		private final PreviewRenderer.ChunkInfo[] current;
		private final PreviewRenderer.ChunkInfo[] pending;

		ChunkInfoList(int i) {
			this.current = new PreviewRenderer.ChunkInfo[i];
			this.pending = new PreviewRenderer.ChunkInfo[i];
		}

		void update() {
			System.arraycopy(this.pending, 0, this.current, 0, this.current.length);
		}

		public void setInfo(ChunkBuilder.BuiltChunk chunk, PreviewRenderer.ChunkInfo info) {
			this.current[chunk.index] = info;
		}

		public PreviewRenderer.ChunkInfo getInfo(ChunkBuilder.BuiltChunk chunk) {
			return this.current[chunk.index];
		}
	}

	@Environment(EnvType.CLIENT)
	public static class ChunkInfo {
		final ChunkBuilder.BuiltChunk chunk;
		private byte direction;
		byte cullingState;
		final int propagationLevel;

		ChunkInfo(ChunkBuilder.BuiltChunk chunk, @Nullable Direction direction, int propagationLevel) {
			this.chunk = chunk;
			if (direction != null) {
				this.addDirection(direction);
			}

			this.propagationLevel = propagationLevel;
		}

		public void updateCullingState(byte parentCullingState, Direction from) {
			this.cullingState = (byte)(this.cullingState | parentCullingState | 1 << from.ordinal());
		}

		public boolean canCull(Direction from) {
			return (this.cullingState & 1 << from.ordinal()) > 0;
		}

		public void addDirection(Direction direction) {
			this.direction = (byte)(this.direction | this.direction | 1 << direction.ordinal());
		}

		public boolean hasDirection(int ordinal) {
			return (this.direction & 1 << ordinal) > 0;
		}

		public boolean hasAnyDirection() {
			return this.direction != 0;
		}
	}
}
