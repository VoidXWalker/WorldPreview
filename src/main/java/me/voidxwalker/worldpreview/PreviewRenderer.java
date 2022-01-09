package me.voidxwalker.worldpreview;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import me.voidxwalker.worldpreview.mixin.access.BuiltChunkStorageMixin;
import me.voidxwalker.worldpreview.mixin.access.RenderPhaseMixin;
import me.voidxwalker.worldpreview.mixin.access.WorldRendererMixin;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.options.*;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.resource.SynchronousResourceReloadListener;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Environment(EnvType.CLIENT)
public class PreviewRenderer extends WorldRenderer implements SynchronousResourceReloadListener, AutoCloseable {
	private static final Identifier MOON_PHASES = new Identifier("textures/environment/moon_phases.png");
	private static final Identifier SUN = new Identifier("textures/environment/sun.png");
	private static final Identifier CLOUDS = new Identifier("textures/environment/clouds.png");
	private static final Identifier RAIN = new Identifier("textures/environment/rain.png");
	private static final Identifier SNOW = new Identifier("textures/environment/snow.png");
	public static final Direction[] DIRECTIONS = Direction.values();
	private Set<ChunkBuilder.BuiltChunk> chunksToRebuild = Sets.newLinkedHashSet();
	private final ObjectArrayList<ChunkInfo> visibleChunks = new ObjectArrayList<>(69696);
	private BuiltChunkStorage chunks;

	@Nullable
	private VertexBuffer lightSkyBuffer;
	private boolean cloudsDirty;
	private ClientWorld world;
	@Nullable
	private VertexBuffer cloudsBuffer;
	public int ticks;


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
	private double lastTranslucentSortX;
	private double lastTranslucentSortY;
	private double lastTranslucentSortZ;
	private boolean needsTerrainUpdate;
	private int frame;

	public PreviewRenderer(MinecraftClient client, BufferBuilderStorage bufferBuilders) {
		super(client, bufferBuilders);
		this.cloudsDirty = true;
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
		this.needsTerrainUpdate = true;
		this.renderLightSky();
	}

	private void renderWeather(LightmapTextureManager manager, float f, double d, double e, double g) {
		float h = this.world.getRainGradient(f);
		if (!(h <= 0.0F)) {
			manager.enable();
			World world = this.world;
			int i = MathHelper.floor(d);
			int j = MathHelper.floor(e);
			int k = MathHelper.floor(g);
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder bufferBuilder = tessellator.getBuffer();
			RenderSystem.enableAlphaTest();
			RenderSystem.disableCull();
			RenderSystem.normal3f(0.0F, 1.0F, 0.0F);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.defaultAlphaFunc();
			RenderSystem.enableDepthTest();
			int l = 5;
			if (MinecraftClient.isFancyGraphicsOrBetter()) {
				l = 10;
			}

			RenderSystem.depthMask(MinecraftClient.isFabulousGraphicsOrBetter());
			int m = -1;
			float n = (float)this.ticks + f;
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			BlockPos.Mutable mutable = new BlockPos.Mutable();

			for(int o = k - l; o <= k + l; ++o) {
				for(int p = i - l; p <= i + l; ++p) {
					int q = (o - k + 16) * 32 + p - i + 16;
					double r = (double)((WorldRendererMixin)this).getField_20794()[q] * 0.5D;
					double s = (double)((WorldRendererMixin)this).getField_20795()[q] * 0.5D;
					mutable.set(p, 0, o);
					Biome biome = world.getBiome(mutable);
					if (biome.getPrecipitation() != Biome.Precipitation.NONE) {
						int t = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, mutable).getY();
						int u = j - l;
						int v = j + l;
						if (u < t) {
							u = t;
						}

						if (v < t) {
							v = t;
						}

						int w = Math.max(t, j);

						if (u != v) {
							Random random = new Random((long) p * p * 3121 + p * 45238971L ^ (long) o * o * 418711 + o * 13761L);
							mutable.set(p, u, o);
							float x = biome.getTemperature(mutable);
							float z;
							float ad;
							if (x >= 0.15F) {
								if (m != 0) {
									if (m >= 0) {
										tessellator.draw();
									}

									m = 0;
									((WorldRendererMixin)this).getClient().getTextureManager().bindTexture(RAIN);
									bufferBuilder.begin(7, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
								}

								int y = this.ticks + p * p * 3121 + p * 45238971 + o * o * 418711 + o * 13761 & 31;
								z = -((float)y + f) / 32.0F * (3.0F + random.nextFloat());
								double aa = (double)((float)p + 0.5F) - d;
								double ab = (double)((float)o + 0.5F) - g;
								float ac = MathHelper.sqrt(aa * aa + ab * ab) / (float)l;
								ad = ((1.0F - ac * ac) * 0.5F + 0.5F) * h;
								mutable.set(p, w, o);
								int ae = getLightmapCoordinates(world, mutable);
								bufferBuilder.vertex((double)p - d - r + 0.5D, (double)v - e, (double)o - g - s + 0.5D).texture(0.0F, (float)u * 0.25F + z).color(1.0F, 1.0F, 1.0F, ad).light(ae).next();
								bufferBuilder.vertex((double)p - d + r + 0.5D, (double)v - e, (double)o - g + s + 0.5D).texture(1.0F, (float)u * 0.25F + z).color(1.0F, 1.0F, 1.0F, ad).light(ae).next();
								bufferBuilder.vertex((double)p - d + r + 0.5D, (double)u - e, (double)o - g + s + 0.5D).texture(1.0F, (float)v * 0.25F + z).color(1.0F, 1.0F, 1.0F, ad).light(ae).next();
								bufferBuilder.vertex((double)p - d - r + 0.5D, (double)u - e, (double)o - g - s + 0.5D).texture(0.0F, (float)v * 0.25F + z).color(1.0F, 1.0F, 1.0F, ad).light(ae).next();
							} else {
								if (m != 1) {
									if (m >= 0) {
										tessellator.draw();
									}

									m = 1;
									((WorldRendererMixin)this).getClient().getTextureManager().bindTexture(SNOW);
									bufferBuilder.begin(7, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
								}

								float af = -((float)(this.ticks & 511) + f) / 512.0F;
								z = (float)(random.nextDouble() + (double)n * 0.01D * (double)((float)random.nextGaussian()));
								float ah = (float)(random.nextDouble() + (double)(n * (float)random.nextGaussian()) * 0.001D);
								double ai = (double)((float)p + 0.5F) - d;
								double aj = (double)((float)o + 0.5F) - g;
								ad = MathHelper.sqrt(ai * ai + aj * aj) / (float)l;
								float al = ((1.0F - ad * ad) * 0.3F + 0.5F) * h;
								mutable.set(p, w, o);
								int am = getLightmapCoordinates(world, mutable);
								int an = am >> 16 & '\uffff';
								int ao = (am & '\uffff') * 3;
								int ap = (an * 3 + 240) / 4;
								int aq = (ao * 3 + 240) / 4;
								bufferBuilder.vertex((double)p - d - r + 0.5D, (double)v - e, (double)o - g - s + 0.5D).texture(0.0F + z, (float)u * 0.25F + af + ah).color(1.0F, 1.0F, 1.0F, al).light(aq, ap).next();
								bufferBuilder.vertex((double)p - d + r + 0.5D, (double)v - e, (double)o - g + s + 0.5D).texture(1.0F + z, (float)u * 0.25F + af + ah).color(1.0F, 1.0F, 1.0F, al).light(aq, ap).next();
								bufferBuilder.vertex((double)p - d + r + 0.5D, (double)u - e, (double)o - g + s + 0.5D).texture(1.0F + z, (float)v * 0.25F + af + ah).color(1.0F, 1.0F, 1.0F, al).light(aq, ap).next();
								bufferBuilder.vertex((double)p - d - r + 0.5D, (double)u - e, (double)o - g - s + 0.5D).texture(0.0F + z, (float)v * 0.25F + af + ah).color(1.0F, 1.0F, 1.0F, al).light(aq, ap).next();
							}
						}
					}
				}
			}

			if (m >= 0) {
				tessellator.draw();
			}

			RenderSystem.enableCull();
			RenderSystem.disableBlend();
			RenderSystem.defaultAlphaFunc();
			RenderSystem.disableAlphaTest();
			manager.disable();
		}
	}

	public void setWorld(@Nullable ClientWorld clientWorld) {
		this.lastCameraChunkUpdateX = Double.MIN_VALUE;
		this.lastCameraChunkUpdateY = Double.MIN_VALUE;
		this.lastCameraChunkUpdateZ = Double.MIN_VALUE;
		this.cameraChunkX = Integer.MIN_VALUE;
		this.cameraChunkY = Integer.MIN_VALUE;
		this.cameraChunkZ = Integer.MIN_VALUE;
		((WorldRendererMixin)this).getEntityRenderDispatcher().setWorld(clientWorld);
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
			((WorldRendererMixin)this).getNoCullingBlockEntities().clear();
		}

	}

	private void renderLightSky() {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		if (this.lightSkyBuffer != null) {
			this.lightSkyBuffer.close();
		}

		this.lightSkyBuffer = new VertexBuffer(((WorldRendererMixin)this).getSkyVertexFormat());
		((WorldRendererMixin)this).callRenderSkyHalf(bufferBuilder, 16.0F, false);
		bufferBuilder.end();
		this.lightSkyBuffer.upload(bufferBuilder);
	}



	public void reload() {
		if (this.world != null) {
			if (MinecraftClient.isFabulousGraphicsOrBetter()) {
				((WorldRendererMixin)this).callLoadTransparencyShader();
			} else {
				((WorldRendererMixin)this).callResetTransparencyShader();
			}

			this.world.reloadColor();
			if (this.chunkBuilder == null) {
				this.chunkBuilder = new ChunkBuilder(this.world, this, Util.getServerWorkerExecutor(), ((WorldRendererMixin)this).getClient().is64Bit(), ((WorldRendererMixin)this).getBufferBuilders().getBlockBufferBuilders());
			} else {
				this.chunkBuilder.setWorld(this.world);
			}

			this.needsTerrainUpdate = true;
			this.cloudsDirty = true;
			RenderLayers.setFancyGraphicsOrBetter(MinecraftClient.isFancyGraphicsOrBetter());
			this.renderDistance = ((WorldRendererMixin)this).getClient().options.viewDistance;
			if (this.chunks != null) {
				this.chunks.clear();
			}

			this.clearChunkRenderers();
			synchronized(((WorldRendererMixin)this).getNoCullingBlockEntities()) {
				((WorldRendererMixin)this).getNoCullingBlockEntities().clear();
			}

			this.chunks = new BuiltChunkStorage(this.chunkBuilder, this.world, ((WorldRendererMixin)this).getClient().options.viewDistance, this);
			if (this.world != null) {
				Entity entity = Main.player;
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


	public String getChunksDebugString() {
		int i = this.chunks.chunks.length;
		int j = this.getCompletedChunkCount();
		return String.format("C: %d/%d %sD: %d, %s", j, i, ((WorldRendererMixin)this).getClient().chunkCullingEnabled ? "(s) " : "", this.renderDistance, this.chunkBuilder == null ? "null" : this.chunkBuilder.getDebugString());
	}
	public boolean isTerrainRenderComplete() {
		return this.chunksToRebuild.isEmpty() && this.chunkBuilder.isEmpty();
	}
	protected int getCompletedChunkCount() {
		int i = 0;
		ObjectListIterator<ChunkInfo> var2 = this.visibleChunks.iterator();

		while(var2.hasNext()) {
			PreviewRenderer.ChunkInfo chunkInfo = var2.next();
			if (!chunkInfo.chunk.getData().isEmpty()) {
				++i;
			}
		}

		return i;
	}
	public void scheduleTerrainUpdate() {
		this.needsTerrainUpdate = true;
		this.cloudsDirty = true;
	}
	public String getEntitiesDebugString() {
		return "E: " + this.regularEntityCount + "/" + this.world.getRegularEntityCount() + ", B: " + this.blockEntityCount;
	}

	private void setupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, int frame, boolean spectator) {
		Vec3d vec3d = camera.getPos();
		if (((WorldRendererMixin)this).getClient().options.viewDistance != this.renderDistance) {
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
			this.cameraChunkX =Main.player.chunkX;
			this.cameraChunkY = Main.player.chunkY;
			this.cameraChunkZ =Main.player.chunkZ;
			this.chunks.updateCameraPosition(Main.player.getX(), Main.player.getZ());
		}

		this.chunkBuilder.setCameraPosition(vec3d);
		this.world.getProfiler().swap("cull");
		((WorldRendererMixin)this).getClient().getProfiler().swap("culling");
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
		((WorldRendererMixin)this).getClient().getProfiler().swap("update");
		PreviewRenderer.ChunkInfo chunkInfo;
		ChunkBuilder.BuiltChunk builtChunk3;
		if (!hasForcedFrustum && this.needsTerrainUpdate) {
			this.needsTerrainUpdate = false;
			this.visibleChunks.clear();

			Queue<PreviewRenderer.ChunkInfo> queue = Queues.newArrayDeque();
			Entity.setRenderDistanceMultiplier(MathHelper.clamp((double)((WorldRendererMixin)this).getClient().options.viewDistance / 8.0D, 1.0D, 2.5D) * (double)((WorldRendererMixin)this).getClient().options.entityDistanceScaling);
			boolean bl = ((WorldRendererMixin)this).getClient().chunkCullingEnabled;
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

			((WorldRendererMixin)this).getClient().getProfiler().push("iteration");

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

			((WorldRendererMixin)this).getClient().getProfiler().pop();
		}

		((WorldRendererMixin)this).getClient().getProfiler().swap("rebuildNear");
		Set<ChunkBuilder.BuiltChunk> set = this.chunksToRebuild;
		this.chunksToRebuild = Sets.newLinkedHashSet();
		ObjectListIterator<ChunkInfo> var31 = this.visibleChunks.iterator();

		while(true) {
			while(true) {
				do {
					if (!var31.hasNext()) {
						this.chunksToRebuild.addAll(set);
						((WorldRendererMixin)this).getClient().getProfiler().pop();
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
					((WorldRendererMixin)this).getClient().getProfiler().push("build near");
					this.chunkBuilder.rebuild(builtChunk3);
					((WorldRendererMixin)this).getClient().getProfiler().pop();
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


	public void render(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f) {
		BlockEntityRenderDispatcher.INSTANCE.configure(this.world, ((WorldRendererMixin)this).getClient().getTextureManager(), ((WorldRendererMixin)this).getClient().textRenderer, camera, ((WorldRendererMixin)this).getClient().crosshairTarget);
		((WorldRendererMixin)this).getEntityRenderDispatcher().configure(this.world, camera,Main.player);
		Profiler profiler = this.world.getProfiler();
		profiler.swap("light_updates");
		this.world.getChunkManager().getLightingProvider().doLightUpdates(Integer.MAX_VALUE, true, true);
		Vec3d vec3d = camera.getPos();
		double d = vec3d.getX();
		double e = vec3d.getY();
		double f = vec3d.getZ();
		Matrix4f matrix4f2 = matrices.peek().getModel();
		profiler.swap("culling");
		boolean bl = ((WorldRendererMixin)this).getCapturedFrustum() != null;
		Frustum frustum2;
		if (bl) {
			frustum2 = ((WorldRendererMixin)this).getCapturedFrustum();
			frustum2.setPosition(((WorldRendererMixin)this).getCapturedFrustumPosition().x, ((WorldRendererMixin)this).getCapturedFrustumPosition().y, ((WorldRendererMixin)this).getCapturedFrustumPosition().z);
		} else {
			frustum2 = new Frustum(matrix4f2, matrix4f);
			frustum2.setPosition(d, e, f);
		}

		((WorldRendererMixin)this).getClient().getProfiler().swap("captureFrustum");
		if (this.shouldCaptureFrustum) {
			((WorldRendererMixin)this).callCaptureFrustum(matrix4f2, matrix4f, vec3d.x, vec3d.y, vec3d.z, bl ? new Frustum(matrix4f2, matrix4f) : frustum2);
			this.shouldCaptureFrustum = false;
		}

		profiler.swap("clear");
		BackgroundRenderer.render(camera, tickDelta, this.world, ((WorldRendererMixin)this).getClient().options.viewDistance, gameRenderer.getSkyDarkness(tickDelta));
		RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);
		float g = ((WorldRendererMixin)this).getClient().options.viewDistance*16;
		boolean bl2 = this.world.getSkyProperties().useThickFog(MathHelper.floor(d), MathHelper.floor(e)) || ((WorldRendererMixin)this).getClient().inGameHud.getBossBarHud().shouldThickenFog();
		if (((WorldRendererMixin)this).getClient().options.viewDistance >= 4) {
			profiler.swap("sky");
			this.renderSky(matrices, tickDelta);
		}

		profiler.swap("fog");
		BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_TERRAIN, Math.max(g - 16.0F, 32.0F), bl2);
		profiler.swap("terrain_setup");
		this.setupTerrain(camera, frustum2, bl, this.frame++, Main.player.isSpectator());
		profiler.swap("updatechunks");
		int j = ((WorldRendererMixin)this).getClient().options.maxFps;
		long n;
		if ((double)j == Option.FRAMERATE_LIMIT.getMax()) {
			n = 0L;
		} else {
			n = 1000000000 / j;
		}

		long o = Util.getMeasuringTimeNano() - limitTime;
		long p = ((WorldRendererMixin)this).getChunkUpdateSmoother().getTargetUsedTime(o);
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
		if (((WorldRendererMixin)this).getEntityFramebuffer() != null) {
			((WorldRendererMixin) this).getEntityFramebuffer().clear(MinecraftClient.IS_SYSTEM_MAC);
			((WorldRendererMixin) this).getEntityFramebuffer().copyDepthFrom(((WorldRendererMixin) this).getClient().getFramebuffer());
			((WorldRendererMixin) this).getClient().getFramebuffer().beginWrite(false);
		}
		if (((WorldRendererMixin)this).getWeatherFramebuffer() != null) {
			((WorldRendererMixin)this).getWeatherFramebuffer().clear(MinecraftClient.IS_SYSTEM_MAC);
		}
		VertexConsumerProvider.Immediate immediate = ((WorldRendererMixin)this).getBufferBuilders().getEntityVertexConsumers();
		Iterator<Entity> var39 = this.world.getEntities().iterator();
		while(true) {
			Entity entity;
			do {
				do {
					do {
						if (!var39.hasNext()) {
							((WorldRendererMixin)this).callCheckEmpty(matrices);
							immediate.draw(RenderLayer.getEntitySolid(SpriteAtlasTexture.BLOCK_ATLAS_TEX));
							immediate.draw(RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEX));
							immediate.draw(RenderLayer.getEntityCutoutNoCull(SpriteAtlasTexture.BLOCK_ATLAS_TEX));
							immediate.draw(RenderLayer.getEntitySmoothCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEX));
							profiler.swap("blockentities");
							ObjectListIterator<ChunkInfo> var53 = this.visibleChunks.iterator();

							while(true) {
								List<BlockEntity> list;
								do {
									if (!var53.hasNext()) {
										synchronized(((WorldRendererMixin)this).getNoCullingBlockEntities()) {
											Iterator<BlockEntity> var57 = ((WorldRendererMixin)this).getNoCullingBlockEntities().iterator();

											while(true) {
												if (!var57.hasNext()) {
													break;
												}

												BlockEntity blockEntity2 = var57.next();
												BlockPos blockPos2 = blockEntity2.getPos();
												matrices.push();
												matrices.translate((double)blockPos2.getX() - d, (double)blockPos2.getY() - e, (double)blockPos2.getZ() - f);
												BlockEntityRenderDispatcher.INSTANCE.render(blockEntity2, tickDelta, matrices, immediate);
												matrices.pop();
											}
										}

										((WorldRendererMixin)this).callCheckEmpty(matrices);
										immediate.draw(RenderLayer.getSolid());
										immediate.draw(TexturedRenderLayers.getEntitySolid());
										immediate.draw(TexturedRenderLayers.getEntityCutout());
										immediate.draw(TexturedRenderLayers.getBeds());
										immediate.draw(TexturedRenderLayers.getShulkerBoxes());
										immediate.draw(TexturedRenderLayers.getSign());
										immediate.draw(TexturedRenderLayers.getChest());
										((WorldRendererMixin)this).getBufferBuilders().getOutlineVertexConsumers().draw();



										HitResult hitResult = ((WorldRendererMixin)this).getClient().crosshairTarget;
										if (renderBlockOutline && hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
											profiler.swap("outline");
											BlockPos blockPos4 = ((BlockHitResult)hitResult).getBlockPos();
											BlockState blockState = this.world.getBlockState(blockPos4);
											if (!blockState.isAir() && this.world.getWorldBorder().contains(blockPos4)) {
												VertexConsumer vertexConsumer3 = immediate.getBuffer(RenderLayer.getLines());
												((WorldRendererMixin)this).callDrawBlockOutline(matrices, vertexConsumer3, camera.getFocusedEntity(), d, e, f, blockPos4, blockState);
											}
										}

										immediate.draw(TexturedRenderLayers.getEntityTranslucentCull());
										immediate.draw(TexturedRenderLayers.getBannerPatterns());
										immediate.draw(TexturedRenderLayers.getShieldPatterns());
										immediate.draw(RenderLayer.getArmorGlint());
										immediate.draw(RenderLayer.getArmorEntityGlint());
										immediate.draw(RenderLayer.getGlint());
										immediate.draw(RenderLayer.getEntityGlint());
										immediate.draw(RenderLayer.getWaterMask());
										((WorldRendererMixin)this).getBufferBuilders().getEffectVertexConsumers().draw();
										immediate.draw(RenderLayer.getLines());
										immediate.draw();
										if (((WorldRendererMixin)this).getTransparencyShader() != null) {
											((WorldRendererMixin)this).getTranslucentFramebuffer().clear(MinecraftClient.IS_SYSTEM_MAC);
											((WorldRendererMixin)this).getTranslucentFramebuffer().copyDepthFrom(((WorldRendererMixin)this).getClient().getFramebuffer());
											profiler.swap("translucent");
											this.renderLayer(RenderLayer.getTranslucent(), matrices, d, e, f);
											profiler.swap("string");
											this.renderLayer(RenderLayer.getTripwire(), matrices, d, e, f);
											((WorldRendererMixin)this).getParticlesFramebuffer().clear(MinecraftClient.IS_SYSTEM_MAC);
											((WorldRendererMixin)this).getParticlesFramebuffer().copyDepthFrom(((WorldRendererMixin)this).getClient().getFramebuffer());
											RenderPhaseMixin.getPARTICLES_TARGET().startDrawing();
											profiler.swap("particles");
											((WorldRendererMixin)this).getClient().particleManager.renderParticles(matrices, immediate, lightmapTextureManager, camera, tickDelta);
											RenderPhaseMixin.getPARTICLES_TARGET().endDrawing();
										} else {
											profiler.swap("translucent");
											this.renderLayer(RenderLayer.getTranslucent(), matrices, d, e, f);
											profiler.swap("string");
											this.renderLayer(RenderLayer.getTripwire(), matrices, d, e, f);
											profiler.swap("particles");
											((WorldRendererMixin)this).getClient().particleManager.renderParticles(matrices, immediate, lightmapTextureManager, camera, tickDelta);
										}

										RenderSystem.pushMatrix();
										RenderSystem.multMatrix(matrices.peek().getModel());
										if (((WorldRendererMixin)this).getClient().options.getCloudRenderMode() != CloudRenderMode.OFF) {
											if (((WorldRendererMixin)this).getTransparencyShader() != null) {
												((WorldRendererMixin)this).getCloudsFramebuffer().clear(MinecraftClient.IS_SYSTEM_MAC);
												RenderPhaseMixin.getCLOUDS_TARGET().startDrawing();
												profiler.swap("clouds");
												this.renderClouds(matrices, tickDelta, d, e, f);
												RenderPhaseMixin.getCLOUDS_TARGET().endDrawing();
											} else {
												profiler.swap("clouds");
												this.renderClouds(matrices, tickDelta, d, e, f);
											}
										}

										if (((WorldRendererMixin)this).getTransparencyShader() != null) {
											RenderPhaseMixin.getWEATHER_TARGET().startDrawing();
											profiler.swap("weather");
											this.renderWeather(lightmapTextureManager, tickDelta, d, e, f);
											RenderPhaseMixin.getWEATHER_TARGET().endDrawing();
											((WorldRendererMixin)this).getTransparencyShader().render(tickDelta);
											((WorldRendererMixin)this).getClient().getFramebuffer().beginWrite(false);
										} else {
											RenderSystem.depthMask(false);
											profiler.swap("weather");
											this.renderWeather(lightmapTextureManager, tickDelta, d, e, f);
											RenderSystem.depthMask(true);
										}

										this.renderChunkDebugInfo(camera);
										RenderSystem.shadeModel(7424);
										RenderSystem.depthMask(true);
										RenderSystem.disableBlend();
										RenderSystem.popMatrix();
										BackgroundRenderer.method_23792();
										return;
									}

									PreviewRenderer.ChunkInfo chunkInfo = var53.next();
									list = chunkInfo.chunk.getData().getBlockEntities();
								} while(list.isEmpty());

							}
						}

						entity = var39.next();
					} while(!((WorldRendererMixin)this).getEntityRenderDispatcher().shouldRender(entity, frustum2, d, e, f) && !entity.hasPassengerDeep(Main.player));
				} while(entity == camera.getFocusedEntity() && !camera.isThirdPerson() && (!(camera.getFocusedEntity() instanceof LivingEntity) || !((LivingEntity)camera.getFocusedEntity()).isSleeping()));
			} while(entity instanceof ClientPlayerEntity && camera.getFocusedEntity() != entity);

			++this.regularEntityCount;
			if (entity.age == 0) {
				entity.lastRenderX = entity.getX();
				entity.lastRenderY = entity.getY();
				entity.lastRenderZ = entity.getZ();
			}

			this.renderEntity(entity, d, e, f, tickDelta, matrices, immediate);
		}
	}

	private void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
		double d = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
		double e = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
		double f = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());
		float g = MathHelper.lerp(tickDelta, entity.prevYaw, entity.yaw);
		((WorldRendererMixin)this).getEntityRenderDispatcher().render(entity, d - cameraX, e - cameraY, f - cameraZ, g, tickDelta, matrices, vertexConsumers, ((WorldRendererMixin)this).getEntityRenderDispatcher().getLight(entity, tickDelta));
	}
	public void scheduleBlockRenders(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		for(int i = minZ - 1; i <= maxZ + 1; ++i) {
			for(int j = minX - 1; j <= maxX + 1; ++j) {
				for(int k = minY - 1; k <= maxY + 1; ++k) {
					this.scheduleBlockRender(j >> 4, k >> 4, i >> 4);
				}
			}
		}

	}

	public void scheduleBlockRender(int x, int y, int z) {
		this.scheduleChunkRender(x, y, z, false);
	}

	private void scheduleChunkRender(int x, int y, int z, boolean important) {
		this.chunks.scheduleRebuild(x, y, z, important);
	}

	public void scheduleBlockRenders(int x, int y, int z) {
		for(int i = z - 1; i <= z + 1; ++i) {
			for(int j = x - 1; j <= x + 1; ++j) {
				for(int k = y - 1; k <= y + 1; ++k) {
					this.scheduleBlockRender(j, k, i);
				}
			}
		}

	}

	private void renderLayer(RenderLayer renderLayer, MatrixStack matrixStack, double d, double e, double f) {
		renderLayer.startDrawing();
		if (renderLayer == RenderLayer.getTranslucent()) {
			((WorldRendererMixin)this).getClient().getProfiler().push("translucent_sort");
			double g = d - this.lastTranslucentSortX;
			double h = e - this.lastTranslucentSortY;
			double i = f - this.lastTranslucentSortZ;
			if (g * g + h * h + i * i > 1.0D) {
				this.lastTranslucentSortX = d;
				this.lastTranslucentSortY = e;
				this.lastTranslucentSortZ = f;
				int j = 0;
				ObjectListIterator<ChunkInfo> var16 = this.visibleChunks.iterator();

				while(var16.hasNext()) {
					PreviewRenderer.ChunkInfo chunkInfo = var16.next();
					if (j < 15 && chunkInfo.chunk.scheduleSort(renderLayer, this.chunkBuilder)) {
						++j;
					}
				}
			}
			((WorldRendererMixin)this).getClient().getProfiler().pop();
		}
		((WorldRendererMixin)this).getClient().getProfiler().push("filterempty");
		((WorldRendererMixin)this).getClient().getProfiler().swap(() -> "render_" + renderLayer);
		boolean bl = renderLayer != RenderLayer.getTranslucent();
		ObjectListIterator<ChunkInfo> objectListIterator = this.visibleChunks.listIterator(bl ? 0 : this.visibleChunks.size());
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
		((WorldRendererMixin)this).getClient().getProfiler().pop();
		renderLayer.endDrawing();
	}

	private void renderChunkDebugInfo(Camera camera) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		if (((WorldRendererMixin)this).getClient().debugChunkInfo || ((WorldRendererMixin)this).getClient().debugChunkOcclusion) {
			double d = camera.getPos().getX();
			double e = camera.getPos().getY();
			double f = camera.getPos().getZ();
			RenderSystem.depthMask(true);
			RenderSystem.disableCull();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableTexture();
			for(ObjectListIterator<ChunkInfo> var10 = this.visibleChunks.iterator(); var10.hasNext(); RenderSystem.popMatrix()) {
				PreviewRenderer.ChunkInfo chunkInfo = var10.next();
				ChunkBuilder.BuiltChunk builtChunk = chunkInfo.chunk;
				RenderSystem.pushMatrix();
				BlockPos blockPos = builtChunk.getOrigin();
				RenderSystem.translated((double)blockPos.getX() - d, (double)blockPos.getY() - e, (double)blockPos.getZ() - f);
				int m;
				int k;
				int l;
				Direction direction2;
				if (((WorldRendererMixin)this).getClient().debugChunkInfo) {
					bufferBuilder.begin(1, VertexFormats.POSITION_COLOR);
					RenderSystem.lineWidth(10.0F);
					m = chunkInfo.propagationLevel == 0 ? 0 : MathHelper.hsvToRgb((float)chunkInfo.propagationLevel / 50.0F, 0.9F, 0.9F);
					int j = m >> 16 & 255;
					k = m >> 8 & 255;
					l = m & 255;
					direction2 = chunkInfo.direction;
					if (direction2 != null) {
						bufferBuilder.vertex(8.0D, 8.0D, 8.0D).color(j, k, l, 255).next();
						bufferBuilder.vertex(8 - 16 * direction2.getOffsetX(), 8 - 16 * direction2.getOffsetY(), 8 - 16 * direction2.getOffsetZ()).color(j, k, l, 255).next();
					}

					tessellator.draw();
					RenderSystem.lineWidth(1.0F);
				}
				if (((WorldRendererMixin)this).getClient().debugChunkOcclusion && !builtChunk.getData().isEmpty()) {
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
								bufferBuilder.vertex(8 + 8 * direction2.getOffsetX(), 8 + 8 * direction2.getOffsetY(), 8 + 8 * direction2.getOffsetZ()).color(1, 0, 0, 1).next();
								bufferBuilder.vertex(8 + 8 * direction3.getOffsetX(), 8 + 8 * direction3.getOffsetY(), 8 + 8 * direction3.getOffsetZ()).color(1, 0, 0, 1).next();
							}
						}
					}
					tessellator.draw();
					RenderSystem.lineWidth(1.0F);
					if (m > 0) {
						bufferBuilder.begin(7, VertexFormats.POSITION_COLOR);
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
		if (((WorldRendererMixin)this).getCapturedFrustum() != null) {
			RenderSystem.disableCull();
			RenderSystem.disableTexture();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.lineWidth(10.0F);
			RenderSystem.pushMatrix();
			RenderSystem.translatef((float)(((WorldRendererMixin)this).getCapturedFrustumPosition().x - camera.getPos().x), (float)(((WorldRendererMixin)this).getCapturedFrustumPosition().y - camera.getPos().y), (float)(((WorldRendererMixin)this).getCapturedFrustumPosition().z - camera.getPos().z));
			RenderSystem.depthMask(true);
			bufferBuilder.begin(7, VertexFormats.POSITION_COLOR);
			((WorldRendererMixin)this).callMethod_22985(bufferBuilder, 0, 1, 2, 3, 0, 1, 1);
			((WorldRendererMixin)this).callMethod_22985(bufferBuilder, 4, 5, 6, 7, 1, 0, 0);
			((WorldRendererMixin)this).callMethod_22985(bufferBuilder, 0, 1, 5, 4, 1, 1, 0);
			((WorldRendererMixin)this).callMethod_22985(bufferBuilder, 2, 3, 7, 6, 0, 0, 1);
			((WorldRendererMixin)this).callMethod_22985(bufferBuilder, 0, 4, 7, 3, 0, 1, 0);
			((WorldRendererMixin)this).callMethod_22985(bufferBuilder, 1, 5, 6, 2, 1, 0, 1);
			tessellator.draw();
			RenderSystem.depthMask(false);
			bufferBuilder.begin(1, VertexFormats.POSITION);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 0);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 1);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 1);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 2);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 2);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 3);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 3);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 0);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 4);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 5);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 5);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 6);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 6);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 7);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 7);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 4);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 0);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 4);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 1);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 5);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 2);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 6);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 3);
			((WorldRendererMixin)this).callMethod_22984(bufferBuilder, 7);
			tessellator.draw();
			RenderSystem.popMatrix();
			RenderSystem.depthMask(true);
			RenderSystem.disableBlend();
			RenderSystem.enableCull();
			RenderSystem.enableTexture();
			RenderSystem.lineWidth(1.0F);
		}
	}
	
	public void renderSky(MatrixStack matrices, float tickDelta) {
		if (this.world.getSkyProperties().getSkyType() == SkyProperties.SkyType.NORMAL) {
			RenderSystem.disableTexture();
			Vec3d vec3d = this.world.method_23777(Main.camera.getBlockPos(), tickDelta);
			float f = (float)vec3d.x;
			float g = (float)vec3d.y;
			float h = (float)vec3d.z;
			BackgroundRenderer.setFogBlack();
			BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
			RenderSystem.depthMask(false);
			RenderSystem.color3f(f, g, h);
			this.lightSkyBuffer.bind();
			((WorldRendererMixin)this).getSkyVertexFormat().startDrawing(0L);
			this.lightSkyBuffer.draw(matrices.peek().getModel(), 7);
			VertexBuffer.unbind();
			((WorldRendererMixin)this).getSkyVertexFormat().endDrawing();
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
			((WorldRendererMixin)this).getTextureManager().bindTexture(SUN);
			bufferBuilder.begin(7, VertexFormats.POSITION_TEXTURE);
			bufferBuilder.vertex(matrix4f2, -s, 100.0F, -s).texture(0.0F, 0.0F).next();
			bufferBuilder.vertex(matrix4f2, s, 100.0F, -s).texture(1.0F, 0.0F).next();
			bufferBuilder.vertex(matrix4f2, s, 100.0F, s).texture(1.0F, 1.0F).next();
			bufferBuilder.vertex(matrix4f2, -s, 100.0F, s).texture(0.0F, 1.0F).next();
			bufferBuilder.end();
			BufferRenderer.draw(bufferBuilder);
			s = 20.0F;
			((WorldRendererMixin)this).getTextureManager().bindTexture(MOON_PHASES);
			int t = this.world.getMoonPhase();
			int u = t % 4;
			int v = t / 4 % 2;
			float w = (float)(u) / 4.0F;
			o = (float)(v) / 2.0F;
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
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.disableBlend();
			RenderSystem.enableAlphaTest();
			matrices.pop();
			RenderSystem.disableTexture();
			RenderSystem.color3f(0.0F, 0.0F, 0.0F);
			if (this.world.getSkyProperties().isAlternateSkyColor()) {
				RenderSystem.color3f(f * 0.2F + 0.04F, g * 0.2F + 0.04F, h * 0.6F + 0.1F);
			} else {
				RenderSystem.color3f(f, g, h);
			}
			RenderSystem.enableTexture();
			RenderSystem.depthMask(true);
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
			RenderSystem.enableFog();
			RenderSystem.depthMask(true);
			double e = ((float)this.ticks + tickDelta) * 0.03F;
			double i = (cameraX + e) / 12.0D;
			double j = f - (float)cameraY + 0.33F;
			double k = cameraZ / 12.0D + 0.33000001311302185D;
			i -= MathHelper.floor(i / 2048.0D) * 2048;
			k -= MathHelper.floor(k / 2048.0D) * 2048;
			float l = (float)(i - (double)MathHelper.floor(i));
			float m = (float)(j / 4.0D - (double)MathHelper.floor(j / 4.0D)) * 4.0F;
			float n = (float)(k - (double)MathHelper.floor(k));
			Vec3d vec3d = this.world.getCloudsColor(tickDelta);
			int o = (int)Math.floor(i);
			int p = (int)Math.floor(j / 4.0D);
			int q = (int)Math.floor(k);
			if (o != this.lastCloudsBlockX || p != this.lastCloudsBlockY || q != this.lastCloudsBlockZ || ((WorldRendererMixin)this).getClient().options.getCloudRenderMode() != this.lastCloudsRenderMode || this.lastCloudsColor.squaredDistanceTo(vec3d) > 2.0E-4D) {
				this.lastCloudsBlockX = o;
				this.lastCloudsBlockY = p;
				this.lastCloudsBlockZ = q;
				this.lastCloudsColor = vec3d;
				this.lastCloudsRenderMode = ((WorldRendererMixin)this).getClient().options.getCloudRenderMode();
				this.cloudsDirty = true;
			}
			if (this.cloudsDirty) {
				this.cloudsDirty = false;
				BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
				if (this.cloudsBuffer != null) {
					this.cloudsBuffer.close();
				}
				this.cloudsBuffer = new VertexBuffer(VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);
				((WorldRendererMixin)this).callRenderClouds(bufferBuilder, i, j, k, vec3d);
				bufferBuilder.end();
				this.cloudsBuffer.upload(bufferBuilder);
			}
			((WorldRendererMixin)this).getTextureManager().bindTexture(CLOUDS);
			matrices.push();
			matrices.scale(12.0F, 1.0F, 12.0F);
			matrices.translate(-l, m, -n);
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
			RenderSystem.disableFog();
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
