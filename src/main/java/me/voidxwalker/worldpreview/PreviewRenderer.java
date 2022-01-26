package me.voidxwalker.worldpreview;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import me.voidxwalker.worldpreview.mixin.access.BuiltChunkStorageMixin;
import me.voidxwalker.worldpreview.mixin.access.RenderPhaseMixin;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.Option;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
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
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.LightType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Environment(EnvType.CLIENT)
public class PreviewRenderer {
	private static final Identifier MOON_PHASES = new Identifier("textures/environment/moon_phases.png");
	private static final Identifier SUN = new Identifier("textures/environment/sun.png");
	private static final Identifier CLOUDS = new Identifier("textures/environment/clouds.png");
	public static final Direction[] DIRECTIONS = Direction.values();
	private final MinecraftClient client;
	private final TextureManager textureManager;
	private final EntityRenderDispatcher entityRenderDispatcher;
	private final BufferBuilderStorage bufferBuilders;
	public ClientWorld world;
	private Set<ChunkBuilder.BuiltChunk> chunksToRebuild = Sets.newLinkedHashSet();
	private final ObjectArrayList<ChunkInfo> visibleChunks = new ObjectArrayList<>(69696);
	private final Set<BlockEntity> noCullingBlockEntities = Sets.newHashSet();
	private BuiltChunkStorage chunks;
	private final VertexFormat skyVertexFormat;
	@Nullable
	private VertexBuffer starsBuffer;
	@Nullable
	private VertexBuffer lightSkyBuffer;
	@Nullable
	private VertexBuffer darkSkyBuffer;
	private boolean cloudsDirty;
	@Nullable
	private VertexBuffer cloudsBuffer;
	private final FpsSmoother chunkUpdateSmoother;
	public int ticks;
	private final Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions;

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
	private double lastCameraChunkUpdateX;
	private double lastCameraChunkUpdateY;
	private double lastCameraChunkUpdateZ;
	private int cameraChunkX;
	private int cameraChunkY;
	private int cameraChunkZ;
	private double lastCameraX;
	private double lastCameraY;
	private double lastCameraZ;
	private double lastCameraPitch;
	private double lastCameraYaw;
	private int lastCloudsBlockX;
	private int lastCloudsBlockY;
	private int lastCloudsBlockZ;
	private Vec3d lastCloudsColor;
	private CloudRenderMode lastCloudsRenderMode;
	private ChunkBuilder chunkBuilder;
	private final VertexFormat vertexFormat;
	private int renderDistance;
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
	private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
	private final float[] field_20794;
	private final float[] field_20795;

	public PreviewRenderer(MinecraftClient client, BufferBuilderStorage bufferBuilders) {
		this.skyVertexFormat = VertexFormats.POSITION;
		this.cloudsDirty = true;
		this.chunkUpdateSmoother = new FpsSmoother(100);
		this.blockBreakingProgressions = new Long2ObjectOpenHashMap<>();
		this.lastCameraChunkUpdateX = Double.MIN_VALUE;
		this.lastCameraChunkUpdateY = Double.MIN_VALUE;
		this.lastCameraChunkUpdateZ = Double.MIN_VALUE;
		this.cameraChunkX = Integer.MIN_VALUE;
		this.cameraChunkY = Integer.MIN_VALUE;
		this.cameraChunkZ = Integer.MIN_VALUE;
		this.lastCameraX = Double.MIN_VALUE;
		this.lastCameraY = Double.MIN_VALUE;
		this.lastCameraZ = Double.MIN_VALUE;
		this.lastCameraPitch = Double.MIN_VALUE;
		this.lastCameraYaw = Double.MIN_VALUE;
		this.lastCloudsBlockX = Integer.MIN_VALUE;
		this.lastCloudsBlockY = Integer.MIN_VALUE;
		this.lastCloudsBlockZ = Integer.MIN_VALUE;
		this.lastCloudsColor = Vec3d.ZERO;
		this.vertexFormat = VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL;
		this.renderDistance = -1;
		this.capturedFrustumOrientation = new Vector4f[8];
		this.capturedFrustumPosition = new Vector3d(0.0D, 0.0D, 0.0D);
		this.needsTerrainUpdate = true;
		this.field_20794 = new float[1024];
		this.field_20795 = new float[1024];
		this.client = client;
		this.entityRenderDispatcher = client.getEntityRenderDispatcher();
		this.blockEntityRenderDispatcher = client.getBlockEntityRenderDispatcher();
		this.bufferBuilders = bufferBuilders;
		this.textureManager = client.getTextureManager();

		for(int i = 0; i < 32; ++i) {
			for(int j = 0; j < 32; ++j) {
				float f = (float)(j - 16);
				float g = (float)(i - 16);
				float h = MathHelper.sqrt(f * f + g * g);
				this.field_20794[i << 5 | j] = -g / h;
				this.field_20795[i << 5 | j] = f / h;
			}
		}

		this.renderStars();
		this.renderLightSky();
		this.renderDarkSky();
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


	private void loadTransparencyShader() {
		this.resetTransparencyShader();
		Identifier identifier = new Identifier("shaders/post/transparency.json");

		try {
			ShaderEffect shaderEffect = new ShaderEffect(this.client.getTextureManager(), this.client.getResourceManager(), this.client.getFramebuffer(), identifier);
			shaderEffect.setupDimensions(this.client.getWindow().getFramebufferWidth(), this.client.getWindow().getFramebufferHeight());
			Framebuffer framebuffer = shaderEffect.getSecondaryTarget("translucent");
			Framebuffer framebuffer2 = shaderEffect.getSecondaryTarget("itemEntity");
			Framebuffer framebuffer3 = shaderEffect.getSecondaryTarget("particles");
			Framebuffer framebuffer4 = shaderEffect.getSecondaryTarget("weather");
			Framebuffer framebuffer5 = shaderEffect.getSecondaryTarget("clouds");
			this.transparencyShader = shaderEffect;
			this.translucentFramebuffer = framebuffer;
			this.entityFramebuffer = framebuffer2;
			this.particlesFramebuffer = framebuffer3;
			this.weatherFramebuffer = framebuffer4;
			this.cloudsFramebuffer = framebuffer5;
		} catch (Exception var8) {
			String string = var8 instanceof JsonSyntaxException ? "parse" : "load";
			GameOptions gameOptions = MinecraftClient.getInstance().options;
			gameOptions.graphicsMode = GraphicsMode.FANCY;
			gameOptions.write();
			throw new WorldRenderer.ShaderException("Failed to " + string + " shader: " + identifier, var8);
		}
	}

	private void resetTransparencyShader() {
		if (this.transparencyShader != null) {
			this.transparencyShader.close();
			this.translucentFramebuffer.delete();
			this.entityFramebuffer.delete();
			this.particlesFramebuffer.delete();
			this.weatherFramebuffer.delete();
			this.cloudsFramebuffer.delete();
			this.transparencyShader = null;
			this.translucentFramebuffer = null;
			this.entityFramebuffer = null;
			this.particlesFramebuffer = null;
			this.weatherFramebuffer = null;
			this.cloudsFramebuffer = null;
		}

	}


	private void renderDarkSky() {

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		if (this.darkSkyBuffer != null) {
			this.darkSkyBuffer.close();
		}

		this.darkSkyBuffer = new VertexBuffer();
		this.renderSkyHalf(bufferBuilder, -16.0F, true);
		bufferBuilder.end();
		this.darkSkyBuffer.upload(bufferBuilder);
	}

	private void renderLightSky() {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		if (this.lightSkyBuffer != null) {
			this.lightSkyBuffer.close();
		}

		this.lightSkyBuffer = new VertexBuffer();
		this.renderSkyHalf(bufferBuilder, 16.0F, false);
		bufferBuilder.end();
		this.lightSkyBuffer.upload(bufferBuilder);
	}

	private void renderSkyHalf(BufferBuilder buffer, float y, boolean bottom) {
		buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

		for(int k = -384; k <= 384; k += 64) {
			for(int l = -384; l <= 384; l += 64) {
				float f = (float)k;
				float g = (float)(k + 64);
				if (bottom) {
					g = (float)k;
					f = (float)(k + 64);
				}

				buffer.vertex(f, y, l).next();
				buffer.vertex(g, y, l).next();
				buffer.vertex(g, y, l + 64).next();
				buffer.vertex(f, y, l + 64).next();

			}
		}

	}

	private void renderStars() {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		if (this.starsBuffer != null) {
			this.starsBuffer.close();
		}

		this.starsBuffer = new VertexBuffer();
		this.renderStars(bufferBuilder);
		bufferBuilder.end();
		this.starsBuffer.upload(bufferBuilder);
	}

	private void renderStars(BufferBuilder buffer) {
		Random random = new Random(10842L);

		buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

		for(int i = 0; i < 1500; ++i) {
			double d = random.nextFloat() * 2.0F - 1.0F;
			double e = random.nextFloat() * 2.0F - 1.0F;
			double f = random.nextFloat() * 2.0F - 1.0F;
			double g = 0.15F + random.nextFloat() * 0.1F;
			double h = d * d + e * e + f * f;
			if (h < 1.0D && h > 0.01D) {
				h = 1.0D / Math.sqrt(h);
				d *= h;
				e *= h;
				f *= h;
				double j = d * 100.0D;
				double k = e * 100.0D;
				double l = f * 100.0D;
				double m = Math.atan2(d, f);
				double n = Math.sin(m);
				double o = Math.cos(m);
				double p = Math.atan2(Math.sqrt(d * d + f * f), e);
				double q = Math.sin(p);
				double r = Math.cos(p);
				double s = random.nextDouble() * 3.141592653589793D * 2.0D;
				double t = Math.sin(s);
				double u = Math.cos(s);

				for(int v = 0; v < 4; ++v) {
					double x = (double)((v & 2) - 1) * g;
					double y = (double)((v + 1 & 2) - 1) * g;
					double aa = x * u - y * t;
					double ab = y * u + x * t;
					double ad = aa * q +  r;
					double ae =  - aa * r;
					double af = ae * n - ab * o;
					double ah = ab * n + ae * o;
					buffer.vertex(j + af, k + ad, l + ah).next();
				}
			}
		}

	}

	public void loadWorld(@Nullable ClientWorld clientWorld) {
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

	public void reload() {
		if (this.world != null) {

			this.resetTransparencyShader();

			this.world.reloadColor();
			if (this.chunkBuilder == null) {
				this.chunkBuilder = new ChunkBuilder(this.world, null, Util.getMainWorkerExecutor(), this.client.is64Bit(), this.bufferBuilders.getBlockBufferBuilders());
			} else {
				this.chunkBuilder.setWorld(this.world);
			}

			this.needsTerrainUpdate = true;
			this.cloudsDirty = true;
			RenderLayers.setFancyGraphicsOrBetter(false);
			this.renderDistance = this.client.options.viewDistance;
			if (this.chunks != null) {
				this.chunks.clear();
			}

			this.clearChunkRenderers();
			synchronized(this.noCullingBlockEntities) {
				this.noCullingBlockEntities.clear();
			}

			this.chunks = new BuiltChunkStorage(this.chunkBuilder, this.world, this.client.options.viewDistance, null);
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
		if (this.client.options.viewDistance != this.renderDistance) {
			this.reload();
		}

		this.world.getProfiler().push("camera");
		double g2 = WorldPreview.player.getX() - this.lastCameraChunkUpdateX;
		double h2 = WorldPreview.player.getY() - this.lastCameraChunkUpdateY;
		double i2 = WorldPreview.player.getZ() - this.lastCameraChunkUpdateZ;
		double d = WorldPreview.player.getX();
		double e = WorldPreview.player.getY();
		double f = WorldPreview.player.getZ();
		int j2 = ChunkSectionPos.getSectionCoord(d);
		int k2 = ChunkSectionPos.getSectionCoord(e);
		int l2 = ChunkSectionPos.getSectionCoord(f);
		if (this.cameraChunkX != j2 || this.cameraChunkY != k2 || this.cameraChunkZ != l2 || g2 * g2 + h2 * h2 + i2 * i2 > 16.0D) {
			this.lastCameraChunkUpdateX = WorldPreview.player.getX();
			this.lastCameraChunkUpdateY = WorldPreview.player.getY();
			this.lastCameraChunkUpdateZ = WorldPreview.player.getZ();
			this.cameraChunkX =j2;
			this.cameraChunkY = k2;
			this.cameraChunkZ = l2;
			this.chunks.updateCameraPosition(WorldPreview.player.getX(), WorldPreview.player.getZ());
		}

		this.chunkBuilder.setCameraPosition(vec3d);
		this.world.getProfiler().swap("cull");
		this.client.getProfiler().swap("culling");
		BlockPos blockPos = camera.getBlockPos();
		ChunkBuilder.BuiltChunk builtChunk = ((BuiltChunkStorageMixin)this.chunks).callGetRenderedChunk(blockPos);
		BlockPos blockPos2 = new BlockPos(MathHelper.floor(vec3d.x / 16.0D) * 16, MathHelper.floor(vec3d.y / 16.0D) * 16, MathHelper.floor(vec3d.z / 16.0D) * 16);
		float g = camera.getPitch();
		float h = camera.getYaw();
		this.needsTerrainUpdate = this.needsTerrainUpdate || !this.chunksToRebuild.isEmpty() || vec3d.x != this.lastCameraX || vec3d.y != this.lastCameraY || vec3d.z != this.lastCameraZ || (double)g != this.lastCameraPitch || (double)h != this.lastCameraYaw;
		this.lastCameraX = vec3d.x;
		this.lastCameraY = vec3d.y;
		this.lastCameraZ = vec3d.z;
		this.lastCameraPitch = g;
		this.lastCameraYaw = h;
		this.client.getProfiler().swap("update");
		PreviewRenderer.ChunkInfo chunkInfo;
		ChunkBuilder.BuiltChunk builtChunk3;
		if (!hasForcedFrustum && this.needsTerrainUpdate) {
			this.needsTerrainUpdate = false;
			this.visibleChunks.clear();

			Queue<PreviewRenderer.ChunkInfo> queue = Queues.newArrayDeque();
			Entity.setRenderDistanceMultiplier(MathHelper.clamp((double)this.client.options.viewDistance / 8.0D, 1.0D, 2.5D) * (double)this.client.options.entityDistanceScaling);
			boolean bl = this.client.chunkCullingEnabled;
			int m;
			int n;
			if (builtChunk != null) {
				if (spectator && this.world.getBlockState(blockPos).isOpaqueFullCube(this.world, blockPos)) {
					bl = false;
				}

				builtChunk.setRebuildFrame(frame);
				queue.add(new PreviewRenderer.ChunkInfo(builtChunk, null, 0));
			} else {
				int j = blockPos.getY() > 0 ? 248 : 8;
				int k = MathHelper.floor(vec3d.x / 16.0D) * 16;
				int l = MathHelper.floor(vec3d.z / 16.0D) * 16;
				List<PreviewRenderer.ChunkInfo> list = Lists.newArrayList();
				m = -this.renderDistance;

				while(true) {
					if (m > this.renderDistance) {
						list.sort(Comparator.comparingDouble((chunkInfox) -> {
							return blockPos.getSquaredDistance(chunkInfox.chunk.getOrigin().add(8, 8, 8));
						}));
						queue.addAll(list);
						break;
					}

					for(n = -this.renderDistance; n <= this.renderDistance; ++n) {
						ChunkBuilder.BuiltChunk builtChunk2 = ((BuiltChunkStorageMixin)this.chunks).callGetRenderedChunk(new BlockPos(k + (m << 4) + 8, j, l + (n << 4) + 8));
						if (builtChunk2 != null && frustum.isVisible(builtChunk2.boundingBox)) {
							builtChunk2.setRebuildFrame(frame);
							list.add(new PreviewRenderer.ChunkInfo(builtChunk2, null, 0));
						}
					}

					++m;
				}
			}

			this.client.getProfiler().push("iteration");

			while(!queue.isEmpty()) {
				chunkInfo = queue.poll();
				builtChunk3 = chunkInfo.chunk;
				Direction direction = chunkInfo.direction;
				this.visibleChunks.add(chunkInfo);
				Direction[] var36 = DIRECTIONS;
				m = var36.length;

				for(n = 0; n < m; ++n) {
					Direction direction2 = var36[n];
					ChunkBuilder.BuiltChunk builtChunk4 = this.getAdjacentChunk(blockPos2, builtChunk3, direction2);
					if ((!bl || !chunkInfo.canCull(direction2.getOpposite())) && (!bl || direction == null || builtChunk3.getData().isVisibleThrough(direction.getOpposite(), direction2)) && builtChunk4 != null && builtChunk4.shouldBuild() && builtChunk4.setRebuildFrame(frame) && frustum.isVisible(builtChunk4.boundingBox)) {
						PreviewRenderer.ChunkInfo chunkInfo2 = new PreviewRenderer.ChunkInfo(builtChunk4, direction2, chunkInfo.propagationLevel + 1);
						chunkInfo2.updateCullingState(chunkInfo.cullingState, direction2);
						queue.add(chunkInfo2);
					}
				}
			}

			this.client.getProfiler().pop();
		}

		this.client.getProfiler().swap("rebuildNear");
		Set<ChunkBuilder.BuiltChunk> set = this.chunksToRebuild;
		this.chunksToRebuild = Sets.newLinkedHashSet();
		ObjectListIterator<ChunkInfo> var31 = this.visibleChunks.iterator();

		while(true) {
			while(true) {
				do {
					if (!var31.hasNext()) {
						this.chunksToRebuild.addAll(set);
						this.client.getProfiler().pop();
						return;
					}

					chunkInfo = var31.next();
					builtChunk3 = chunkInfo.chunk;
				} while(!builtChunk3.needsRebuild() && !set.contains(builtChunk3));

				this.needsTerrainUpdate = true;
				BlockPos blockPos3 = builtChunk3.getOrigin().add(8, 8, 8);
				boolean bl2 = blockPos3.getSquaredDistance(blockPos) < 768.0D;
				if (!builtChunk3.needsImportantRebuild() && !bl2) {
					this.chunksToRebuild.add(builtChunk3);
				} else {
					this.client.getProfiler().push("build near");
					this.chunkBuilder.rebuild(builtChunk3);

					this.client.getProfiler().pop();
				}
			}
		}
	}

	@Nullable
	private ChunkBuilder.BuiltChunk getAdjacentChunk(BlockPos pos, ChunkBuilder.BuiltChunk chunk, Direction direction) {
		BlockPos blockPos = chunk.getNeighborPosition(direction);
		if (MathHelper.abs(pos.getX() - blockPos.getX()) > this.renderDistance * 16) {
			return null;
		} else if (blockPos.getY() >= 0 && blockPos.getY() < 256) {
			return MathHelper.abs(pos.getZ() - blockPos.getZ()) > this.renderDistance * 16 ? null : ((BuiltChunkStorageMixin)this.chunks).callGetRenderedChunk(blockPos);
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

	public void render(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f) {
		this.blockEntityRenderDispatcher.configure(this.world, camera, this.client.crosshairTarget);
		this.entityRenderDispatcher.configure(this.world, camera, WorldPreview.player);
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
			frustum2 = new Frustum(matrix4f2, matrix4f);
			frustum2.setPosition(d, e, f);
		}

		this.client.getProfiler().swap("captureFrustum");
		if (this.shouldCaptureFrustum) {
			this.captureFrustum(matrix4f2, matrix4f, vec3d.x, vec3d.y, vec3d.z, bl ? new Frustum(matrix4f2, matrix4f) : frustum2);
			this.shouldCaptureFrustum = false;
		}

		profiler.swap("clear");
		BackgroundRenderer.render(camera, tickDelta, this.world, this.client.options.viewDistance, gameRenderer.getSkyDarkness(tickDelta));
		RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);
		float g = client.options.viewDistance*16;
		if (this.client.options.viewDistance >= 4) {
			profiler.swap("sky");
			this.renderSky(matrices, matrix4f,tickDelta);
		}

		profiler.swap("fog");
		BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_TERRAIN, Math.max(g - 16.0F, 32.0F), false);
		profiler.swap("terrain_setup");
		this.setupTerrain(camera, frustum2, bl, this.frame++, WorldPreview.player.isSpectator());
		profiler.swap("updatechunks");
		int j = this.client.options.maxFps;
		long n;
		if ((double)j == Option.FRAMERATE_LIMIT.getMax()) {
			n = 0L;
		} else {
			n = 1000000000 / j;
		}

		long o = Util.getMeasuringTimeNano() - limitTime;
		long p = this.chunkUpdateSmoother.getTargetUsedTime(o);
		long q = p * 3L / 2L;
		long r = MathHelper.clamp(q, n, 33333333L);
		this.updateChunks(limitTime + r);
		profiler.swap("terrain");
		this.renderLayer(RenderLayer.getSolid(), matrices, d, e, f,matrix4f);
		this.renderLayer(RenderLayer.getCutoutMipped(), matrices, d, e, f,matrix4f);
		this.renderLayer(RenderLayer.getCutout(), matrices, d, e, f,matrix4f);
		DiffuseLighting.disableForLevel(matrices.peek().getModel());

		profiler.swap("entities");
		profiler.push("prepare");
		int regularEntityCount = 0;
		int blockEntityCount = 0;
		profiler.swap("entities");
		if (this.entityFramebuffer != null) {
			this.entityFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
			this.entityFramebuffer.copyDepthFrom(this.client.getFramebuffer());
			this.client.getFramebuffer().beginWrite(false);
		}
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

								Iterator var62 = list.iterator();

								while(var62.hasNext()) {
									BlockEntity blockEntity = (BlockEntity)var62.next();
									BlockPos blockPos = blockEntity.getPos();
									VertexConsumerProvider vertexConsumerProvider3 = immediate;
									matrices.push();
									matrices.translate((double)blockPos.getX() - d, (double)blockPos.getY() - e, (double)blockPos.getZ() - f);
									SortedSet<BlockBreakingInfo> sortedSet = this.blockBreakingProgressions.get(blockPos.asLong());
									if (sortedSet != null && !sortedSet.isEmpty()) {
										w = sortedSet.last().getStage();
										if (w >= 0) {
											MatrixStack.Entry entry = matrices.peek();
											VertexConsumer vertexConsumer = new OverlayVertexConsumer(this.bufferBuilders.getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(w)), entry.getModel(), entry.getNormal());
											vertexConsumerProvider3 = (renderLayer) -> {
												VertexConsumer vertexConsumer2 = immediate.getBuffer(renderLayer);
												return renderLayer.hasCrumbling() ? VertexConsumers.union(vertexConsumer, vertexConsumer2) : vertexConsumer2;
											};
										}
									}

									this.blockEntityRenderDispatcher.render(blockEntity, tickDelta, matrices, vertexConsumerProvider3);
									matrices.pop();
								}
							}
						}

						entity = (Entity)var39.next();
					} while(!this.entityRenderDispatcher.shouldRender(entity, frustum2, d, e, f) && !entity.hasPassengerDeep(this.client.player));
				} while(entity == camera.getFocusedEntity() && !camera.isThirdPerson() && (!(camera.getFocusedEntity() instanceof LivingEntity) || !((LivingEntity)camera.getFocusedEntity()).isSleeping()));
			} while(entity instanceof ClientPlayerEntity && camera.getFocusedEntity() != entity);


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
		double d = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
		double e = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
		double f = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
		float g = MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw());
		this.entityRenderDispatcher.render(entity, d - cameraX, e - cameraY, f - cameraZ, g, tickDelta, matrices, vertexConsumers, this.entityRenderDispatcher.getLight(entity, tickDelta));
	}

	private void renderLayer(RenderLayer renderLayer, MatrixStack matrixStack, double d, double e, double f, Matrix4f matrix4f) {
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
			shader.modelViewMat.set(matrixStack.peek().getModel());
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

			PreviewRenderer.ChunkInfo chunkInfo2 = bl ? objectListIterator.next() : objectListIterator.previous();
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

	public void renderSky(MatrixStack matrices, Matrix4f matrix4f, float f) {

			RenderSystem.disableTexture();
			Vec3d vec3d = this.world.getSkyColor(WorldPreview.camera.getPos(), f);
			float g = (float)vec3d.x;
			float h = (float)vec3d.y;
			float i = (float)vec3d.z;
			BackgroundRenderer.setFogBlack();
			BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
			RenderSystem.depthMask(false);
			RenderSystem.setShaderColor(g, h, i, 0.0F);
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
			double l = ((float)this.ticks + f) * 0.03F;
			double m = (d + l) / 12.0D;
			double n = h - (float)e + 0.33F;
			double o = g / 12.0D + 0.33000001311302185D;
			m -= MathHelper.floor(m / 2048.0D) * 2048;
			o -= MathHelper.floor(o / 2048.0D) * 2048;
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
			matrices.translate(-p, q, -r);
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
						builder.vertex(ae + 0.0F, ab + 0.0F, af + 8.0F).texture((ae + 0.0F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(s, t, u, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
						builder.vertex(ae + 8.0F, ab + 0.0F, af + 8.0F).texture((ae + 8.0F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(s, t, u, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
						builder.vertex(ae + 8.0F, ab + 0.0F, af + 0.0F).texture((ae + 8.0F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(s, t, u, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
						builder.vertex(ae + 0.0F, ab + 0.0F, af + 0.0F).texture((ae + 0.0F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(s, t, u, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
					}

					if (ab <= 5.0F) {
						builder.vertex(ae + 0.0F, ab + 4.0F - 9.765625E-4F, af + 8.0F).texture((ae + 0.0F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, 1.0F, 0.0F).next();
						builder.vertex(ae + 8.0F, ab + 4.0F - 9.765625E-4F, af + 8.0F).texture((ae + 8.0F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, 1.0F, 0.0F).next();
						builder.vertex(ae + 8.0F, ab + 4.0F - 9.765625E-4F, af + 0.0F).texture((ae + 8.0F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, 1.0F, 0.0F).next();
						builder.vertex(ae + 0.0F, ab + 4.0F - 9.765625E-4F, af + 0.0F).texture((ae + 0.0F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, 1.0F, 0.0F).next();
					}

					int aj;
					if (ac > -1) {
						for(aj = 0; aj < 8; ++aj) {
							builder.vertex(ae + (float)aj + 0.0F, ab + 0.0F, af + 8.0F).texture((ae + (float)aj + 0.5F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(-1.0F, 0.0F, 0.0F).next();
							builder.vertex(ae + (float)aj + 0.0F, ab + 4.0F, af + 8.0F).texture((ae + (float)aj + 0.5F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(-1.0F, 0.0F, 0.0F).next();
							builder.vertex(ae + (float)aj + 0.0F, ab + 4.0F, af + 0.0F).texture((ae + (float)aj + 0.5F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(-1.0F, 0.0F, 0.0F).next();
							builder.vertex(ae + (float)aj + 0.0F, ab + 0.0F, af + 0.0F).texture((ae + (float)aj + 0.5F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(-1.0F, 0.0F, 0.0F).next();
						}
					}

					if (ac <= 1) {
						for(aj = 0; aj < 8; ++aj) {
							builder.vertex(ae + (float)aj + 1.0F - 9.765625E-4F, ab + 0.0F, af + 8.0F).texture((ae + (float)aj + 0.5F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(1.0F, 0.0F, 0.0F).next();
							builder.vertex(ae + (float)aj + 1.0F - 9.765625E-4F, ab + 4.0F, af + 8.0F).texture((ae + (float)aj + 0.5F) * 0.00390625F + k, (af + 8.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(1.0F, 0.0F, 0.0F).next();
							builder.vertex(ae + (float)aj + 1.0F - 9.765625E-4F, ab + 4.0F, af + 0.0F).texture((ae + (float)aj + 0.5F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(1.0F, 0.0F, 0.0F).next();
							builder.vertex(ae + (float)aj + 1.0F - 9.765625E-4F, ab + 0.0F, af + 0.0F).texture((ae + (float)aj + 0.5F) * 0.00390625F + k, (af + 0.0F) * 0.00390625F + l).color(p, q, r, 0.8F).normal(1.0F, 0.0F, 0.0F).next();
						}
					}

					if (ad > -1) {
						for(aj = 0; aj < 8; ++aj) {
							builder.vertex(ae + 0.0F, ab + 4.0F, af + (float)aj + 0.0F).texture((ae + 0.0F) * 0.00390625F + k, (af + (float)aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, -1.0F).next();
							builder.vertex(ae + 8.0F, ab + 4.0F, af + (float)aj + 0.0F).texture((ae + 8.0F) * 0.00390625F + k, (af + (float)aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, -1.0F).next();
							builder.vertex(ae + 8.0F, ab + 0.0F, af + (float)aj + 0.0F).texture((ae + 8.0F) * 0.00390625F + k, (af + (float)aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, -1.0F).next();
							builder.vertex(ae + 0.0F, ab + 0.0F, af + (float)aj + 0.0F).texture((ae + 0.0F) * 0.00390625F + k, (af + (float)aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, -1.0F).next();
						}
					}

					if (ad <= 1) {
						for(aj = 0; aj < 8; ++aj) {
							builder.vertex(ae + 0.0F, ab + 4.0F, af + (float)aj + 1.0F - 9.765625E-4F).texture((ae + 0.0F) * 0.00390625F + k, (af + (float)aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, 1.0F).next();
							builder.vertex(ae + 8.0F, ab + 4.0F, af + (float)aj + 1.0F - 9.765625E-4F).texture((ae + 8.0F) * 0.00390625F + k, (af + (float)aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, 1.0F).next();
							builder.vertex(ae + 8.0F, ab + 0.0F, af + (float)aj + 1.0F - 9.765625E-4F).texture((ae + 8.0F) * 0.00390625F + k, (af + (float)aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, 1.0F).next();
							builder.vertex(ae + 0.0F, ab + 0.0F, af + (float)aj + 1.0F - 9.765625E-4F).texture((ae + 0.0F) * 0.00390625F + k, (af + (float)aj + 0.5F) * 0.00390625F + l).color(v, w, aa, 0.8F).normal(0.0F, 0.0F, 1.0F).next();
						}
					}
				}
			}
		} else {


			for(int am = -32; am < 32; am += 32) {
				for(int an = -32; an < 32; an += 32) {
					builder.vertex(am + 0, ab, an + 32).texture((float)(am + 0) * 0.00390625F + k, (float)(an + 32) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
					builder.vertex(am + 32, ab, an + 32).texture((float)(am + 32) * 0.00390625F + k, (float)(an + 32) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
					builder.vertex(am + 32, ab, an + 0).texture((float)(am + 32) * 0.00390625F + k, (float)(an + 0) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
					builder.vertex(am + 0, ab, an + 0).texture((float)(am + 0) * 0.00390625F + k, (float)(an + 0) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
				}
			}
		}


	}




	private void updateChunks(long limitTime) {
		this.needsTerrainUpdate |= this.chunkBuilder.upload();
		long l = Util.getMeasuringTimeNano();
		int i = 0;
		if (!this.chunksToRebuild.isEmpty()) {
			Iterator<ChunkBuilder.BuiltChunk> iterator = this.chunksToRebuild.iterator();

			while(iterator.hasNext()) {
				ChunkBuilder.BuiltChunk builtChunk = iterator.next();
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




	private void drawBlockOutline(MatrixStack matrixStack, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos, BlockState blockState) {
		drawShapeOutline(matrixStack, vertexConsumer, blockState.getOutlineShape(this.world, blockPos, ShapeContext.of(entity)), (double)blockPos.getX() - d, (double)blockPos.getY() - e, (double)blockPos.getZ() - f);
	}
	public void updateNoCullingBlockEntities(Collection<BlockEntity> removed, Collection<BlockEntity> added) {
		synchronized(this.noCullingBlockEntities) {
			this.noCullingBlockEntities.removeAll(removed);
			this.noCullingBlockEntities.addAll(added);
		}
	}


	private static void drawShapeOutline(MatrixStack matrixStack, VertexConsumer vertexConsumer, VoxelShape voxelShape, double d, double e, double f) {
		Matrix4f matrix4f = matrixStack.peek().getModel();
		voxelShape.forEachEdge((k, l, m, n, o, p) -> {
			vertexConsumer.vertex(matrix4f, (float)(k + d), (float)(l + e), (float)(m + f)).color((float) 0.0, (float) 0.0, (float) 0.0, (float) 0.4).next();
			vertexConsumer.vertex(matrix4f, (float)(n + d), (float)(o + e), (float)(p + f)).color((float) 0.0, (float) 0.0, (float) 0.0, (float) 0.4).next();
		});
	}

	@Environment(EnvType.CLIENT)
	class ChunkInfo {
		private final ChunkBuilder.BuiltChunk chunk;
		private final Direction direction;
		private byte cullingState;
		private final int propagationLevel;

		private ChunkInfo(ChunkBuilder.BuiltChunk chunk, @Nullable Direction direction, int propagationLevel) {
			this.chunk = chunk;
			this.direction = direction;
			this.propagationLevel = propagationLevel;
		}

		public void updateCullingState(byte parentCullingState, Direction from) {
			this.cullingState = (byte)(this.cullingState | parentCullingState | 1 << from.ordinal());
		}

		public boolean canCull(Direction from) {
			return (this.cullingState & 1 << from.ordinal()) > 0;
		}
	}
}
