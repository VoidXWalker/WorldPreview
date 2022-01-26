package me.voidxwalker.worldpreview;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import me.voidxwalker.worldpreview.mixin.access.BuiltChunkStorageMixin;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlProgramManager;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.CloudRenderMode;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.chunk.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloadListener;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.CollisionView;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.*;

@Environment(EnvType.CLIENT)
public class PreviewRenderer implements AutoCloseable, SynchronousResourceReloadListener {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Identifier MOON_PHASES = new Identifier("textures/environment/moon_phases.png");
	private static final Identifier SUN = new Identifier("textures/environment/sun.png");
	private static final Identifier CLOUDS = new Identifier("textures/environment/clouds.png");
	private static final Identifier FORCEFIELD = new Identifier("textures/misc/forcefield.png");
	public static final Direction[] DIRECTIONS = Direction.values();
	private final MinecraftClient client;
	private final TextureManager textureManager;
	public BackgroundRenderer backgroundRenderer;
	private final EntityRenderDispatcher entityRenderDispatcher;
	public ClientWorld world;
	private Set<ChunkRenderer> chunksToRebuild = Sets.newLinkedHashSet();
	private List<PreviewRenderer.ChunkInfo> chunkInfos = Lists.newArrayListWithCapacity(69696);
	private final Set<BlockEntity> noCullingBlockEntities = Sets.newHashSet();
	private BuiltChunkStorage chunks;
	private int starsDisplayList = -1;
	private int field_4117 = -1;
	private int field_4067 = -1;
	private final VertexFormat field_4100;
	private VertexBuffer starsBuffer;
	private VertexBuffer field_4087;
	private VertexBuffer field_4102;
	private boolean cloudsDirty = true;
	private int cloudsDisplayList = -1;
	private VertexBuffer cloudsBuffer;
	public int ticks;

	private Framebuffer entityOutlinesFramebuffer;
	private ShaderEffect entityOutlineShader;
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
	private int field_4082 = Integer.MIN_VALUE;
	private int field_4097 = Integer.MIN_VALUE;
	private int field_4116 = Integer.MIN_VALUE;
	private Vec3d field_4072;
	private CloudRenderMode field_4080;
	private ChunkBuilder chunkBuilder;
	private ChunkRendererList chunkRendererList;
	private int renderDistance;
	private boolean field_4066;
	private boolean vertexBufferObjectsEnabled;
	private ChunkRendererFactory chunkRendererFactory;
	private double lastTranslucentSortX;
	private double lastTranslucentSortY;
	private double lastTranslucentSortZ;
	private boolean needsTerrainUpdate;

	public PreviewRenderer(MinecraftClient client) {
		this.field_4072 = Vec3d.ZERO;
		this.renderDistance = -1;
		this.needsTerrainUpdate = true;
		this.client = client;
		this.entityRenderDispatcher = client.getEntityRenderManager();
		this.textureManager = client.getTextureManager();
		this.vertexBufferObjectsEnabled = GLX.useVbo();
		if (this.vertexBufferObjectsEnabled) {
			this.chunkRendererList = new VboChunkRendererList();
			this.chunkRendererFactory = ChunkRenderer::new;
		} else {
			this.chunkRendererList = new DisplayListChunkRendererList();
			this.chunkRendererFactory = DisplayListChunkRenderer::new;
		}

		this.field_4100 = new VertexFormat();
		this.backgroundRenderer=new BackgroundRenderer();
		this.field_4100.add(new VertexFormatElement(0, VertexFormatElement.Format.FLOAT, VertexFormatElement.Type.POSITION, 3));
		this.renderStars();
		this.method_3277();
		this.method_3265();
	}

	public void close() {
		if (this.entityOutlineShader != null) {
			this.entityOutlineShader.close();
		}

	}

	public void apply(ResourceManager manager) {
		this.textureManager.bindTexture(FORCEFIELD);
		GlStateManager.texParameter(3553, 10242, 10497);
		GlStateManager.texParameter(3553, 10243, 10497);
		GlStateManager.bindTexture(0);
		this.loadEntityOutlineShader();
	}



	public void loadEntityOutlineShader() {
		if (GLX.usePostProcess) {
			if (GlProgramManager.getInstance() == null) {
				GlProgramManager.init();
			}

			if (this.entityOutlineShader != null) {
				this.entityOutlineShader.close();
			}

			Identifier identifier = new Identifier("shaders/post/entity_outline.json");

			try {
				this.entityOutlineShader = new ShaderEffect(this.client.getTextureManager(), this.client.getResourceManager(), this.client.getFramebuffer(), identifier);
				this.entityOutlineShader.setupDimensions(this.client.window.getFramebufferWidth(), this.client.window.getFramebufferHeight());
				this.entityOutlinesFramebuffer = this.entityOutlineShader.getSecondaryTarget("final");
			} catch (IOException var3) {
				LOGGER.warn("Failed to load shader: {}", identifier, var3);
				this.entityOutlineShader = null;
				this.entityOutlinesFramebuffer = null;
			} catch (JsonSyntaxException var4) {
				LOGGER.warn("Failed to load shader: {}", identifier, var4);
				this.entityOutlineShader = null;
				this.entityOutlinesFramebuffer = null;
			}
		} else {
			this.entityOutlineShader = null;
			this.entityOutlinesFramebuffer = null;
		}

	}

	public void drawEntityOutlinesFramebuffer() {
		if (this.canDrawEntityOutlines()) {
			GlStateManager.enableBlend();
			GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
			this.entityOutlinesFramebuffer.drawInternal(this.client.window.getFramebufferWidth(), this.client.window.getFramebufferHeight(), false);
			GlStateManager.disableBlend();
		}

	}

	protected boolean canDrawEntityOutlines() {
		return this.entityOutlinesFramebuffer != null && this.entityOutlineShader != null && WorldPreview.player != null;
	}

	private void method_3265() {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		if (this.field_4102 != null) {
			this.field_4102.delete();
		}

		if (this.field_4067 >= 0) {
			GlAllocationUtils.deleteSingletonList(this.field_4067);
			this.field_4067 = -1;
		}

		if (this.vertexBufferObjectsEnabled) {
			this.field_4102 = new VertexBuffer(this.field_4100);
			this.renderSkyHalf(bufferBuilder, -16.0F, true);
			bufferBuilder.end();
			bufferBuilder.clear();
			this.field_4102.set(bufferBuilder.getByteBuffer());
		} else {
			this.field_4067 = GlAllocationUtils.genLists(1);
			GlStateManager.newList(this.field_4067, 4864);
			this.renderSkyHalf(bufferBuilder, -16.0F, true);
			tessellator.draw();
			GlStateManager.endList();
		}

	}

	private void method_3277() {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		if (this.field_4087 != null) {
			this.field_4087.delete();
		}

		if (this.field_4117 >= 0) {
			GlAllocationUtils.deleteSingletonList(this.field_4117);
			this.field_4117 = -1;
		}

		if (this.vertexBufferObjectsEnabled) {
			this.field_4087 = new VertexBuffer(this.field_4100);
			this.renderSkyHalf(bufferBuilder, 16.0F, false);
			bufferBuilder.end();
			bufferBuilder.clear();
			this.field_4087.set(bufferBuilder.getByteBuffer());
		} else {
			this.field_4117 = GlAllocationUtils.genLists(1);
			GlStateManager.newList(this.field_4117, 4864);
			this.renderSkyHalf(bufferBuilder, 16.0F, false);
			tessellator.draw();
			GlStateManager.endList();
		}

	}

	private void renderSkyHalf(BufferBuilder buffer, float y, boolean bottom) {

		buffer.begin(7, VertexFormats.POSITION);

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
			this.starsBuffer.delete();
		}

		if (this.starsDisplayList >= 0) {
			GlAllocationUtils.deleteSingletonList(this.starsDisplayList);
			this.starsDisplayList = -1;
		}

		if (this.vertexBufferObjectsEnabled) {
			this.starsBuffer = new VertexBuffer(this.field_4100);
			this.renderStars(bufferBuilder);
			bufferBuilder.end();
			bufferBuilder.clear();
			this.starsBuffer.set(bufferBuilder.getByteBuffer());
		} else {
			this.starsDisplayList = GlAllocationUtils.genLists(1);
			GlStateManager.pushMatrix();
			GlStateManager.newList(this.starsDisplayList, 4864);
			this.renderStars(bufferBuilder);
			tessellator.draw();
			GlStateManager.endList();
			GlStateManager.popMatrix();
		}

	}

	private void renderStars(BufferBuilder buffer) {
		Random random = new Random(10842L);
		buffer.begin(7, VertexFormats.POSITION);

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
					double ad = aa * q + 0.0D * r;
					double ae = 0.0D * q - aa * r;
					double af = ae * n - ab * o;
					double ah = ab * n + ae * o;
					buffer.vertex(j + af, k + ad, l + ah).next();
				}
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
			this.chunkInfos.clear();
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
			if (this.chunkBuilder == null) {
				this.chunkBuilder = new ChunkBuilder(this.client.is64Bit());
			}

			this.needsTerrainUpdate = true;
			this.cloudsDirty = true;
			LeavesBlock.setRenderingMode(this.client.options.fancyGraphics);
			this.renderDistance = this.client.options.viewDistance;
			boolean bl = this.vertexBufferObjectsEnabled;
			this.vertexBufferObjectsEnabled = GLX.useVbo();
			if (bl && !this.vertexBufferObjectsEnabled) {
				this.chunkRendererList = new DisplayListChunkRendererList();
				this.chunkRendererFactory = DisplayListChunkRenderer::new;
			} else if (!bl && this.vertexBufferObjectsEnabled) {
				this.chunkRendererList = new VboChunkRendererList();
				this.chunkRendererFactory = ChunkRenderer::new;
			}

			if (bl != this.vertexBufferObjectsEnabled) {
				this.renderStars();
				this.method_3277();
				this.method_3265();
			}

			if (this.chunks != null) {
				this.chunks.clear();
			}

			this.clearChunkRenderers();
			synchronized(this.noCullingBlockEntities) {
				this.noCullingBlockEntities.clear();
			}

			this.chunks = new BuiltChunkStorage(this.world, this.client.options.viewDistance, null, this.chunkRendererFactory);
			if (this.world != null) {
				Entity entity = WorldPreview.player;
				if (entity != null) {
					this.chunks.updateCameraPosition(entity.x, entity.z);
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
		if (GLX.usePostProcess) {
			if (this.entityOutlineShader != null) {
				this.entityOutlineShader.setupDimensions(i, j);
			}

		}
	}

	public void method_21595(Camera camera) {
		BlockEntityRenderDispatcher.INSTANCE.configure(this.world, this.client.getTextureManager(), this.client.textRenderer, camera,null);
		this.entityRenderDispatcher.configure(this.world, this.client.textRenderer, camera,null, this.client.options);
	}




	protected int getCompletedChunkCount() {
		int i = 0;
		Iterator var2 = this.chunkInfos.iterator();

		while(var2.hasNext()) {
			PreviewRenderer.ChunkInfo chunkInfo = (PreviewRenderer.ChunkInfo)var2.next();
			ChunkRenderData chunkRenderData = chunkInfo.renderer.data;
			if (chunkRenderData != ChunkRenderData.EMPTY && !chunkRenderData.isEmpty()) {
				++i;
			}
		}

		return i;
	}


	public void setUpTerrain(Camera camera, VisibleRegion visibleRegion, int i, boolean bl) {
		if (this.client.options.viewDistance != this.renderDistance) {
			this.reload();
		}

		this.world.getProfiler().push("camera");
		double d = WorldPreview.player.x - this.lastCameraChunkUpdateX;
		double e = WorldPreview.player.y - this.lastCameraChunkUpdateY;
		double f = WorldPreview.player.z - this.lastCameraChunkUpdateZ;
		if (this.cameraChunkX != WorldPreview.player.chunkX || this.cameraChunkY != WorldPreview.player.chunkY || this.cameraChunkZ != WorldPreview.player.chunkZ || d * d + e * e + f * f > 16.0D) {
			this.lastCameraChunkUpdateX = WorldPreview.player.x;
			this.lastCameraChunkUpdateY = WorldPreview.player.y;
			this.lastCameraChunkUpdateZ = WorldPreview.player.z;
			this.cameraChunkX = WorldPreview.player.chunkX;
			this.cameraChunkY = WorldPreview.player.chunkY;
			this.cameraChunkZ = WorldPreview.player.chunkZ;
			this.chunks.updateCameraPosition(WorldPreview.player.x, WorldPreview.player.z);
		}

		this.world.getProfiler().swap("renderlistcamera");
		this.chunkRendererList.setCameraPosition(camera.getPos().x, camera.getPos().y, camera.getPos().z);
		this.chunkBuilder.setCameraPosition(camera.getPos());
		this.world.getProfiler().swap("cull");


		this.client.getProfiler().swap("culling");
		BlockPos blockPos = camera.getBlockPos();
		ChunkRenderer chunkRenderer = ((BuiltChunkStorageMixin)this.chunks).callGetChunkRenderer(blockPos);
		BlockPos blockPos2 = new BlockPos(MathHelper.floor(camera.getPos().x / 16.0D) * 16, MathHelper.floor(camera.getPos().y / 16.0D) * 16, MathHelper.floor(camera.getPos().z / 16.0D) * 16);
		float g = camera.getPitch();
		float h = camera.getYaw();
		this.needsTerrainUpdate = this.needsTerrainUpdate || !this.chunksToRebuild.isEmpty() || camera.getPos().x != this.lastCameraX || camera.getPos().y != this.lastCameraY || camera.getPos().z != this.lastCameraZ || (double)g != this.lastCameraPitch || (double)h != this.lastCameraYaw;
		this.lastCameraX = camera.getPos().x;
		this.lastCameraY = camera.getPos().y;
		this.lastCameraZ = camera.getPos().z;
		this.lastCameraPitch = g;
		this.lastCameraYaw = h;
		this.client.getProfiler().swap("update");
		PreviewRenderer.ChunkInfo chunkInfo2;
		ChunkRenderer chunkRenderer3;
		if (this.needsTerrainUpdate) {
			this.needsTerrainUpdate = false;
			this.chunkInfos = Lists.newArrayList();
			Queue<PreviewRenderer.ChunkInfo> queue = Queues.newArrayDeque();
			Entity.setRenderDistanceMultiplier(MathHelper.clamp((double)this.client.options.viewDistance / 8.0D, 1.0D, 2.5D));
			boolean bl3 = this.client.field_1730;
			if (chunkRenderer != null) {
				boolean bl4 = false;
				PreviewRenderer.ChunkInfo chunkInfo = new PreviewRenderer.ChunkInfo(chunkRenderer, null, 0);
				Set<Direction> set = this.getOpenChunkFaces(blockPos);
				if (set.size() == 1) {
					Vec3d vec3d = camera.getHorizontalPlane();
					Direction direction = Direction.getFacing(vec3d.x, vec3d.y, vec3d.z).getOpposite();
					set.remove(direction);
				}

				if (set.isEmpty()) {
					bl4 = true;
				}

				if (bl4 && !bl) {
					this.chunkInfos.add(chunkInfo);
				} else {
					if (bl && this.world.getBlockState(blockPos).isFullOpaque(this.world, blockPos)) {
						bl3 = false;
					}

					chunkRenderer.method_3671(i);
					queue.add(chunkInfo);
				}
			} else {
				int j = blockPos.getY() > 0 ? 248 : 8;

				for(int k = -this.renderDistance; k <= this.renderDistance; ++k) {
					for(int l = -this.renderDistance; l <= this.renderDistance; ++l) {
						ChunkRenderer chunkRenderer2 =  ((BuiltChunkStorageMixin)this.chunks).callGetChunkRenderer(new BlockPos((k << 4) + 8, j, (l << 4) + 8));
						if (chunkRenderer2 != null && visibleRegion.intersects(chunkRenderer2.boundingBox)) {
							chunkRenderer2.method_3671(i);
							queue.add(new PreviewRenderer.ChunkInfo(chunkRenderer2, null, 0));
						}
					}
				}
			}

			this.client.getProfiler().push("iteration");

			while(!queue.isEmpty()) {
				chunkInfo2 = queue.poll();
				chunkRenderer3 = chunkInfo2.renderer;
				Direction direction2 = chunkInfo2.field_4125;
				this.chunkInfos.add(chunkInfo2);
				Direction[] var39 = DIRECTIONS;
				int var41 = var39.length;

				for(int var24 = 0; var24 < var41; ++var24) {
					Direction direction3 = var39[var24];
					ChunkRenderer chunkRenderer4 = this.getAdjacentChunkRenderer(blockPos2, chunkRenderer3, direction3);
					if ((!bl3 || !chunkInfo2.method_3298(direction3.getOpposite())) && (!bl3 || direction2 == null || chunkRenderer3.getData().isVisibleThrough(direction2.getOpposite(), direction3)) && chunkRenderer4 != null && chunkRenderer4.shouldBuild() && chunkRenderer4.method_3671(i) && visibleRegion.intersects(chunkRenderer4.boundingBox)) {
						PreviewRenderer.ChunkInfo chunkInfo3 = new PreviewRenderer.ChunkInfo(chunkRenderer4, direction3, chunkInfo2.field_4122 + 1);
						chunkInfo3.method_3299(chunkInfo2.field_4126, direction3);
						queue.add(chunkInfo3);
					}
				}
			}

			this.client.getProfiler().pop();
		}

		this.client.getProfiler().swap("captureFrustum");
		if (this.field_4066) {
			this.field_4066 = false;
		}

		this.client.getProfiler().swap("rebuildNear");
		Set<ChunkRenderer> set2 = this.chunksToRebuild;
		this.chunksToRebuild = Sets.newLinkedHashSet();
		Iterator var30 = this.chunkInfos.iterator();

		while(true) {
			while(true) {
				do {
					if (!var30.hasNext()) {
						this.chunksToRebuild.addAll(set2);
						this.client.getProfiler().pop();
						return;
					}

					chunkInfo2 = (PreviewRenderer.ChunkInfo)var30.next();
					chunkRenderer3 = chunkInfo2.renderer;
				} while(!chunkRenderer3.shouldRebuild() && !set2.contains(chunkRenderer3));

				this.needsTerrainUpdate = true;
				BlockPos blockPos3 = chunkRenderer3.getOrigin().add(8, 8, 8);
				boolean bl5 = blockPos3.getSquaredDistance(blockPos) < 768.0D;
				if (!chunkRenderer3.shouldRebuildOnClientThread() && !bl5) {
					this.chunksToRebuild.add(chunkRenderer3);
				} else {
					this.client.getProfiler().push("build near");
					this.chunkBuilder.rebuildSync(chunkRenderer3);
					this.client.getProfiler().pop();
				}
			}
		}
	}

	private Set<Direction> getOpenChunkFaces(BlockPos pos) {
		ChunkOcclusionDataBuilder chunkOcclusionDataBuilder = new ChunkOcclusionDataBuilder();
		BlockPos blockPos = new BlockPos(pos.getX() >> 4 << 4, pos.getY() >> 4 << 4, pos.getZ() >> 4 << 4);
		WorldChunk worldChunk = this.world.getWorldChunk(blockPos);
		Iterator var5 = BlockPos.iterate(blockPos, blockPos.add(15, 15, 15)).iterator();

		while(var5.hasNext()) {
			BlockPos blockPos2 = (BlockPos)var5.next();
			if (worldChunk.getBlockState(blockPos2).isFullOpaque(this.world, blockPos2)) {
				chunkOcclusionDataBuilder.markClosed(blockPos2);
			}
		}

		return chunkOcclusionDataBuilder.getOpenFaces(pos);
	}

	@Nullable
	private ChunkRenderer getAdjacentChunkRenderer(BlockPos pos, ChunkRenderer chunkRenderer, Direction direction) {
		BlockPos blockPos = chunkRenderer.getNeighborPosition(direction);
		if (MathHelper.abs(pos.getX() - blockPos.getX()) > this.renderDistance * 16) {
			return null;
		} else if (blockPos.getY() >= 0 && blockPos.getY() < 256) {
			return MathHelper.abs(pos.getZ() - blockPos.getZ()) > this.renderDistance * 16 ? null : ((BuiltChunkStorageMixin)this.chunks).callGetChunkRenderer(blockPos);
		} else {
			return null;
		}
	}



	public int renderLayer(RenderLayer layer, Camera camera) {
		DiffuseLighting.disable();
		if (layer == RenderLayer.TRANSLUCENT) {
			this.client.getProfiler().push("translucent_sort");
			double d = camera.getPos().x - this.lastTranslucentSortX;
			double e = camera.getPos().y - this.lastTranslucentSortY;
			double f = camera.getPos().z - this.lastTranslucentSortZ;
			if (d * d + e * e + f * f > 1.0D) {
				this.lastTranslucentSortX = camera.getPos().x;
				this.lastTranslucentSortY = camera.getPos().y;
				this.lastTranslucentSortZ = camera.getPos().z;
				int i = 0;
				Iterator var10 = this.chunkInfos.iterator();

				while(var10.hasNext()) {
					PreviewRenderer.ChunkInfo chunkInfo = (PreviewRenderer.ChunkInfo)var10.next();
					if (chunkInfo.renderer.data.isBufferInitialized(layer) && i++ < 15) {
						this.chunkBuilder.resortTransparency(chunkInfo.renderer);
					}
				}
			}

			this.client.getProfiler().pop();
		}

		this.client.getProfiler().push("filterempty");
		int j = 0;
		boolean bl = layer == RenderLayer.TRANSLUCENT;
		int k = bl ? this.chunkInfos.size() - 1 : 0;
		int l = bl ? -1 : this.chunkInfos.size();
		int m = bl ? -1 : 1;

		for(int n = k; n != l; n += m) {
			ChunkRenderer chunkRenderer = this.chunkInfos.get(n).renderer;
			if (!chunkRenderer.getData().isEmpty(layer)) {
				++j;
				this.chunkRendererList.add(chunkRenderer, layer);
			}
		}

		this.client.getProfiler().swap(() -> {
			return "render_" + layer;
		});
		this.renderLayer(layer);
		this.client.getProfiler().pop();
		return j;
	}

	private void renderLayer(RenderLayer layer) {
		//this.enableLightmap();
		if (GLX.useVbo()) {
			GlStateManager.enableClientState(32884);
			GLX.glClientActiveTexture(GLX.GL_TEXTURE0);
			GlStateManager.enableClientState(32888);
			GLX.glClientActiveTexture(GLX.GL_TEXTURE1);
			GlStateManager.enableClientState(32888);
			GLX.glClientActiveTexture(GLX.GL_TEXTURE0);
			GlStateManager.enableClientState(32886);
		}

		this.chunkRendererList.render(layer);
		if (GLX.useVbo()) {
			List<VertexFormatElement> list = VertexFormats.POSITION_COLOR_UV_LMAP.getElements();
			Iterator var3 = list.iterator();

			while(var3.hasNext()) {
				VertexFormatElement vertexFormatElement = (VertexFormatElement)var3.next();
				VertexFormatElement.Type type = vertexFormatElement.getType();
				int i = vertexFormatElement.getIndex();
				switch(type) {
					case POSITION:
						GlStateManager.disableClientState(32884);
						break;
					case UV:
						GLX.glClientActiveTexture(GLX.GL_TEXTURE0 + i);
						GlStateManager.disableClientState(32888);
						GLX.glClientActiveTexture(GLX.GL_TEXTURE0);
						break;
					case COLOR:
						GlStateManager.disableClientState(32886);
						GlStateManager.clearCurrentColor();
				}
			}
		}

		//this.disableLightmap();
	}







	public void renderSky(float tickDelta) {

			GlStateManager.disableTexture();
			Vec3d vec3d = this.world.getSkyColor(WorldPreview.camera.getBlockPos(), tickDelta);
			float f = (float)vec3d.x;
			float g = (float)vec3d.y;
			float h = (float)vec3d.z;
			GlStateManager.color3f(f, g, h);
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder bufferBuilder = tessellator.getBuffer();
			GlStateManager.depthMask(false);
			GlStateManager.enableFog();
			GlStateManager.color3f(f, g, h);
			if (this.vertexBufferObjectsEnabled) {
				this.field_4087.bind();
				GlStateManager.enableClientState(32884);
				GlStateManager.vertexPointer(3, 5126, 12, 0);
				this.field_4087.draw(7);
				VertexBuffer.unbind();
				GlStateManager.disableClientState(32884);
			} else {
				GlStateManager.callList(this.field_4117);
			}

			GlStateManager.disableFog();
			GlStateManager.disableAlphaTest();
			GlStateManager.enableBlend();
			GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
			DiffuseLighting.disable();
			float[] fs = this.world.dimension.getBackgroundColor(this.world.getSkyAngle(tickDelta), tickDelta);
			float q;
			float r;
			int m;
			float n;
			float o;
			float p;
			if (fs != null) {
				GlStateManager.disableTexture();
				GlStateManager.shadeModel(7425);
				GlStateManager.pushMatrix();
				GlStateManager.rotatef(90.0F, 1.0F, 0.0F, 0.0F);
				GlStateManager.rotatef(MathHelper.sin(this.world.getSkyAngleRadians(tickDelta)) < 0.0F ? 180.0F : 0.0F, 0.0F, 0.0F, 1.0F);
				GlStateManager.rotatef(90.0F, 0.0F, 0.0F, 1.0F);
				q = fs[0];
				r = fs[1];
				float k = fs[2];
				bufferBuilder.begin(6, VertexFormats.POSITION_COLOR);
				bufferBuilder.vertex(0.0D, 100.0D, 0.0D).color(q, r, k, fs[3]).next();


				for(m = 0; m <= 16; ++m) {
					n = (float)m * 6.2831855F / 16.0F;
					o = MathHelper.sin(n);
					p = MathHelper.cos(n);
					bufferBuilder.vertex(o * 120.0F, p * 120.0F, -p * 40.0F * fs[3]).color(fs[0], fs[1], fs[2], 0.0F).next();
				}

				tessellator.draw();
				GlStateManager.popMatrix();
				GlStateManager.shadeModel(7424);
			}

			GlStateManager.enableTexture();
			GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
			GlStateManager.pushMatrix();
			q = 1.0F - this.world.getRainGradient(tickDelta);
			GlStateManager.color4f(1.0F, 1.0F, 1.0F, q);
			GlStateManager.rotatef(-90.0F, 0.0F, 1.0F, 0.0F);
			GlStateManager.rotatef(this.world.getSkyAngle(tickDelta) * 360.0F, 1.0F, 0.0F, 0.0F);
			r = 30.0F;
			this.textureManager.bindTexture(SUN);
			bufferBuilder.begin(7, VertexFormats.POSITION_TEXTURE);
			bufferBuilder.vertex(-r, 100.0D, -r).texture(0.0D, 0.0D).next();
			bufferBuilder.vertex(r, 100.0D, -r).texture(1.0D, 0.0D).next();
			bufferBuilder.vertex(r, 100.0D, r).texture(1.0D, 1.0D).next();
			bufferBuilder.vertex(-r, 100.0D, r).texture(0.0D, 1.0D).next();
			tessellator.draw();
			r = 20.0F;
			this.textureManager.bindTexture(MOON_PHASES);
			int s = this.world.getMoonPhase();
			int t = s % 4;
			m = s / 4 % 2;
			n = (float)(t) / 4.0F;
			o = (float)(m) / 2.0F;
			p = (float)(t + 1) / 4.0F;
			float y = (float)(m + 1) / 2.0F;
			bufferBuilder.begin(7, VertexFormats.POSITION_TEXTURE);
			bufferBuilder.vertex(-r, -100.0D, r).texture(p, y).next();
			bufferBuilder.vertex(r, -100.0D, r).texture(n, y).next();
			bufferBuilder.vertex(r, -100.0D, -r).texture(n, o).next();
			bufferBuilder.vertex(-r, -100.0D, -r).texture(p, o).next();
			tessellator.draw();
			GlStateManager.disableTexture();
			float z = this.world.getStarsBrightness(tickDelta) * q;
			if (z > 0.0F) {
				GlStateManager.color4f(z, z, z, z);
				if (this.vertexBufferObjectsEnabled) {
					this.starsBuffer.bind();
					GlStateManager.enableClientState(32884);
					GlStateManager.vertexPointer(3, 5126, 12, 0);
					this.starsBuffer.draw(7);
					VertexBuffer.unbind();
					GlStateManager.disableClientState(32884);
				} else {
					GlStateManager.callList(this.starsDisplayList);
				}
			}

			GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.disableBlend();
			GlStateManager.enableAlphaTest();
			GlStateManager.enableFog();
			GlStateManager.popMatrix();
			GlStateManager.disableTexture();
			GlStateManager.color3f(0.0F, 0.0F, 0.0F);
			double d = WorldPreview.player.getCameraPosVec(tickDelta).y - this.world.getHorizonHeight();
			if (d < 0.0D) {
				GlStateManager.pushMatrix();
				GlStateManager.translatef(0.0F, 12.0F, 0.0F);
				if (this.vertexBufferObjectsEnabled) {
					this.field_4102.bind();
					GlStateManager.enableClientState(32884);
					GlStateManager.vertexPointer(3, 5126, 12, 0);
					this.field_4102.draw(7);
					VertexBuffer.unbind();
					GlStateManager.disableClientState(32884);
				} else {
					GlStateManager.callList(this.field_4067);
				}

				GlStateManager.popMatrix();
			}

			if (this.world.dimension.method_12449()) {
				GlStateManager.color3f(f * 0.2F + 0.04F, g * 0.2F + 0.04F, h * 0.6F + 0.1F);
			} else {
				GlStateManager.color3f(f, g, h);
			}

			GlStateManager.pushMatrix();
			GlStateManager.translatef(0.0F, -((float)(d - 16.0D)), 0.0F);
			GlStateManager.callList(this.field_4067);
			GlStateManager.popMatrix();
			GlStateManager.enableTexture();
			GlStateManager.depthMask(true);
	}

	public void renderClouds(float tickDelta, double d, double e, double f) {
		if (this.world.dimension.hasVisibleSky()) {
			double j = ((float)this.ticks + tickDelta) * 0.03F;
			double k = (d + j) / 12.0D;
			double l = this.world.dimension.getCloudHeight() - (float)e + 0.33F;
			double m = f / 12.0D + 0.33000001311302185D;
			k -= MathHelper.floor(k / 2048.0D) * 2048;
			m -= MathHelper.floor(m / 2048.0D) * 2048;
			float n = (float)(k - (double)MathHelper.floor(k));
			float o = (float)(l / 4.0D - (double)MathHelper.floor(l / 4.0D)) * 4.0F;
			float p = (float)(m - (double)MathHelper.floor(m));
			Vec3d vec3d = this.world.getCloudColor(tickDelta);
			int q = (int)Math.floor(k);
			int r = (int)Math.floor(l / 4.0D);
			int s = (int)Math.floor(m);
			if (q != this.field_4082 || r != this.field_4097 || s != this.field_4116 || this.client.options.getCloudRenderMode() != this.field_4080 || this.field_4072.squaredDistanceTo(vec3d) > 2.0E-4D) {
				this.field_4082 = q;
				this.field_4097 = r;
				this.field_4116 = s;
				this.field_4072 = vec3d;
				this.field_4080 = this.client.options.getCloudRenderMode();
				this.cloudsDirty = true;
			}

			if (this.cloudsDirty) {
				this.cloudsDirty = false;
				Tessellator tessellator = Tessellator.getInstance();
				BufferBuilder bufferBuilder = tessellator.getBuffer();
				if (this.cloudsBuffer != null) {
					this.cloudsBuffer.delete();
				}

				if (this.cloudsDisplayList >= 0) {
					GlAllocationUtils.deleteSingletonList(this.cloudsDisplayList);
					this.cloudsDisplayList = -1;
				}

				if (this.vertexBufferObjectsEnabled) {
					this.cloudsBuffer = new VertexBuffer(VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);
					this.renderClouds(bufferBuilder, k, l, m, vec3d);
					bufferBuilder.end();
					bufferBuilder.clear();
					this.cloudsBuffer.set(bufferBuilder.getByteBuffer());
				} else {
					this.cloudsDisplayList = GlAllocationUtils.genLists(1);
					GlStateManager.newList(this.cloudsDisplayList, 4864);
					this.renderClouds(bufferBuilder, k, l, m, vec3d);
					tessellator.draw();
					GlStateManager.endList();
				}
			}

			GlStateManager.disableCull();
			this.textureManager.bindTexture(CLOUDS);
			GlStateManager.enableBlend();
			GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
			GlStateManager.pushMatrix();
			GlStateManager.scalef(12.0F, 1.0F, 12.0F);
			GlStateManager.translatef(-n, o, -p);
			int v;
			int w;
			if (this.vertexBufferObjectsEnabled && this.cloudsBuffer != null) {
				this.cloudsBuffer.bind();
				GlStateManager.enableClientState(32884);
				GlStateManager.enableClientState(32888);
				GLX.glClientActiveTexture(GLX.GL_TEXTURE0);
				GlStateManager.enableClientState(32886);
				GlStateManager.enableClientState(32885);
				GlStateManager.vertexPointer(3, 5126, 28, 0);
				GlStateManager.texCoordPointer(2, 5126, 28, 12);
				GlStateManager.colorPointer(4, 5121, 28, 20);
				GlStateManager.normalPointer(5120, 28, 24);
				v = this.field_4080 == CloudRenderMode.FANCY ? 0 : 1;

				for(w = v; w < 2; ++w) {
					if (w == 0) {
						GlStateManager.colorMask(false, false, false, false);
					} else {
						GlStateManager.colorMask(true, true, true, true);
					}

					this.cloudsBuffer.draw(7);
				}

				VertexBuffer.unbind();
				GlStateManager.disableClientState(32884);
				GlStateManager.disableClientState(32888);
				GlStateManager.disableClientState(32886);
				GlStateManager.disableClientState(32885);
			} else if (this.cloudsDisplayList >= 0) {
				v = this.field_4080 == CloudRenderMode.FANCY ? 0 : 1;

				for(w = v; w < 2; ++w) {
					if (w == 0) {
						GlStateManager.colorMask(false, false, false, false);
					} else {
						GlStateManager.colorMask(true, true, true, true);
					}

					GlStateManager.callList(this.cloudsDisplayList);
				}
			}

			GlStateManager.popMatrix();
			GlStateManager.clearCurrentColor();
			GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.disableBlend();
			GlStateManager.enableCull();
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
		builder.begin(7, VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);
		float ab = (float)Math.floor(y / 4.0D) * 4.0F;
		if (this.field_4080 == CloudRenderMode.FANCY) {
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
					builder.vertex(am, ab, an + 32).texture((float)(am) * 0.00390625F + k, (float)(an + 32) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
					builder.vertex(am + 32, ab, an + 32).texture((float)(am + 32) * 0.00390625F + k, (float)(an + 32) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
					builder.vertex(am + 32, ab, an).texture((float)(am + 32) * 0.00390625F + k, (float)(an) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
					builder.vertex(am, ab, an).texture((float)(am) * 0.00390625F + k, (float)(an) * 0.00390625F + l).color(m, n, o, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
				}
			}
		}

	}

	public void updateChunks(long limitTime) {
		this.needsTerrainUpdate |= this.chunkBuilder.runTasksSync(limitTime);
		if (!this.chunksToRebuild.isEmpty()) {
			Iterator iterator = this.chunksToRebuild.iterator();

			while(iterator.hasNext()) {
				ChunkRenderer chunkRenderer = (ChunkRenderer)iterator.next();
				boolean bl2;
				if (chunkRenderer.shouldRebuildOnClientThread()) {
					bl2 = this.chunkBuilder.rebuildSync(chunkRenderer);
				} else {
					bl2 = this.chunkBuilder.rebuild(chunkRenderer);
				}

				if (!bl2) {
					break;
				}

				chunkRenderer.unscheduleRebuild();
				iterator.remove();
				long l = limitTime - Util.getMeasuringTimeNano();
				if (l < 0L) {
					break;
				}
			}
		}

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

	@Environment(EnvType.CLIENT)
	public class ChunkInfo {
		private final ChunkRenderer renderer;
		private final Direction field_4125;
		private byte field_4126;
		private final int field_4122;

		private ChunkInfo(ChunkRenderer chunkRenderer, @Nullable Direction direction, int i) {
			this.renderer = chunkRenderer;
			this.field_4125 = direction;
			this.field_4122 = i;
		}

		public void method_3299(byte b, Direction direction) {
			this.field_4126 = (byte)(this.field_4126 | b | 1 << direction.ordinal());
		}

		public boolean method_3298(Direction direction) {
			return (this.field_4126 & 1 << direction.ordinal()) > 0;
		}
	}


@Environment(EnvType.CLIENT)
public class BackgroundRenderer {
	private final FloatBuffer blackColorBuffer = GlAllocationUtils.allocateFloatBuffer(16);
	private final FloatBuffer colorBuffer = GlAllocationUtils.allocateFloatBuffer(16);
	private float red;
	private float green;
	private float blue;
	private float bufferRed = -1.0F;
	private float bufferGreen = -1.0F;
	private float bufferBlue = -1.0F;
	private int waterFogColor = -1;
	private int nextWaterFogColor = -1;
	private long lastWaterFogColorUpdateTime = -1L;
	private final MinecraftClient client;

	public BackgroundRenderer() {

		this.client = PreviewRenderer.this.client;
		this.blackColorBuffer.put(0.0F).put(0.0F).put(0.0F).put(1.0F).flip();
	}

	public void renderBackground(Camera camera, float tickDelta) {
		World world = PreviewRenderer.this.world;
		FluidState fluidState = camera.getSubmergedFluidState();
		if (fluidState.matches(FluidTags.WATER)) {
			this.updateColorInWater(camera, world);
		} else if (fluidState.matches(FluidTags.LAVA)) {
			this.red = 0.6F;
			this.green = 0.1F;
			this.blue = 0.0F;
			this.lastWaterFogColorUpdateTime = -1L;
		} else {
			this.updateColorNotInWater(camera, world, tickDelta);
			this.lastWaterFogColorUpdateTime = -1L;
		}

		double d = camera.getPos().y * world.dimension.getHorizonShadingRatio();
		if (camera.getFocusedEntity() instanceof LivingEntity && ((LivingEntity)camera.getFocusedEntity()).hasStatusEffect(StatusEffects.BLINDNESS)) {
			int i = ((LivingEntity)camera.getFocusedEntity()).getStatusEffect(StatusEffects.BLINDNESS).getDuration();
			if (i < 20) {
				d *= 1.0F - (float)i / 20.0F;
			} else {
				d = 0.0D;
			}
		}

		if (d < 1.0D) {
			if (d < 0.0D) {
				d = 0.0D;
			}

			d *= d;
			this.red = (float)((double)this.red * d);
			this.green = (float)((double)this.green * d);
			this.blue = (float)((double)this.blue * d);
		}

		float g;


		float h;
		if (fluidState.matches(FluidTags.WATER)) {
			g = 0.0F;
			if (camera.getFocusedEntity() instanceof ClientPlayerEntity) {
				ClientPlayerEntity clientPlayerEntity = (ClientPlayerEntity)camera.getFocusedEntity();
				g = clientPlayerEntity.method_3140();
			}

			h = 1.0F / this.red;
			if (h > 1.0F / this.green) {
				h = 1.0F / this.green;
			}

			if (h > 1.0F / this.blue) {
				h = 1.0F / this.blue;
			}

			this.red = this.red * (1.0F - g) + this.red * h * g;
			this.green = this.green * (1.0F - g) + this.green * h * g;
			this.blue = this.blue * (1.0F - g) + this.blue * h * g;
		}

		GlStateManager.clearColor(this.red, this.green, this.blue, 0.0F);
	}

	private void updateColorNotInWater(Camera camera, World world, float tickDelta) {
		float f = 0.25F + 0.75F * (float)this.client.options.viewDistance / 32.0F;
		f = 1.0F - (float)Math.pow(f, 0.25D);
		Vec3d vec3d = world.getSkyColor(camera.getBlockPos(), tickDelta);
		float g = (float)vec3d.x;
		float h = (float)vec3d.y;
		float i = (float)vec3d.z;
		Vec3d vec3d2 = world.getFogColor(tickDelta);
		this.red = (float)vec3d2.x;
		this.green = (float)vec3d2.y;
		this.blue = (float)vec3d2.z;
		if (this.client.options.viewDistance >= 4) {
			double d = MathHelper.sin(world.getSkyAngleRadians(tickDelta)) > 0.0F ? -1.0D : 1.0D;
			Vec3d vec3d3 = new Vec3d(d, 0.0D, 0.0D);
			float j = (float)camera.getHorizontalPlane().dotProduct(vec3d3);
			if (j < 0.0F) {
				j = 0.0F;
			}

			if (j > 0.0F) {
				float[] fs = world.dimension.getBackgroundColor(world.getSkyAngle(tickDelta), tickDelta);
				if (fs != null) {
					j *= fs[3];
					this.red = this.red * (1.0F - j) + fs[0] * j;
					this.green = this.green * (1.0F - j) + fs[1] * j;
					this.blue = this.blue * (1.0F - j) + fs[2] * j;
				}
			}
		}

		this.red += (g - this.red) * f;
		this.green += (h - this.green) * f;
		this.blue += (i - this.blue) * f;
		float k = world.getRainGradient(tickDelta);
		float n;
		float o;
		if (k > 0.0F) {
			n = 1.0F - k * 0.5F;
			o = 1.0F - k * 0.4F;
			this.red *= n;
			this.green *= n;
			this.blue *= o;
		}

		n = world.getThunderGradient(tickDelta);
		if (n > 0.0F) {
			o = 1.0F - n * 0.5F;
			this.red *= o;
			this.green *= o;
			this.blue *= o;
		}

	}

	private void updateColorInWater(Camera camera, CollisionView world) {
		long l = Util.getMeasuringTimeMs();
		int i = world.getBiome(new BlockPos(camera.getPos())).getWaterFogColor();
		if (this.lastWaterFogColorUpdateTime < 0L) {
			this.waterFogColor = i;
			this.nextWaterFogColor = i;
			this.lastWaterFogColorUpdateTime = l;
		}

		int j = this.waterFogColor >> 16 & 255;
		int k = this.waterFogColor >> 8 & 255;
		int m = this.waterFogColor & 255;
		int n = this.nextWaterFogColor >> 16 & 255;
		int o = this.nextWaterFogColor >> 8 & 255;
		int p = this.nextWaterFogColor & 255;
		float f = MathHelper.clamp((float)(l - this.lastWaterFogColorUpdateTime) / 5000.0F, 0.0F, 1.0F);
		float g = MathHelper.lerp(f, (float)n, (float)j);
		float h = MathHelper.lerp(f, (float)o, (float)k);
		float q = MathHelper.lerp(f, (float)p, (float)m);
		this.red = g / 255.0F;
		this.green = h / 255.0F;
		this.blue = q / 255.0F;
		if (this.waterFogColor != i) {
			this.waterFogColor = i;
			this.nextWaterFogColor = MathHelper.floor(g) << 16 | MathHelper.floor(h) << 8 | MathHelper.floor(q);
			this.lastWaterFogColorUpdateTime = l;
		}

	}

	public void applyFog(Camera camera, int i) {
		this.setFogBlack(false);
		GlStateManager.normal3f(0.0F, -1.0F, 0.0F);
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		FluidState fluidState = camera.getSubmergedFluidState();
		float h;
		if (camera.getFocusedEntity() instanceof LivingEntity && ((LivingEntity)camera.getFocusedEntity()).hasStatusEffect(StatusEffects.BLINDNESS)) {
			h = 5.0F;
			int j = ((LivingEntity)camera.getFocusedEntity()).getStatusEffect(StatusEffects.BLINDNESS).getDuration();
			if (j < 20) {
				h = MathHelper.lerp(1.0F - (float)j / 20.0F, 5.0F, PreviewRenderer.this.client.options.viewDistance*16);
			}

			GlStateManager.fogMode(GlStateManager.FogMode.LINEAR);
			if (i == -1) {
				GlStateManager.fogStart(0.0F);
				GlStateManager.fogEnd(h * 0.8F);
			} else {
				GlStateManager.fogStart(h * 0.25F);
				GlStateManager.fogEnd(h);
			}

			GLX.setupNvFogDistance();
		} else if (fluidState.matches(FluidTags.WATER)) {
			GlStateManager.fogMode(GlStateManager.FogMode.EXP2);
			if (camera.getFocusedEntity() instanceof LivingEntity) {
				if (camera.getFocusedEntity() instanceof ClientPlayerEntity) {
					ClientPlayerEntity clientPlayerEntity = (ClientPlayerEntity)camera.getFocusedEntity();
					float g = 0.05F - clientPlayerEntity.method_3140() * clientPlayerEntity.method_3140() * 0.03F;
					Biome biome = clientPlayerEntity.world.getBiome(new BlockPos(clientPlayerEntity));
					if (biome == Biomes.SWAMP || biome == Biomes.SWAMP_HILLS) {
						g += 0.005F;
					}

					GlStateManager.fogDensity(g);
				} else {
					GlStateManager.fogDensity(0.05F);
				}
			} else {
				GlStateManager.fogDensity(0.1F);
			}
		} else if (fluidState.matches(FluidTags.LAVA)) {
			GlStateManager.fogMode(GlStateManager.FogMode.EXP);
			GlStateManager.fogDensity(2.0F);
		} else {
			h = PreviewRenderer.this.client.options.viewDistance*16;
			GlStateManager.fogMode(GlStateManager.FogMode.LINEAR);
			if (i == -1) {
				GlStateManager.fogStart(0.0F);
				GlStateManager.fogEnd(h);
			} else {
				GlStateManager.fogStart(h * 0.75F);
				GlStateManager.fogEnd(h);
			}

			GLX.setupNvFogDistance();

		}

		GlStateManager.enableColorMaterial();
		GlStateManager.enableFog();
		GlStateManager.colorMaterial(1028, 4608);
	}

	public void setFogBlack(boolean fogBlack) {
		if (fogBlack) {
			GlStateManager.fog(2918, this.blackColorBuffer);
		} else {
			GlStateManager.fog(2918, this.getColorAsBuffer());
		}

	}

	private FloatBuffer getColorAsBuffer() {
		if (this.bufferRed != this.red || this.bufferGreen != this.green || this.bufferBlue != this.blue) {
			this.colorBuffer.clear();
			this.colorBuffer.put(this.red).put(this.green).put(this.blue).put(1.0F);
			this.colorBuffer.flip();
			this.bufferRed = this.red;
			this.bufferGreen = this.green;
			this.bufferBlue = this.blue;
		}

		return this.colorBuffer;
	}
}


}
