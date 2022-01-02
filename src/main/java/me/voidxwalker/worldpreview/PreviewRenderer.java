package me.voidxwalker.worldpreview;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;

import me.voidxwalker.worldpreview.mixin.access.BuiltChunkStorageMixin;
import me.voidxwalker.worldpreview.mixin.access.RenderPhaseMixin;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.CloudRenderMode;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.GraphicsMode;
import net.minecraft.client.options.Option;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.resource.SynchronousResourceReloadListener;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class PreviewRenderer extends net.minecraft.client.render.WorldRenderer implements SynchronousResourceReloadListener, AutoCloseable {
	private static final Identifier MOON_PHASES = new Identifier("textures/environment/moon_phases.png");
	private static final Identifier SUN = new Identifier("textures/environment/sun.png");
	private static final Identifier CLOUDS = new Identifier("textures/environment/clouds.png");
	public static final Direction[] DIRECTIONS = Direction.values();
	private final MinecraftClient client;
	private final TextureManager textureManager;
	private final EntityRenderDispatcher entityRenderDispatcher;
	public final BufferBuilderStorage bufferBuilders;
	private ClientWorld world;
	private Set<ChunkBuilder.BuiltChunk> chunksToRebuild = Sets.newLinkedHashSet();
	private final ObjectList<PreviewRenderer.ChunkInfo> visibleChunks = new ObjectArrayList(69696);
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
	private int ticks;
	private final Int2ObjectMap<BlockBreakingInfo> blockBreakingInfos;
	private final Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions;

	@Nullable
	private Framebuffer entityOutlinesFramebuffer;
	@Nullable
	private ShaderEffect entityOutlineShader;
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
	private int regularEntityCount;
	private int blockEntityCount;
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
		super(client,bufferBuilders);
		this.skyVertexFormat = VertexFormats.POSITION;
		this.cloudsDirty = true;
		this.chunkUpdateSmoother = new FpsSmoother(100);
		this.blockBreakingInfos = new Int2ObjectOpenHashMap();
		this.blockBreakingProgressions = new Long2ObjectOpenHashMap();
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
		this.entityRenderDispatcher = client.getEntityRenderManager();
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
		this.renderLightSky();
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
			throw new PreviewRenderer.ShaderException("Failed to " + string + " shader: " + identifier, var8);
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

	public void drawEntityOutlinesFramebuffer() {
		if (this.canDrawEntityOutlines()) {
			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE);
			this.entityOutlinesFramebuffer.draw(this.client.getWindow().getFramebufferWidth(), this.client.getWindow().getFramebufferHeight(), false);
			RenderSystem.disableBlend();
		}
	}

	protected boolean canDrawEntityOutlines() {
		return this.entityOutlinesFramebuffer != null && this.entityOutlineShader != null && Main.player != null;
	}

	private void renderLightSky() {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		if (this.lightSkyBuffer != null) {
			this.lightSkyBuffer.close();
		}
		this.lightSkyBuffer = new VertexBuffer(this.skyVertexFormat);
		this.renderSkyHalf(bufferBuilder, false);
		bufferBuilder.end();
		this.lightSkyBuffer.upload(bufferBuilder);
	}

	private void renderSkyHalf(BufferBuilder buffer, boolean bottom) {
		buffer.begin(7, VertexFormats.POSITION);
		for(int k = -384; k <= 384; k += 64) {
			for(int l = -384; l <= 384; l += 64) {
				float f = (float)k;
				float g = (float)(k + 64);
				if (bottom) {
					g = (float)k;
					f = (float)(k + 64);
				}
				buffer.vertex(f, (float) 16.0, l).next();
				buffer.vertex(g, (float) 16.0, l).next();
				buffer.vertex(g, (float) 16.0, l + 64).next();
				buffer.vertex(f, (float) 16.0, l + 64).next();
			}
		}

	}

	public void setWorld(@Nullable ClientWorld clientWorld) {
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
			if (MinecraftClient.isFabulousGraphicsOrBetter()) {
				this.loadTransparencyShader();
			} else {
				this.resetTransparencyShader();
			}
			this.world.reloadColor();
			if (this.chunkBuilder == null) {
				this.chunkBuilder = new ChunkBuilder(this.world, this, Util.getServerWorkerExecutor(), this.client.is64Bit(), this.bufferBuilders.getBlockBufferBuilders());
			} else {
				this.chunkBuilder.setWorld(this.world);
			}
			this.needsTerrainUpdate = true;
			this.cloudsDirty = true;
			RenderLayers.setFancyGraphicsOrBetter(MinecraftClient.isFancyGraphicsOrBetter());
			this.renderDistance = this.client.options.viewDistance;
			if (this.chunks != null) {
				this.chunks.clear();
			}
			this.clearChunkRenderers();
			synchronized(this.noCullingBlockEntities) {
				this.noCullingBlockEntities.clear();
			}
			this.chunks = new BuiltChunkStorage(this.chunkBuilder, this.world, this.client.options.viewDistance, this);
			if (this.world != null) {
				Entity entity = this.client.getCameraEntity();
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

	public void onResized(int i, int j) {
		this.scheduleTerrainUpdate();
		if (this.entityOutlineShader != null) {
			this.entityOutlineShader.setupDimensions(i, j);
		}
		if (this.transparencyShader != null) {
			this.transparencyShader.setupDimensions(i, j);
		}

	}

	public String getChunksDebugString() {
		int i = this.chunks.chunks.length;
		int j = this.getCompletedChunkCount();
		return String.format("C: %d/%d %sD: %d, %s", j, i, this.client.chunkCullingEnabled ? "(s) " : "", this.renderDistance, this.chunkBuilder == null ? "null" : this.chunkBuilder.getDebugString());
	}

	protected int getCompletedChunkCount() {
		int i = 0;
		ObjectListIterator var2 = this.visibleChunks.iterator();

		while(var2.hasNext()) {
			PreviewRenderer.ChunkInfo chunkInfo = (PreviewRenderer.ChunkInfo)var2.next();
			if (!chunkInfo.chunk.getData().isEmpty()) {
				++i;
			}
		}
		return i;
	}

	public String getEntitiesDebugString() {
		return "E: " + this.regularEntityCount + "/" + this.world.getRegularEntityCount() + ", B: " + this.blockEntityCount;
	}

	private void setupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, int frame, boolean spectator) {
		Vec3d vec3d = camera.getPos();
		if (this.client.options.viewDistance != this.renderDistance) {
			this.reload();
		}
		this.world.getProfiler().push("camera");
		double d = Main.player.getX() - this.lastCameraChunkUpdateX;
		double e = Main.player.getY() - this.lastCameraChunkUpdateY;
		double f = Main.player.getZ() - this.lastCameraChunkUpdateZ;
		if (this.cameraChunkX != Main.player.chunkX || this.cameraChunkY != Main.player.chunkY || this.cameraChunkZ != Main.player.chunkZ || d * d + e * e + f * f > 16.0D) {
			this.lastCameraChunkUpdateX = Main.player.getX();
			this.lastCameraChunkUpdateY = Main.player.getY();
			this.lastCameraChunkUpdateZ = Main.player.getZ();
			this.cameraChunkX = Main.player.chunkX;
			this.cameraChunkY = Main.player.chunkY;
			this.cameraChunkZ = Main.player.chunkZ;
			this.chunks.updateCameraPosition(Main.player.getX(), Main.player.getZ());
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
		ObjectListIterator var31 = this.visibleChunks.iterator();
		while(true) {
			while(true) {
				do {
					if (!var31.hasNext()) {
						this.chunksToRebuild.addAll(set);
						this.client.getProfiler().pop();
						return;
					}
					chunkInfo = (PreviewRenderer.ChunkInfo)var31.next();
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
					builtChunk3.cancelRebuild();
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
		BlockEntityRenderDispatcher.INSTANCE.configure(this.world, this.client.getTextureManager(), this.client.textRenderer, camera, this.client.crosshairTarget);
		this.entityRenderDispatcher.configure(this.world, camera, this.client.targetedEntity);
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
		float g =this.client.options.viewDistance;

		boolean bl2 = this.world.getSkyProperties().useThickFog(MathHelper.floor(d), MathHelper.floor(e));
		if (this.client.options.viewDistance >= 4) {
			profiler.swap("sky");
			this.renderSky(matrices, tickDelta);
		}

		profiler.swap("fog");
		profiler.swap("terrain_setup");
		this.setupTerrain(camera, frustum2, bl, this.frame++, Main.player.isSpectator());
		profiler.swap("updatechunks");
		int j = this.client.options.maxFps;
		long n;
		if ((double)j == Option.FRAMERATE_LIMIT.getMax()) {
			n = 0L;
		} else {
			n = (1000000000 / j);
		}

		long o = Util.getMeasuringTimeNano() - limitTime;
		long p = this.chunkUpdateSmoother.getTargetUsedTime(o);
		long q = p * 3L / 2L;
		long r = MathHelper.clamp(q, n, 33333333L);
		this.updateChunks(limitTime + r);
		profiler.swap("terrain");
		this.renderLayer(RenderLayer.getSolid(), matrices, d, e, f);
		this.renderLayer(RenderLayer.getCutoutMipped(), matrices, d, e, f);
		this.renderLayer(RenderLayer.getCutout(), matrices, d, e, f);
		if (this.world.getSkyProperties().isDarkened()) {
			DiffuseLighting.enableForLevel(matrices.peek().getModel());
		} else {
			DiffuseLighting.method_27869(matrices.peek().getModel());
		}

		profiler.swap("entities");
		profiler.push("prepare");
		this.regularEntityCount = 0;
		this.blockEntityCount = 0;
		profiler.swap("entities");
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
							this.checkEmpty(matrices);
							immediate.draw(RenderLayer.getEntitySolid(SpriteAtlasTexture.BLOCK_ATLAS_TEX));
							immediate.draw(RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEX));
							immediate.draw(RenderLayer.getEntityCutoutNoCull(SpriteAtlasTexture.BLOCK_ATLAS_TEX));
							immediate.draw(RenderLayer.getEntitySmoothCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEX));
							profiler.swap("blockentities");
							ObjectListIterator var53 = this.visibleChunks.iterator();
							while(true) {
								List list;
								do {
									if (!var53.hasNext()) {
										synchronized(this.noCullingBlockEntities) {
											Iterator var57 = this.noCullingBlockEntities.iterator();
											while(true) {
												if (!var57.hasNext()) {
													break;
												}
												BlockEntity blockEntity2 = (BlockEntity)var57.next();
												BlockPos blockPos2 = blockEntity2.getPos();
												matrices.push();
												matrices.translate((double)blockPos2.getX() - d, (double)blockPos2.getY() - e, (double)blockPos2.getZ() - f);
												BlockEntityRenderDispatcher.INSTANCE.render(blockEntity2, tickDelta, matrices, immediate);
												matrices.pop();
											}
										}
										this.checkEmpty(matrices);
										immediate.draw(RenderLayer.getSolid());
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
											Entry<SortedSet<BlockBreakingInfo>> entry2 = (Entry)var54.next();
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
													VertexConsumer vertexConsumer2 = new TransformingVertexConsumer(this.bufferBuilders.getEffectVertexConsumers().getBuffer((RenderLayer)ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(z)), entry3.getModel(), entry3.getNormal());
													this.client.getBlockRenderManager().renderDamage(this.world.getBlockState(blockPos3), blockPos3, this.world, matrices, vertexConsumer2);
													matrices.pop();
												}
											}
										}
										this.checkEmpty(matrices);
										profiler.pop();
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
										RenderSystem.pushMatrix();
										RenderSystem.multMatrix(matrices.peek().getModel());
										RenderSystem.popMatrix();
										immediate.draw(TexturedRenderLayers.getEntityTranslucentCull());
										immediate.draw(TexturedRenderLayers.getBannerPatterns());
										immediate.draw(TexturedRenderLayers.getShieldPatterns());
										immediate.draw(RenderLayer.getArmorGlint());
										immediate.draw(RenderLayer.getArmorEntityGlint());
										immediate.draw(RenderLayer.getGlint());
										immediate.draw(RenderLayer.getEntityGlint());
										immediate.draw(RenderLayer.getWaterMask());
										this.bufferBuilders.getEffectVertexConsumers().draw();
										immediate.draw(RenderLayer.getLines());
										immediate.draw();
										if (this.transparencyShader != null) {
											this.translucentFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
											this.translucentFramebuffer.copyDepthFrom(this.client.getFramebuffer());
											profiler.swap("translucent");
											this.renderLayer(RenderLayer.getTranslucent(), matrices, d, e, f);
											profiler.swap("string");
											this.renderLayer(RenderLayer.getTripwire(), matrices, d, e, f);
											this.particlesFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
											this.particlesFramebuffer.copyDepthFrom(this.client.getFramebuffer());
											RenderPhaseMixin.getPARTICLES_TARGET().startDrawing();
											profiler.swap("particles");
											this.client.particleManager.renderParticles(matrices, immediate, lightmapTextureManager, camera, tickDelta);
											RenderPhaseMixin.getPARTICLES_TARGET().endDrawing();
										} else {
											profiler.swap("translucent");
											this.renderLayer(RenderLayer.getTranslucent(), matrices, d, e, f);
											profiler.swap("string");
											this.renderLayer(RenderLayer.getTripwire(), matrices, d, e, f);
											profiler.swap("particles");
											this.client.particleManager.renderParticles(matrices, immediate, lightmapTextureManager, camera, tickDelta);
										}
										RenderSystem.pushMatrix();
										RenderSystem.multMatrix(matrices.peek().getModel());
										if (this.client.options.getCloudRenderMode() != CloudRenderMode.OFF) {
											if (this.transparencyShader != null) {
												this.cloudsFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
												RenderPhaseMixin.getCLOUDS_TARGET().startDrawing();
												profiler.swap("clouds");
												this.renderClouds(matrices, tickDelta, d, e, f);
												RenderPhaseMixin.getCLOUDS_TARGET().endDrawing();
											} else {
												profiler.swap("clouds");
												this.renderClouds(matrices, tickDelta, d, e, f);
											}
										}
										this.renderChunkDebugInfo(camera);
										RenderSystem.shadeModel(7424);
										RenderSystem.depthMask(true);
										RenderSystem.disableBlend();
										RenderSystem.popMatrix();
										BackgroundRenderer.method_23792();
										return;
									}
									PreviewRenderer.ChunkInfo chunkInfo = (PreviewRenderer.ChunkInfo)var53.next();
									list = chunkInfo.chunk.getData().getBlockEntities();
								} while(list.isEmpty());
								Iterator var61 = list.iterator();
								while(var61.hasNext()) {
									BlockEntity blockEntity = (BlockEntity)var61.next();
									BlockPos blockPos = blockEntity.getPos();
									VertexConsumerProvider vertexConsumerProvider3 = immediate;
									matrices.push();
									matrices.translate((double)blockPos.getX() - d, (double)blockPos.getY() - e, (double)blockPos.getZ() - f);
									SortedSet<BlockBreakingInfo> sortedSet = (SortedSet)this.blockBreakingProgressions.get(blockPos.asLong());
									if (sortedSet != null && !sortedSet.isEmpty()) {
										w = ((BlockBreakingInfo)sortedSet.last()).getStage();
										if (w >= 0) {
											MatrixStack.Entry entry = matrices.peek();
											VertexConsumer vertexConsumer = new TransformingVertexConsumer(this.bufferBuilders.getEffectVertexConsumers().getBuffer((RenderLayer)ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(w)), entry.getModel(), entry.getNormal());
											vertexConsumerProvider3 = (renderLayer) -> {
												VertexConsumer vertexConsumer2 = immediate.getBuffer(renderLayer);
												return renderLayer.hasCrumbling() ? VertexConsumers.dual(vertexConsumer, vertexConsumer2) : vertexConsumer2;
											};
										}
									}
									BlockEntityRenderDispatcher.INSTANCE.render(blockEntity, tickDelta, matrices, (VertexConsumerProvider)vertexConsumerProvider3);
									matrices.pop();
								}
							}
						}

						entity = (Entity)var39.next();
					} while(!this.entityRenderDispatcher.shouldRender(entity, frustum2, d, e, f) && !entity.hasPassengerDeep(Main.player));
				} while(entity == camera.getFocusedEntity() && !camera.isThirdPerson() && (!(camera.getFocusedEntity() instanceof LivingEntity) || !((LivingEntity)camera.getFocusedEntity()).isSleeping()));
			} while(entity instanceof ClientPlayerEntity && camera.getFocusedEntity() != entity);
			++this.regularEntityCount;
			if (entity.age == 0) {
				entity.lastRenderX = entity.getX();
				entity.lastRenderY = entity.getY();
				entity.lastRenderZ = entity.getZ();
			}
			Object vertexConsumerProvider2;
			if (this.canDrawEntityOutlines() && this.client.method_27022(entity)) {
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

	private void checkEmpty(MatrixStack matrices) {
		if (!matrices.isEmpty()) {
			throw new IllegalStateException("Pose stack not empty");
		}
	}

	private void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
		double d = MathHelper.lerp((double)tickDelta, entity.lastRenderX, entity.getX());
		double e = MathHelper.lerp((double)tickDelta, entity.lastRenderY, entity.getY());
		double f = MathHelper.lerp((double)tickDelta, entity.lastRenderZ, entity.getZ());
		float g = MathHelper.lerp(tickDelta, entity.prevYaw, entity.yaw);
		this.entityRenderDispatcher.render(entity, d - cameraX, e - cameraY, f - cameraZ, g, tickDelta, matrices, vertexConsumers, this.entityRenderDispatcher.getLight(entity, tickDelta));
	}

	private void renderLayer(RenderLayer renderLayer, MatrixStack matrixStack, double d, double e, double f) {
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
				ObjectListIterator var16 = this.visibleChunks.iterator();
				while(var16.hasNext()) {
					PreviewRenderer.ChunkInfo chunkInfo = (PreviewRenderer.ChunkInfo)var16.next();
					if (j < 15 && chunkInfo.chunk.scheduleSort(renderLayer, this.chunkBuilder)) {
						++j;
					}
				}
			}

			this.client.getProfiler().pop();
		}

		this.client.getProfiler().push("filterempty");
		this.client.getProfiler().swap(() -> "render_" + renderLayer);
		boolean bl = renderLayer != RenderLayer.getTranslucent();
		ObjectListIterator objectListIterator = this.visibleChunks.listIterator(bl ? 0 : this.visibleChunks.size());

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
				matrixStack.push();
				BlockPos blockPos = builtChunk.getOrigin();
				matrixStack.translate((double)blockPos.getX() - d, (double)blockPos.getY() - e, (double)blockPos.getZ() - f);
				vertexBuffer.bind();
				this.vertexFormat.startDrawing(0L);
				vertexBuffer.draw(matrixStack.peek().getModel(), 7);
				matrixStack.pop();
			}
		}

		VertexBuffer.unbind();
		RenderSystem.clearCurrentColor();
		this.vertexFormat.endDrawing();
		this.client.getProfiler().pop();
		renderLayer.endDrawing();
	}

	private void renderChunkDebugInfo(Camera camera) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		if (this.client.debugChunkInfo || this.client.debugChunkOcclusion) {
			double d = camera.getPos().getX();
			double e = camera.getPos().getY();
			double f = camera.getPos().getZ();
			RenderSystem.depthMask(true);
			RenderSystem.disableCull();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableTexture();

			for(ObjectListIterator var10 = this.visibleChunks.iterator(); var10.hasNext(); RenderSystem.popMatrix()) {
				PreviewRenderer.ChunkInfo chunkInfo = (PreviewRenderer.ChunkInfo)var10.next();
				ChunkBuilder.BuiltChunk builtChunk = chunkInfo.chunk;
				RenderSystem.pushMatrix();
				BlockPos blockPos = builtChunk.getOrigin();
				RenderSystem.translated((double)blockPos.getX() - d, (double)blockPos.getY() - e, (double)blockPos.getZ() - f);
				int m;
				int k;
				int l;
				Direction direction2;
				if (this.client.debugChunkInfo) {
					bufferBuilder.begin(1, VertexFormats.POSITION_COLOR);
					RenderSystem.lineWidth(10.0F);
					m = chunkInfo.propagationLevel == 0 ? 0 : MathHelper.hsvToRgb((float)chunkInfo.propagationLevel / 50.0F, 0.9F, 0.9F);
					int j = m >> 16 & 255;
					k = m >> 8 & 255;
					l = m & 255;
					direction2 = chunkInfo.direction;
					if (direction2 != null) {
						bufferBuilder.vertex(8.0D, 8.0D, 8.0D).color(j, k, l, 255).next();
						bufferBuilder.vertex((double)(8 - 16 * direction2.getOffsetX()), (double)(8 - 16 * direction2.getOffsetY()), (double)(8 - 16 * direction2.getOffsetZ())).color(j, k, l, 255).next();
					}

					tessellator.draw();
					RenderSystem.lineWidth(1.0F);
				}

				if (this.client.debugChunkOcclusion && !builtChunk.getData().isEmpty()) {
					bufferBuilder.begin(1, VertexFormats.POSITION_COLOR);
					RenderSystem.lineWidth(10.0F);
					m = 0;
					Direction[] var24 = DIRECTIONS;
					k = var24.length;

					for(l = 0; l < k; ++l) {
						direction2 = var24[l];
						Direction[] var19 = DIRECTIONS;
						int var20 = var19.length;

						for(int var21 = 0; var21 < var20; ++var21) {
							Direction direction3 = var19[var21];
							boolean bl = builtChunk.getData().isVisibleThrough(direction2, direction3);
							if (!bl) {
								++m;
								bufferBuilder.vertex((double)(8 + 8 * direction2.getOffsetX()), (double)(8 + 8 * direction2.getOffsetY()), (double)(8 + 8 * direction2.getOffsetZ())).color(1, 0, 0, 1).next();
								bufferBuilder.vertex((double)(8 + 8 * direction3.getOffsetX()), (double)(8 + 8 * direction3.getOffsetY()), (double)(8 + 8 * direction3.getOffsetZ())).color(1, 0, 0, 1).next();
							}
						}
					}

					tessellator.draw();
					RenderSystem.lineWidth(1.0F);
					if (m > 0) {
						bufferBuilder.begin(7, VertexFormats.POSITION_COLOR);
						float g = 0.5F;
						float h = 0.2F;
						bufferBuilder.vertex(0.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(15.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(15.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(0.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(0.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(15.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(15.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(0.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(0.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(0.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(0.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(0.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(15.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(15.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(15.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(15.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(0.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(15.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(15.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(0.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(0.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(15.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(15.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						bufferBuilder.vertex(0.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
						tessellator.draw();
					}
				}
			}

			RenderSystem.depthMask(true);
			RenderSystem.disableBlend();
			RenderSystem.enableCull();
			RenderSystem.enableTexture();
		}

		if (this.capturedFrustum != null) {
			RenderSystem.disableCull();
			RenderSystem.disableTexture();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.lineWidth(10.0F);
			RenderSystem.pushMatrix();
			RenderSystem.translatef((float)(this.capturedFrustumPosition.x - camera.getPos().x), (float)(this.capturedFrustumPosition.y - camera.getPos().y), (float)(this.capturedFrustumPosition.z - camera.getPos().z));
			RenderSystem.depthMask(true);
			bufferBuilder.begin(7, VertexFormats.POSITION_COLOR);
			this.method_22985(bufferBuilder, 0, 1, 2, 3, 0, 1, 1);
			this.method_22985(bufferBuilder, 4, 5, 6, 7, 1, 0, 0);
			this.method_22985(bufferBuilder, 0, 1, 5, 4, 1, 1, 0);
			this.method_22985(bufferBuilder, 2, 3, 7, 6, 0, 0, 1);
			this.method_22985(bufferBuilder, 0, 4, 7, 3, 0, 1, 0);
			this.method_22985(bufferBuilder, 1, 5, 6, 2, 1, 0, 1);
			tessellator.draw();
			RenderSystem.depthMask(false);
			bufferBuilder.begin(1, VertexFormats.POSITION);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
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
			RenderSystem.popMatrix();
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







	public void renderSky(MatrixStack matrices, float tickDelta) {
		 if (this.world.getSkyProperties().getSkyType() == SkyProperties.SkyType.NORMAL) {
			RenderSystem.disableTexture();
			Vec3d vec3d = this.world.method_23777(Main.camera.getBlockPos(), tickDelta);
			float f = (float)vec3d.x;
			float g = (float)vec3d.y;
			float h = (float)vec3d.z;
			//BackgroundRenderer.setFogBlack();
			BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
			RenderSystem.depthMask(false);
			//RenderSystem.enableFog();
			RenderSystem.color3f(f, g, h);
			this.lightSkyBuffer.bind();
			this.skyVertexFormat.startDrawing(0L);
			this.lightSkyBuffer.draw(matrices.peek().getModel(), 7);
			VertexBuffer.unbind();
			this.skyVertexFormat.endDrawing();
			//RenderSystem.disableFog();
			RenderSystem.disableAlphaTest();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			float[] fs = this.world.getSkyProperties().getSkyColor(this.world.getSkyAngle(tickDelta), tickDelta);
			float r;
			float s;
			float o;
			float p;
			float q;
			if (fs != null) {
				RenderSystem.disableTexture();
				RenderSystem.shadeModel(7425);
				matrices.push();
				matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(90.0F));
				r = MathHelper.sin(this.world.getSkyAngleRadians(tickDelta)) < 0.0F ? 180.0F : 0.0F;
				matrices.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(r));
				matrices.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(90.0F));
				float j = fs[0];
				s = fs[1];
				float l = fs[2];
				Matrix4f matrix4f = matrices.peek().getModel();
				bufferBuilder.begin(6, VertexFormats.POSITION_COLOR);
				bufferBuilder.vertex(matrix4f, 0.0F, 100.0F, 0.0F).color(j, s, l, fs[3]).next();
				int m = 1;

				for(int n = 0; n <= 16; ++n) {
					o = (float)n * 6.2831855F / 16.0F;
					p = MathHelper.sin(o);
					q = MathHelper.cos(o);
					bufferBuilder.vertex(matrix4f, p * 120.0F, q * 120.0F, -q * 40.0F * fs[3]).color(fs[0], fs[1], fs[2], 0.0F).next();
				}

				bufferBuilder.end();
				BufferRenderer.draw(bufferBuilder);
				matrices.pop();
				RenderSystem.shadeModel(7424);
			}

			RenderSystem.enableTexture();
			RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
			matrices.push();
			r = 1.0F - this.world.getRainGradient(tickDelta);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, r);
			matrices.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-90.0F));
			matrices.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(this.world.getSkyAngle(tickDelta) * 360.0F));
			Matrix4f matrix4f2 = matrices.peek().getModel();
			s = 30.0F;
			this.textureManager.bindTexture(SUN);
			bufferBuilder.begin(7, VertexFormats.POSITION_TEXTURE);
			bufferBuilder.vertex(matrix4f2, -s, 100.0F, -s).texture(0.0F, 0.0F).next();
			bufferBuilder.vertex(matrix4f2, s, 100.0F, -s).texture(1.0F, 0.0F).next();
			bufferBuilder.vertex(matrix4f2, s, 100.0F, s).texture(1.0F, 1.0F).next();
			bufferBuilder.vertex(matrix4f2, -s, 100.0F, s).texture(0.0F, 1.0F).next();
			bufferBuilder.end();
			BufferRenderer.draw(bufferBuilder);
			s = 20.0F;
			this.textureManager.bindTexture(MOON_PHASES);
			int t = this.world.getMoonPhase();
			int u = t % 4;
			int v = t / 4 % 2;
			float w = (float)(u + 0) / 4.0F;
			o = (float)(v + 0) / 2.0F;
			p = (float)(u + 1) / 4.0F;
			q = (float)(v + 1) / 2.0F;
			bufferBuilder.begin(7, VertexFormats.POSITION_TEXTURE);
			bufferBuilder.vertex(matrix4f2, -s, -100.0F, s).texture(p, q).next();
			bufferBuilder.vertex(matrix4f2, s, -100.0F, s).texture(w, q).next();
			bufferBuilder.vertex(matrix4f2, s, -100.0F, -s).texture(w, o).next();
			bufferBuilder.vertex(matrix4f2, -s, -100.0F, -s).texture(p, o).next();
			bufferBuilder.end();
			BufferRenderer.draw(bufferBuilder);
			RenderSystem.disableTexture();
			float aa = this.world.method_23787(tickDelta) * r;


			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.disableBlend();
			RenderSystem.enableAlphaTest();
			//RenderSystem.enableFog();
			matrices.pop();
			RenderSystem.disableTexture();
			RenderSystem.color3f(0.0F, 0.0F, 0.0F);
			double d = Main.player.getCameraPosVec(tickDelta).y - this.world.getLevelProperties().getSkyDarknessHeight();
			if (d < 0.0D) {
				matrices.push();
				matrices.translate(0.0D, 12.0D, 0.0D);
				this.darkSkyBuffer.bind();
				this.skyVertexFormat.startDrawing(0L);
				this.darkSkyBuffer.draw(matrices.peek().getModel(), 7);
				VertexBuffer.unbind();
				this.skyVertexFormat.endDrawing();
				matrices.pop();
			}

			if (this.world.getSkyProperties().isAlternateSkyColor()) {
				RenderSystem.color3f(f * 0.2F + 0.04F, g * 0.2F + 0.04F, h * 0.6F + 0.1F);
			} else {
				RenderSystem.color3f(f, g, h);
			}

			RenderSystem.enableTexture();
			RenderSystem.depthMask(true);
			//RenderSystem.disableFog();



		}
	}

	public void renderClouds(MatrixStack matrices, float tickDelta, double cameraX, double cameraY, double cameraZ) {
		float f = this.world.getSkyProperties().getCloudsHeight();
		if (!Float.isNaN(f)) {
			RenderSystem.disableCull();
			RenderSystem.enableBlend();
			RenderSystem.enableAlphaTest();
			RenderSystem.enableDepthTest();
			RenderSystem.defaultAlphaFunc();
			RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
		//	RenderSystem.enableFog();
			RenderSystem.depthMask(true);
			float g = 12.0F;
			float h = 4.0F;
			double d = 2.0E-4D;
			double e = (double)(((float)this.ticks + tickDelta) * 0.03F);
			double i = (cameraX + e) / 12.0D;
			double j = (double)(f - (float)cameraY + 0.33F);
			double k = cameraZ / 12.0D + 0.33000001311302185D;
			i -= (double)(MathHelper.floor(i / 2048.0D) * 2048);
			k -= (double)(MathHelper.floor(k / 2048.0D) * 2048);
			float l = (float)(i - (double)MathHelper.floor(i));
			float m = (float)(j / 4.0D - (double)MathHelper.floor(j / 4.0D)) * 4.0F;
			float n = (float)(k - (double)MathHelper.floor(k));
			Vec3d vec3d = this.world.getCloudsColor(tickDelta);
			int o = (int)Math.floor(i);
			int p = (int)Math.floor(j / 4.0D);
			int q = (int)Math.floor(k);
			if (o != this.lastCloudsBlockX || p != this.lastCloudsBlockY || q != this.lastCloudsBlockZ || this.client.options.getCloudRenderMode() != this.lastCloudsRenderMode || this.lastCloudsColor.squaredDistanceTo(vec3d) > 2.0E-4D) {
				this.lastCloudsBlockX = o;
				this.lastCloudsBlockY = p;
				this.lastCloudsBlockZ = q;
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

				this.cloudsBuffer = new VertexBuffer(VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);
				this.renderClouds(bufferBuilder, i, j, k, vec3d);
				bufferBuilder.end();
				this.cloudsBuffer.upload(bufferBuilder);
			}

			this.textureManager.bindTexture(CLOUDS);
			matrices.push();
			matrices.scale(12.0F, 1.0F, 12.0F);
			matrices.translate((double)(-l), (double)m, (double)(-n));
			if (this.cloudsBuffer != null) {
				this.cloudsBuffer.bind();
				VertexFormats.POSITION_TEXTURE_COLOR_NORMAL.startDrawing(0L);
				int r = this.lastCloudsRenderMode == CloudRenderMode.FANCY ? 0 : 1;

				for(int s = r; s < 2; ++s) {
					if (s == 0) {
						RenderSystem.colorMask(false, false, false, false);
					} else {
						RenderSystem.colorMask(true, true, true, true);
					}

					this.cloudsBuffer.draw(matrices.peek().getModel(), 7);
				}

				VertexBuffer.unbind();
				VertexFormats.POSITION_TEXTURE_COLOR_NORMAL.endDrawing();
			}

			matrices.pop();
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.disableAlphaTest();
			RenderSystem.enableCull();
			RenderSystem.disableBlend();
			//RenderSystem.disableFog();
		}
	}

	private void renderClouds(BufferBuilder builder, double x, double y, double z, Vec3d color) {
		float f = 4.0F;
		float g = 0.00390625F;
		int i = 1;
		int j = 1;
		float h = 9.765625E-4F;
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
		builder.begin(7, VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);
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

	private void drawBlockOutline(MatrixStack matrixStack, VertexConsumer vertexConsumer, Entity entity, double d, double e, double f, BlockPos blockPos, BlockState blockState) {
		drawShapeOutline(matrixStack, vertexConsumer, blockState.getOutlineShape(this.world, blockPos, ShapeContext.of(entity)), (double)blockPos.getX() - d, (double)blockPos.getY() - e, (double)blockPos.getZ() - f, 0.0F, 0.0F, 0.0F, 0.4F);
	}

	private static void drawShapeOutline(MatrixStack matrixStack, VertexConsumer vertexConsumer, VoxelShape voxelShape, double d, double e, double f, float g, float h, float i, float j) {
		Matrix4f matrix4f = matrixStack.peek().getModel();
		voxelShape.forEachEdge((k, l, m, n, o, p) -> {
			vertexConsumer.vertex(matrix4f, (float)(k + d), (float)(l + e), (float)(m + f)).color(g, h, i, j).next();
			vertexConsumer.vertex(matrix4f, (float)(n + d), (float)(o + e), (float)(p + f)).color(g, h, i, j).next();
		});
	}

	@Environment(EnvType.CLIENT)
	public class ChunkInfo {
		public final ChunkBuilder.BuiltChunk chunk;
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
