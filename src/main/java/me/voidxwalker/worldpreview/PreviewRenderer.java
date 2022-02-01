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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import me.voidxwalker.worldpreview.mixin.access.BuiltChunkStorageMixin;
import me.voidxwalker.worldpreview.mixin.access.RenderPhaseMixin;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkRendererRegionBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.*;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Environment(EnvType.CLIENT)
public class PreviewRenderer implements SynchronousResourceReloader, AutoCloseable {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final double field_34814 = Math.ceil(Math.sqrt(3.0D) * 16.0D);
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
	@Nullable
	public ClientWorld world;
	private final BlockingQueue<ChunkBuilder.BuiltChunk> builtChunks = new LinkedBlockingQueue();
	private final AtomicReference<PreviewRenderer.class_6600> field_34817 = new AtomicReference();
	private final ObjectArrayList<PreviewRenderer.ChunkInfo> chunkInfos = new ObjectArrayList(10000);
	private final Set<BlockEntity> noCullingBlockEntities = Sets.newHashSet();
	@Nullable
	private Future<?> field_34808;
	@Nullable
	private BuiltChunkStorage chunks;
	@Nullable
	private VertexBuffer starsBuffer;
	@Nullable
	private VertexBuffer lightSkyBuffer;
	@Nullable
	private VertexBuffer darkSkyBuffer;
	private boolean cloudsDirty = true;
	@Nullable
	private VertexBuffer cloudsBuffer;
	public int ticks;
	private final Int2ObjectMap<BlockBreakingInfo> blockBreakingInfos = new Int2ObjectOpenHashMap();
	private final Long2ObjectMap<SortedSet<BlockBreakingInfo>> blockBreakingProgressions = new Long2ObjectOpenHashMap();
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
	@Nullable
	private CloudRenderMode lastCloudsRenderMode;
	@Nullable
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
	private boolean field_34810;
	private final AtomicLong field_34811;
	private final AtomicBoolean field_34809;
	private int field_20793;
	private final float[] field_20794;
	private final float[] field_20795;

	public PreviewRenderer(MinecraftClient client, BufferBuilderStorage bufferBuilders) {
		this.lastCloudsColor = Vec3d.ZERO;
		this.viewDistance = -1;
		this.capturedFrustumOrientation = new Vector4f[8];
		this.capturedFrustumPosition = new Vector3d(0.0D, 0.0D, 0.0D);
		this.field_34810 = true;
		this.field_34811 = new AtomicLong(0L);
		this.field_34809 = new AtomicBoolean(false);
		this.field_20794 = new float[1024];
		this.field_20795 = new float[1024];
		this.client = client;
		this.entityRenderDispatcher = client.getEntityRenderDispatcher();
		this.blockEntityRenderDispatcher = client.getBlockEntityRenderDispatcher();
		this.bufferBuilders = bufferBuilders;

		for(int $$2 = 0; $$2 < 32; ++$$2) {
			for(int $$3 = 0; $$3 < 32; ++$$3) {
				float $$4 = (float)($$3 - 16);
				float $$5 = (float)($$2 - 16);
				float $$6 = MathHelper.sqrt($$4 * $$4 + $$5 * $$5);
				this.field_20794[$$2 << 5 | $$3] = -$$5 / $$6;
				this.field_20795[$$2 << 5 | $$3] = $$4 / $$6;
			}
		}

		this.renderStars();
		this.renderLightSky();
		this.renderDarkSky();
	}
	public void setupFrustum(MatrixStack matrices, Vec3d pos, Matrix4f projectionMatrix) {
		Matrix4f matrix4f = matrices.peek().getPositionMatrix();
		double d = pos.getX();
		double e = pos.getY();
		double f = pos.getZ();
		this.frustum = new Frustum(matrix4f, projectionMatrix);
		this.frustum.setPosition(d, e, f);
	}
	private void renderWeather(LightmapTextureManager manager, float $$1, double $$2, double $$3, double $$4) {
		float $$5 = WorldPreview.clientWorld.getRainGradient($$1);
		if (!($$5 <= 0.0F)) {
			manager.enable();
			World $$6 = WorldPreview.clientWorld;
			int $$7 = MathHelper.floor($$2);
			int $$8 = MathHelper.floor($$3);
			int $$9 = MathHelper.floor($$4);
			Tessellator $$10 = Tessellator.getInstance();
			BufferBuilder $$11 = $$10.getBuffer();
			RenderSystem.disableCull();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.enableDepthTest();
			int $$12 = 5;


			RenderSystem.depthMask(false);
			int $$13 = -1;
			float $$14 = (float)this.ticks + $$1;
			RenderSystem.setShader(GameRenderer::getParticleShader);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			BlockPos.Mutable $$15 = new BlockPos.Mutable();

			for(int $$16 = $$9 - $$12; $$16 <= $$9 + $$12; ++$$16) {
				for(int $$17 = $$7 - $$12; $$17 <= $$7 + $$12; ++$$17) {
					int $$18 = ($$16 - $$9 + 16) * 32 + $$17 - $$7 + 16;
					double $$19 = (double)this.field_20794[$$18] * 0.5D;
					double $$20 = (double)this.field_20795[$$18] * 0.5D;
					$$15.set((double)$$17, $$3, (double)$$16);
					Biome $$21 = $$6.getBiome($$15);
					if ($$21.getPrecipitation() != Biome.Precipitation.NONE) {
						int $$22 = $$6.getTopY(Heightmap.Type.MOTION_BLOCKING, $$17, $$16);
						int $$23 = $$8 - $$12;
						int $$24 = $$8 + $$12;
						if ($$23 < $$22) {
							$$23 = $$22;
						}

						if ($$24 < $$22) {
							$$24 = $$22;
						}

						int $$25 = $$22;
						if ($$22 < $$8) {
							$$25 = $$8;
						}

						if ($$23 != $$24) {
							Random $$26 = new Random((long)($$17 * $$17 * 3121 + $$17 * 45238971 ^ $$16 * $$16 * 418711 + $$16 * 13761));
							$$15.set($$17, $$23, $$16);
							float $$28;
							float $$32;
							if ($$21.doesNotSnow($$15)) {
								if ($$13 != 0) {
									if ($$13 >= 0) {
										$$10.draw();
									}

									$$13 = 0;
									RenderSystem.setShaderTexture(0, RAIN);
									$$11.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
								}

								int $$27 = this.ticks + $$17 * $$17 * 3121 + $$17 * 45238971 + $$16 * $$16 * 418711 + $$16 * 13761 & 31;
								$$28 = -((float)$$27 + $$1) / 32.0F * (3.0F + $$26.nextFloat());
								double $$29 = (double)$$17 + 0.5D - $$2;
								double $$30 = (double)$$16 + 0.5D - $$4;
								float $$31 = (float)Math.sqrt($$29 * $$29 + $$30 * $$30) / (float)$$12;
								$$32 = ((1.0F - $$31 * $$31) * 0.5F + 0.5F) * $$5;
								$$15.set($$17, $$25, $$16);
								int $$33 = getLightmapCoordinates($$6, $$15);
								$$11.vertex((double)$$17 - $$2 - $$19 + 0.5D, (double)$$24 - $$3, (double)$$16 - $$4 - $$20 + 0.5D).texture(0.0F, (float)$$23 * 0.25F + $$28).color(1.0F, 1.0F, 1.0F, $$32).light($$33).next();
								$$11.vertex((double)$$17 - $$2 + $$19 + 0.5D, (double)$$24 - $$3, (double)$$16 - $$4 + $$20 + 0.5D).texture(1.0F, (float)$$23 * 0.25F + $$28).color(1.0F, 1.0F, 1.0F, $$32).light($$33).next();
								$$11.vertex((double)$$17 - $$2 + $$19 + 0.5D, (double)$$23 - $$3, (double)$$16 - $$4 + $$20 + 0.5D).texture(1.0F, (float)$$24 * 0.25F + $$28).color(1.0F, 1.0F, 1.0F, $$32).light($$33).next();
								$$11.vertex((double)$$17 - $$2 - $$19 + 0.5D, (double)$$23 - $$3, (double)$$16 - $$4 - $$20 + 0.5D).texture(0.0F, (float)$$24 * 0.25F + $$28).color(1.0F, 1.0F, 1.0F, $$32).light($$33).next();
							} else {
								if ($$13 != 1) {
									if ($$13 >= 0) {
										$$10.draw();
									}

									$$13 = 1;
									RenderSystem.setShaderTexture(0, SNOW);
									$$11.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
								}

								float $$34 = -((float)(this.ticks & 511) + $$1) / 512.0F;
								$$28 = (float)($$26.nextDouble() + (double)$$14 * 0.01D * (double)((float)$$26.nextGaussian()));
								float $$36 = (float)($$26.nextDouble() + (double)($$14 * (float)$$26.nextGaussian()) * 0.001D);
								double $$37 = (double)$$17 + 0.5D - $$2;
								double $$38 = (double)$$16 + 0.5D - $$4;
								$$32 = (float)Math.sqrt($$37 * $$37 + $$38 * $$38) / (float)$$12;
								float $$40 = ((1.0F - $$32 * $$32) * 0.3F + 0.5F) * $$5;
								$$15.set($$17, $$25, $$16);
								int $$41 = getLightmapCoordinates($$6, $$15);
								int $$42 = $$41 >> 16 & '\uffff';
								int $$43 = $$41 & '\uffff';
								int $$44 = ($$42 * 3 + 240) / 4;
								int $$45 = ($$43 * 3 + 240) / 4;
								$$11.vertex((double)$$17 - $$2 - $$19 + 0.5D, (double)$$24 - $$3, (double)$$16 - $$4 - $$20 + 0.5D).texture(0.0F + $$28, (float)$$23 * 0.25F + $$34 + $$36).color(1.0F, 1.0F, 1.0F, $$40).light($$45, $$44).next();
								$$11.vertex((double)$$17 - $$2 + $$19 + 0.5D, (double)$$24 - $$3, (double)$$16 - $$4 + $$20 + 0.5D).texture(1.0F + $$28, (float)$$23 * 0.25F + $$34 + $$36).color(1.0F, 1.0F, 1.0F, $$40).light($$45, $$44).next();
								$$11.vertex((double)$$17 - $$2 + $$19 + 0.5D, (double)$$23 - $$3, (double)$$16 - $$4 + $$20 + 0.5D).texture(1.0F + $$28, (float)$$24 * 0.25F + $$34 + $$36).color(1.0F, 1.0F, 1.0F, $$40).light($$45, $$44).next();
								$$11.vertex((double)$$17 - $$2 - $$19 + 0.5D, (double)$$23 - $$3, (double)$$16 - $$4 - $$20 + 0.5D).texture(0.0F + $$28, (float)$$24 * 0.25F + $$34 + $$36).color(1.0F, 1.0F, 1.0F, $$40).light($$45, $$44).next();
							}
						}
					}
				}
			}

			if ($$13 >= 0) {
				$$10.draw();
			}

			RenderSystem.enableCull();
			RenderSystem.disableBlend();
			manager.disable();
		}
	}


	public void close() {
		if (this.entityOutlineShader != null) {
			this.entityOutlineShader.close();
		}

		if (this.transparencyShader != null) {
			this.transparencyShader.close();
		}

	}

	public void reload(ResourceManager manager) {
		this.loadEntityOutlineShader();


	}

	public void loadEntityOutlineShader() {
		if (this.entityOutlineShader != null) {
			this.entityOutlineShader.close();
		}

		Identifier $$0 = new Identifier("shaders/post/entity_outline.json");

		try {
			this.entityOutlineShader = new ShaderEffect(this.client.getTextureManager(), this.client.getResourceManager(), this.client.getFramebuffer(), $$0);
			this.entityOutlineShader.setupDimensions(this.client.getWindow().getFramebufferWidth(), this.client.getWindow().getFramebufferHeight());
			this.entityOutlinesFramebuffer = this.entityOutlineShader.getSecondaryTarget("final");
		} catch (IOException var3) {
			LOGGER.warn((String)"Failed to load shader: {}", (Object)$$0, (Object)var3);
			this.entityOutlineShader = null;
			this.entityOutlinesFramebuffer = null;
		} catch (JsonSyntaxException var4) {
			LOGGER.warn((String)"Failed to parse shader: {}", (Object)$$0, (Object)var4);
			this.entityOutlineShader = null;
			this.entityOutlinesFramebuffer = null;
		}

	}

	private void loadTransparencyShader() {
		this.resetTransparencyShader();
		Identifier $$0 = new Identifier("shaders/post/transparency.json");

		try {
			ShaderEffect $$1 = new ShaderEffect(this.client.getTextureManager(), this.client.getResourceManager(), this.client.getFramebuffer(), $$0);
			$$1.setupDimensions(this.client.getWindow().getFramebufferWidth(), this.client.getWindow().getFramebufferHeight());
			Framebuffer $$2 = $$1.getSecondaryTarget("translucent");
			Framebuffer $$3 = $$1.getSecondaryTarget("itemEntity");
			Framebuffer $$4 = $$1.getSecondaryTarget("particles");
			Framebuffer $$5 = $$1.getSecondaryTarget("weather");
			Framebuffer $$6 = $$1.getSecondaryTarget("clouds");
			this.transparencyShader = $$1;
			this.translucentFramebuffer = $$2;
			this.entityFramebuffer = $$3;
			this.particlesFramebuffer = $$4;
			this.weatherFramebuffer = $$5;
			this.cloudsFramebuffer = $$6;
		} catch (Exception var9) {
			String $$8 = var9 instanceof JsonSyntaxException ? "parse" : "load";
			String $$9 = "Failed to " + $$8 + " shader: " + $$0;
			PreviewRenderer.ShaderException $$10 = new PreviewRenderer.ShaderException($$9, var9);
			if (this.client.getResourcePackManager().getEnabledNames().size() > 1) {
				LiteralText $$13;
				try {
					$$13 = new LiteralText(this.client.getResourceManager().getResource($$0).getResourcePackName());
				} catch (IOException var8) {
					$$13 = null;
				}

				this.client.options.graphicsMode = GraphicsMode.FANCY;
				this.client.onResourceReloadFailure($$10, $$13);
			} else {
				CrashReport $$14 = this.client.addDetailsToCrashReport(new CrashReport($$9, $$10));
				this.client.options.graphicsMode = GraphicsMode.FANCY;
				this.client.options.write();
				LOGGER.fatal((String)$$9, (Throwable)$$10);
				this.client.cleanUpAfterCrash();
				MinecraftClient.printCrashReport($$14);
			}
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
		Tessellator $$0 = Tessellator.getInstance();
		BufferBuilder $$1 = $$0.getBuffer();
		if (this.darkSkyBuffer != null) {
			this.darkSkyBuffer.close();
		}

		this.darkSkyBuffer = new VertexBuffer();
		renderSky($$1, -16.0F);
		this.darkSkyBuffer.upload($$1);
	}

	private void renderLightSky() {
		Tessellator $$0 = Tessellator.getInstance();
		BufferBuilder $$1 = $$0.getBuffer();
		if (this.lightSkyBuffer != null) {
			this.lightSkyBuffer.close();
		}

		this.lightSkyBuffer = new VertexBuffer();
		renderSky($$1, 16.0F);
		this.lightSkyBuffer.upload($$1);
	}

	private static void renderSky(BufferBuilder builder, float $$1) {
		float $$2 = Math.signum($$1) * 512.0F;
		float $$3 = 512.0F;
		RenderSystem.setShader(GameRenderer::getPositionShader);
		builder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION);
		builder.vertex(0.0D, (double)$$1, 0.0D).next();

		for(int $$4 = -180; $$4 <= 180; $$4 += 45) {
			builder.vertex((double)($$2 * MathHelper.cos((float)$$4 * 0.017453292F)), (double)$$1, (double)(512.0F * MathHelper.sin((float)$$4 * 0.017453292F))).next();
		}

		builder.end();
	}

	private void renderStars() {
		Tessellator $$0 = Tessellator.getInstance();
		BufferBuilder $$1 = $$0.getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionShader);
		if (this.starsBuffer != null) {
			this.starsBuffer.close();
		}

		this.starsBuffer = new VertexBuffer();
		this.renderStars($$1);
		$$1.end();
		this.starsBuffer.upload($$1);
	}

	private void renderStars(BufferBuilder buffer) {
		Random $$1 = new Random(10842L);
		buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

		for(int $$2 = 0; $$2 < 1500; ++$$2) {
			double $$3 = (double)($$1.nextFloat() * 2.0F - 1.0F);
			double $$4 = (double)($$1.nextFloat() * 2.0F - 1.0F);
			double $$5 = (double)($$1.nextFloat() * 2.0F - 1.0F);
			double $$6 = (double)(0.15F + $$1.nextFloat() * 0.1F);
			double $$7 = $$3 * $$3 + $$4 * $$4 + $$5 * $$5;
			if ($$7 < 1.0D && $$7 > 0.01D) {
				$$7 = 1.0D / Math.sqrt($$7);
				$$3 *= $$7;
				$$4 *= $$7;
				$$5 *= $$7;
				double $$8 = $$3 * 100.0D;
				double $$9 = $$4 * 100.0D;
				double $$10 = $$5 * 100.0D;
				double $$11 = Math.atan2($$3, $$5);
				double $$12 = Math.sin($$11);
				double $$13 = Math.cos($$11);
				double $$14 = Math.atan2(Math.sqrt($$3 * $$3 + $$5 * $$5), $$4);
				double $$15 = Math.sin($$14);
				double $$16 = Math.cos($$14);
				double $$17 = $$1.nextDouble() * 3.141592653589793D * 2.0D;
				double $$18 = Math.sin($$17);
				double $$19 = Math.cos($$17);

				for(int $$20 = 0; $$20 < 4; ++$$20) {
					double $$21 = 0.0D;
					double $$22 = (double)(($$20 & 2) - 1) * $$6;
					double $$23 = (double)(($$20 + 1 & 2) - 1) * $$6;
					double $$24 = 0.0D;
					double $$25 = $$22 * $$19 - $$23 * $$18;
					double $$26 = $$23 * $$19 + $$22 * $$18;
					double $$28 = $$25 * $$15 + 0.0D * $$16;
					double $$29 = 0.0D * $$15 - $$25 * $$16;
					double $$30 = $$29 * $$12 - $$26 * $$13;
					double $$32 = $$26 * $$12 + $$29 * $$13;
					buffer.vertex($$8 + $$30, $$9 + $$28, $$10 + $$32).next();
				}
			}
		}

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

	public void reloadTransparencyShader() {

		this.resetTransparencyShader();

	}

	public void reload() {
		if (this.world != null) {
			this.reloadTransparencyShader();
			this.world.reloadColor();
			if (this.chunkBuilder == null) {
				this.chunkBuilder = new ChunkBuilder(this.world, null, Util.getMainWorkerExecutor(), this.client.is64Bit(), this.bufferBuilders.getBlockBufferBuilders());
			} else {
				this.chunkBuilder.setWorld(this.world);
			}

			this.field_34810 = true;
			this.cloudsDirty = true;
			this.builtChunks.clear();
			RenderLayers.setFancyGraphicsOrBetter(false);
			this.viewDistance = this.client.options.getViewDistance();
			if (this.chunks != null) {
				this.chunks.clear();
			}

			this.chunkBuilder.reset();
			synchronized(this.noCullingBlockEntities) {
				this.noCullingBlockEntities.clear();
			}

			this.chunks = new BuiltChunkStorage(this.chunkBuilder, this.world, this.client.options.getViewDistance(), null);
			if (this.field_34808 != null) {
				try {
					this.field_34808.get();
					this.field_34808 = null;
				} catch (Exception var3) {
					LOGGER.warn((String)"Full update failed", (Throwable)var3);
				}
			}

			this.field_34817.set(new PreviewRenderer.class_6600(this.chunks.chunks.length));
			this.chunkInfos.clear();
			Entity $$1 = WorldPreview.player;
			if ($$1 != null) {
				this.chunks.updateCameraPosition($$1.getX(), $$1.getZ());
			}

		}
	}

	public void onResized(int width, int height) {
		this.scheduleTerrainUpdate();
		if (this.entityOutlineShader != null) {
			this.entityOutlineShader.setupDimensions(width, height);
		}

		if (this.transparencyShader != null) {
			this.transparencyShader.setupDimensions(width, height);
		}

	}

	

	

	

	

	
	private void setupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator) {
		Vec3d $$4 = camera.getPos();
		if (this.client.options.getViewDistance() != this.viewDistance) {
			this.reload();
		}

		this.world.getProfiler().push("camera");
		double $$5 = WorldPreview.player.getX();
		double $$6 = WorldPreview.player.getY();
		double $$7 = WorldPreview.player.getZ();
		double $$8 = $$5 - this.lastCameraChunkUpdateX;
		double $$9 = $$6 - this.lastCameraChunkUpdateY;
		double $$10 = $$7 - this.lastCameraChunkUpdateZ;
		int $$11 = ChunkSectionPos.getSectionCoord($$5);
		int $$12 = ChunkSectionPos.getSectionCoord($$6);
		int $$13 = ChunkSectionPos.getSectionCoord($$7);
		if (this.cameraChunkX != $$11 || this.cameraChunkY != $$12 || this.cameraChunkZ != $$13 || $$8 * $$8 + $$9 * $$9 + $$10 * $$10 > 16.0D) {
			this.lastCameraChunkUpdateX = $$5;
			this.lastCameraChunkUpdateY = $$6;
			this.lastCameraChunkUpdateZ = $$7;
			this.cameraChunkX = $$11;
			this.cameraChunkY = $$12;
			this.cameraChunkZ = $$13;
			this.chunks.updateCameraPosition($$5, $$7);
		}

		this.chunkBuilder.setCameraPosition($$4);
		this.world.getProfiler().swap("cull");
		this.client.getProfiler().swap("culling");
		BlockPos $$14 = camera.getBlockPos();
		double $$15 = Math.floor($$4.x / 8.0D);
		double $$16 = Math.floor($$4.y / 8.0D);
		double $$17 = Math.floor($$4.z / 8.0D);
		this.field_34810 = this.field_34810 || $$15 != this.lastCameraX || $$16 != this.lastCameraY || $$17 != this.lastCameraZ;
		this.field_34811.updateAndGet(($$0) -> {
			if ($$0 > 0L && System.currentTimeMillis() > $$0) {
				this.field_34810 = true;
				return 0L;
			} else {
				return $$0;
			}
		});
		this.lastCameraX = $$15;
		this.lastCameraY = $$16;
		this.lastCameraZ = $$17;
		this.client.getProfiler().swap("update");
		boolean $$18 = this.client.chunkCullingEnabled;
		if (spectator && this.world.getBlockState($$14).isOpaqueFullCube(this.world, $$14)) {
			$$18 = false;
		}

		if (!hasForcedFrustum) {
			if (this.field_34810 && (this.field_34808 == null || this.field_34808.isDone())) {
				this.client.getProfiler().push("full_update_schedule");
				this.field_34810 = false;
				boolean final$$1 = $$18;
				this.field_34808 = Util.getMainWorkerExecutor().submit(() -> {
					Queue<PreviewRenderer.ChunkInfo> $$3 = Queues.newArrayDeque();
					this.method_38549(camera, $$3);
					PreviewRenderer.class_6600 $$4x = new PreviewRenderer.class_6600(this.chunks.chunks.length);
					this.method_34808($$4x.field_34819, $$4x.field_34818, $$4, $$3, final$$1);
					this.field_34817.set($$4x);
					this.field_34809.set(true);
				});
				this.client.getProfiler().pop();
			}

			PreviewRenderer.class_6600 $$20 = (PreviewRenderer.class_6600)this.field_34817.get();
			if (!this.builtChunks.isEmpty()) {
				this.client.getProfiler().push("partial_update");
				ArrayDeque $$21 = Queues.newArrayDeque();

				while(!this.builtChunks.isEmpty()) {
					ChunkBuilder.BuiltChunk $$22 = (ChunkBuilder.BuiltChunk)this.builtChunks.poll();
					PreviewRenderer.ChunkInfo $$23 = $$20.field_34818.getInfo($$22);
					if ($$23 != null && $$23.chunk == $$22) {
						$$21.add($$23);
					}
				}

				this.method_34808($$20.field_34819, $$20.field_34818, $$4, $$21, $$18);
				this.field_34809.set(true);
				this.client.getProfiler().pop();
			}

			double $$24 = Math.floor((double)(camera.getPitch() / 2.0F));
			double $$25 = Math.floor((double)(camera.getYaw() / 2.0F));
			if (this.field_34809.compareAndSet(true, false) || $$24 != this.lastCameraPitch || $$25 != this.lastCameraYaw) {
				this.applyFrustum((new Frustum(frustum)).method_38557(8));
				this.lastCameraPitch = $$24;
				this.lastCameraYaw = $$25;
			}
		}

		this.client.getProfiler().pop();
	}

	private void applyFrustum(Frustum frustum) {
		this.client.getProfiler().push("apply_frustum");
		this.chunkInfos.clear();
		Iterator var2 = ((PreviewRenderer.class_6600)this.field_34817.get()).field_34819.iterator();

		while(var2.hasNext()) {
			PreviewRenderer.ChunkInfo $$1 = (PreviewRenderer.ChunkInfo)var2.next();
			if (frustum.isVisible($$1.chunk.boundingBox)) {
				this.chunkInfos.add($$1);
			}
		}

		this.client.getProfiler().pop();
	}

	private void method_38549(Camera camera, Queue<PreviewRenderer.ChunkInfo> $$1) {
		Vec3d $$3 = camera.getPos();
		BlockPos $$4 = camera.getBlockPos();
		ChunkBuilder.BuiltChunk $$5 = ((BuiltChunkStorageMixin)this.chunks).callGetRenderedChunk($$4);
		if ($$5 == null) {
			boolean $$6 = $$4.getY() > this.world.getBottomY();
			int $$7 = $$6 ? this.world.getTopY() - 8 : this.world.getBottomY() + 8;
			int $$8 = MathHelper.floor($$3.x / 16.0D) * 16;
			int $$9 = MathHelper.floor($$3.z / 16.0D) * 16;
			List<PreviewRenderer.ChunkInfo> $$10 = Lists.newArrayList();

			for(int $$11 = -this.viewDistance; $$11 <= this.viewDistance; ++$$11) {
				for(int $$12 = -this.viewDistance; $$12 <= this.viewDistance; ++$$12) {
					ChunkBuilder.BuiltChunk $$13 = ((BuiltChunkStorageMixin)this.chunks).callGetRenderedChunk(new BlockPos($$8 + ChunkSectionPos.getOffsetPos($$11, 8), $$7, $$9 + ChunkSectionPos.getOffsetPos($$12, 8)));
					if ($$13 != null) {
						$$10.add(new PreviewRenderer.ChunkInfo($$13, (Direction)null, 0));
					}
				}
			}

			$$10.sort(Comparator.comparingDouble(($$1x) -> {
				return $$4.getSquaredDistance($$1x.chunk.getOrigin().add(8, 8, 8));
			}));
			$$1.addAll($$10);
		} else {
			$$1.add(new PreviewRenderer.ChunkInfo($$5, (Direction)null, 0));
		}

	}
	

	private void method_34808(LinkedHashSet<PreviewRenderer.ChunkInfo> $$0, PreviewRenderer.ChunkInfoList $$1, Vec3d $$2, Queue<PreviewRenderer.ChunkInfo> $$3, boolean $$4) {
		BlockPos $$6 = new BlockPos(MathHelper.floor($$2.x / 16.0D) * 16, MathHelper.floor($$2.y / 16.0D) * 16, MathHelper.floor($$2.z / 16.0D) * 16);
		BlockPos $$7 = $$6.add(8, 8, 8);
		Entity.setRenderDistanceMultiplier(MathHelper.clamp((double)this.client.options.getViewDistance() / 8.0D, 1.0D, 2.5D) * (double)this.client.options.entityDistanceScaling);

		while(!$$3.isEmpty()) {
			PreviewRenderer.ChunkInfo $$8 = (PreviewRenderer.ChunkInfo)$$3.poll();
			ChunkBuilder.BuiltChunk $$9 = $$8.chunk;
			$$0.add($$8);
			Direction $$10 = Direction.getFacing((float)($$9.getOrigin().getX() - $$6.getX()), (float)($$9.getOrigin().getY() - $$6.getY()), (float)($$9.getOrigin().getZ() - $$6.getZ()));
			boolean $$11 = Math.abs($$9.getOrigin().getX() - $$6.getX()) > 60 || Math.abs($$9.getOrigin().getY() - $$6.getY()) > 60 || Math.abs($$9.getOrigin().getZ() - $$6.getZ()) > 60;
			Direction[] var13 = DIRECTIONS;
			int var14 = var13.length;

			for(int var15 = 0; var15 < var14; ++var15) {
				Direction $$12 = var13[var15];
				ChunkBuilder.BuiltChunk $$13 = this.getAdjacentChunk($$6, $$9, $$12);
				if ($$13 == null) {
					if (!this.method_38553($$6, $$9)) {
						this.field_34811.set(System.currentTimeMillis() + 500L);
					}
				} else if (!$$4 || !$$8.canCull($$12.getOpposite())) {
					if ($$4 && $$8.hasAnyDirection()) {
						ChunkBuilder.ChunkData $$14 = $$9.getData();
						boolean $$15 = false;

						for(int $$16 = 0; $$16 < DIRECTIONS.length; ++$$16) {
							if ($$8.hasDirection($$16) && $$14.isVisibleThrough(DIRECTIONS[$$16].getOpposite(), $$12)) {
								$$15 = true;
								break;
							}
						}

						if (!$$15) {
							continue;
						}
					}

					PreviewRenderer.ChunkInfo $$26;
					if ($$4 && $$11 && $$8.hasAnyDirection() && !$$8.hasDirection($$10.ordinal())) {
						ChunkBuilder.BuiltChunk $$17 = this.getAdjacentChunk($$6, $$9, $$10.getOpposite());
						if ($$17 == null) {
							continue;
						}

						$$26 = $$1.getInfo($$17);
						if ($$26 == null) {
							continue;
						}
					}

					if ($$4 && $$11) {
						byte var10001;
						BlockPos $$19;
						label140: {
							label139: {
								$$19 = $$13.getOrigin();
								if ($$12.getAxis() == Direction.Axis.X) {
									if ($$7.getX() > $$19.getX()) {
										break label139;
									}
								} else if ($$7.getX() < $$19.getX()) {
									break label139;
								}

								var10001 = 0;
								break label140;
							}

							var10001 = 16;
						}

						byte var10002;
						label132: {
							label131: {
								if ($$12.getAxis() == Direction.Axis.Y) {
									if ($$7.getY() > $$19.getY()) {
										break label131;
									}
								} else if ($$7.getY() < $$19.getY()) {
									break label131;
								}

								var10002 = 0;
								break label132;
							}

							var10002 = 16;
						}

						byte var10003;
						label124: {
							label123: {
								if ($$12.getAxis() == Direction.Axis.Z) {
									if ($$7.getZ() > $$19.getZ()) {
										break label123;
									}
								} else if ($$7.getZ() < $$19.getZ()) {
									break label123;
								}

								var10003 = 0;
								break label124;
							}

							var10003 = 16;
						}

						BlockPos $$20 = $$19.add(var10001, var10002, var10003);
						Vec3d $$21 = new Vec3d((double)$$20.getX(), (double)$$20.getY(), (double)$$20.getZ());
						Vec3d $$22 = $$2.subtract($$21).normalize().multiply(field_34814);
						boolean $$23 = true;

						label115: {
							ChunkBuilder.BuiltChunk $$24;
							do {
								if (!($$2.subtract($$21).lengthSquared() > 3600.0D)) {
									break label115;
								}

								$$21 = $$21.add($$22);
								if ($$21.y > (double)this.world.getTopY() || $$21.y < (double)this.world.getBottomY()) {
									break label115;
								}

								$$24 = ((BuiltChunkStorageMixin)this.chunks).callGetRenderedChunk(new BlockPos($$21.x, $$21.y, $$21.z));
							} while($$24 != null && $$1.getInfo($$24) != null);

							$$23 = false;
						}

						if (!$$23) {
							continue;
						}
					}

					PreviewRenderer.ChunkInfo $$25 = $$1.getInfo($$13);
					if ($$25 != null) {
						$$25.addDirection($$12);
					} else if (!$$13.shouldBuild()) {
						if (!this.method_38553($$6, $$9)) {
							this.field_34811.set(System.currentTimeMillis() + 500L);
						}
					} else {
						$$26 = new PreviewRenderer.ChunkInfo($$13, $$12, $$8.propagationLevel + 1);
						$$26.updateCullingState($$8.cullingState, $$12);
						$$3.add($$26);
						$$1.setInfo($$13, $$26);
					}
				}
			}
		}

	}

	@Nullable
	private ChunkBuilder.BuiltChunk getAdjacentChunk(BlockPos pos, ChunkBuilder.BuiltChunk chunk, Direction direction) {
		BlockPos $$3 = chunk.getNeighborPosition(direction);
		if (MathHelper.abs(pos.getX() - $$3.getX()) > this.viewDistance * 16) {
			return null;
		} else if (MathHelper.abs(pos.getY() - $$3.getY()) <= this.viewDistance * 16 && $$3.getY() >= this.world.getBottomY() && $$3.getY() < this.world.getTopY()) {
			return MathHelper.abs(pos.getZ() - $$3.getZ()) > this.viewDistance * 16 ? null : ((BuiltChunkStorageMixin)this.chunks).callGetRenderedChunk($$3);
		} else {
			return null;
		}
	}

	private boolean method_38553(BlockPos pos, ChunkBuilder.BuiltChunk $$1) {
		int $$2 = ChunkSectionPos.getSectionCoord(pos.getX());
		int $$3 = ChunkSectionPos.getSectionCoord(pos.getZ());
		BlockPos $$4 = $$1.getOrigin();
		int $$5 = ChunkSectionPos.getSectionCoord($$4.getX());
		int $$6 = ChunkSectionPos.getSectionCoord($$4.getZ());
		return !ThreadedAnvilChunkStorage.method_39975($$5, $$6, $$2, $$3, this.viewDistance - 2);
	}

	private void captureFrustum(Matrix4f positionMatrix, Matrix4f $$1, double x, double y, double z, Frustum frustum) {
		this.capturedFrustum = frustum;
		Matrix4f $$6 = $$1.copy();
		$$6.multiply(positionMatrix);
		$$6.invert();
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

		for(int $$7 = 0; $$7 < 8; ++$$7) {
			this.capturedFrustumOrientation[$$7].transform($$6);
			this.capturedFrustumOrientation[$$7].normalizeProjectiveCoordinates();
		}

	}



	public void render(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix) {
		RenderSystem.setShaderGameTime(this.world.getTime(), tickDelta);
		this.blockEntityRenderDispatcher.configure(this.world, camera, null);
		this.entityRenderDispatcher.configure(this.world, camera,null);
		Profiler $$8 = this.world.getProfiler();
		$$8.swap("light_update_queue");
		this.world.runQueuedChunkUpdates();
		$$8.swap("light_updates");
		boolean $$9 = this.world.hasNoChunkUpdaters();
		this.world.getChunkManager().getLightingProvider().doLightUpdates(Integer.MAX_VALUE, $$9, true);
		Vec3d $$10 = camera.getPos();
		double $$11 = $$10.getX();
		double $$12 = $$10.getY();
		double $$13 = $$10.getZ();
		Matrix4f $$14 = matrices.peek().getPositionMatrix();
		$$8.swap("culling");
		boolean $$15 = this.capturedFrustum != null;
		Frustum $$17;
		if ($$15) {
			$$17 = this.capturedFrustum;
			$$17.setPosition(this.capturedFrustumPosition.x, this.capturedFrustumPosition.y, this.capturedFrustumPosition.z);
		} else {
			$$17 = this.frustum;
		}

		this.client.getProfiler().swap("captureFrustum");
		if (this.shouldCaptureFrustum) {
			this.captureFrustum($$14, positionMatrix, $$10.x, $$10.y, $$10.z, $$15 ? new Frustum($$14, positionMatrix) : $$17);
			this.shouldCaptureFrustum = false;
		}

		$$8.swap("clear");
		BackgroundRenderer.render(camera, tickDelta, WorldPreview.clientWorld, this.client.options.getViewDistance(), gameRenderer.getSkyDarkness(tickDelta));
		BackgroundRenderer.setFogBlack();
		RenderSystem.clear(16640, MinecraftClient.IS_SYSTEM_MAC);
		float $$18 = gameRenderer.getViewDistance();
		boolean $$19 = WorldPreview.clientWorld.getDimensionEffects().useThickFog(MathHelper.floor($$11), MathHelper.floor($$12)) ;
		$$8.swap("sky");
		RenderSystem.setShader(GameRenderer::getPositionShader);
		this.renderSky(matrices, positionMatrix, tickDelta, () -> {
			BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_SKY, $$18, $$19);
		});
		$$8.swap("fog");
		BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_TERRAIN, Math.max($$18, 32.0F), $$19);
		$$8.swap("terrain_setup");
		this.setupTerrain(camera, $$17, $$15,false);
		$$8.swap("compilechunks");
		this.updateChunks(camera);
		$$8.swap("terrain");
		this.renderLayer(RenderLayer.getSolid(), matrices, $$11, $$12, $$13, positionMatrix);
		this.renderLayer(RenderLayer.getCutoutMipped(), matrices, $$11, $$12, $$13, positionMatrix);
		this.renderLayer(RenderLayer.getCutout(), matrices, $$11, $$12, $$13, positionMatrix);
		if (this.world.getDimensionEffects().isDarkened()) {
			DiffuseLighting.enableForLevel(matrices.peek().getPositionMatrix());
		} else {
			DiffuseLighting.disableForLevel(matrices.peek().getPositionMatrix());
		}

		$$8.swap("entities");
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


		boolean $$20 = false;
		VertexConsumerProvider.Immediate $$21 = this.bufferBuilders.getEntityVertexConsumers();
		Iterator var26 = this.world.getEntities().iterator();

		while(true) {
			Entity $$22;
			int $$37;
			do {
				do {
					do {
						if (!var26.hasNext()) {
							$$21.drawCurrentLayer();
							this.checkEmpty(matrices);
							$$21.draw(RenderLayer.getEntitySolid(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
							$$21.draw(RenderLayer.getEntityCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
							$$21.draw(RenderLayer.getEntityCutoutNoCull(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
							$$21.draw(RenderLayer.getEntitySmoothCutout(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));
							$$8.swap("blockentities");
							ObjectListIterator var40 = this.chunkInfos.iterator();

							while(true) {
								List $$32;
								do {
									if (!var40.hasNext()) {
										synchronized(this.noCullingBlockEntities) {
											Iterator var44 = this.noCullingBlockEntities.iterator();

											while(true) {
												if (!var44.hasNext()) {
													break;
												}

												BlockEntity $$40 = (BlockEntity)var44.next();
												BlockPos $$41 = $$40.getPos();
												matrices.push();
												matrices.translate((double)$$41.getX() - $$11, (double)$$41.getY() - $$12, (double)$$41.getZ() - $$13);
												this.blockEntityRenderDispatcher.render($$40, tickDelta, matrices, $$21);
												matrices.pop();
											}
										}

										this.checkEmpty(matrices);
										$$21.draw(RenderLayer.getSolid());
										$$21.draw(RenderLayer.getEndPortal());
										$$21.draw(RenderLayer.getEndGateway());
										$$21.draw(TexturedRenderLayers.getEntitySolid());
										$$21.draw(TexturedRenderLayers.getEntityCutout());
										$$21.draw(TexturedRenderLayers.getBeds());
										$$21.draw(TexturedRenderLayers.getShulkerBoxes());
										$$21.draw(TexturedRenderLayers.getSign());
										$$21.draw(TexturedRenderLayers.getChest());
										this.bufferBuilders.getOutlineVertexConsumers().draw();
										if ($$20) {
											this.entityOutlineShader.render(tickDelta);
											this.client.getFramebuffer().beginWrite(false);
										}

										$$8.swap("destroyProgress");


										this.checkEmpty(matrices);


										MatrixStack $$55 = RenderSystem.getModelViewStack();
										$$55.push();
										$$55.multiplyPositionMatrix(matrices.peek().getPositionMatrix());
										RenderSystem.applyModelViewMatrix();
										$$55.pop();
										RenderSystem.applyModelViewMatrix();
										$$21.draw(TexturedRenderLayers.getEntityTranslucentCull());
										$$21.draw(TexturedRenderLayers.getBannerPatterns());
										$$21.draw(TexturedRenderLayers.getShieldPatterns());
										$$21.draw(RenderLayer.getArmorGlint());
										$$21.draw(RenderLayer.getArmorEntityGlint());
										$$21.draw(RenderLayer.getGlint());
										$$21.draw(RenderLayer.getDirectGlint());
										$$21.draw(RenderLayer.getGlintTranslucent());
										$$21.draw(RenderLayer.getEntityGlint());
										$$21.draw(RenderLayer.getDirectEntityGlint());
										$$21.draw(RenderLayer.getWaterMask());
										this.bufferBuilders.getEffectVertexConsumers().draw();
										if (this.transparencyShader != null) {
											$$21.draw(RenderLayer.getLines());
											$$21.draw();
											this.translucentFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
											this.translucentFramebuffer.copyDepthFrom(this.client.getFramebuffer());
											$$8.swap("translucent");
											this.renderLayer(RenderLayer.getTranslucent(), matrices, $$11, $$12, $$13, positionMatrix);
											$$8.swap("string");
											this.renderLayer(RenderLayer.getTripwire(), matrices, $$11, $$12, $$13, positionMatrix);
											this.particlesFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
											this.particlesFramebuffer.copyDepthFrom(this.client.getFramebuffer());
											RenderPhaseMixin.getPARTICLES_TARGET().startDrawing();
										} else {
											$$8.swap("translucent");
											if (this.translucentFramebuffer != null) {
												this.translucentFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
											}

											this.renderLayer(RenderLayer.getTranslucent(), matrices, $$11, $$12, $$13, positionMatrix);
											$$21.draw(RenderLayer.getLines());
											$$21.draw();
											$$8.swap("string");
											this.renderLayer(RenderLayer.getTripwire(), matrices, $$11, $$12, $$13, positionMatrix);
										}

										$$55.push();
										$$55.multiplyPositionMatrix(matrices.peek().getPositionMatrix());
										RenderSystem.applyModelViewMatrix();
										if (this.client.options.getCloudRenderMode() != CloudRenderMode.OFF) {
											if (this.transparencyShader != null) {
												this.cloudsFramebuffer.clear(MinecraftClient.IS_SYSTEM_MAC);
												RenderPhaseMixin.getCLOUDS_TARGET().startDrawing();
												$$8.swap("clouds");
												this.renderClouds(matrices, positionMatrix, tickDelta, $$11, $$12, $$13);
												RenderPhaseMixin.getCLOUDS_TARGET().endDrawing();
											} else {
												$$8.swap("clouds");
												RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
												this.renderClouds(matrices, positionMatrix, tickDelta, $$11, $$12, $$13);
											}
										}

										if (this.transparencyShader != null) {
											RenderPhaseMixin.getWEATHER_TARGET().startDrawing();
											$$8.swap("weather");
											this.renderWeather(lightmapTextureManager, tickDelta, $$11, $$12, $$13);
											RenderPhaseMixin.getWEATHER_TARGET().endDrawing();
											this.transparencyShader.render(tickDelta);
											this.client.getFramebuffer().beginWrite(false);
										} else {
											RenderSystem.depthMask(false);
											$$8.swap("weather");
											this.renderWeather(lightmapTextureManager, tickDelta, $$11, $$12, $$13);
											RenderSystem.depthMask(true);
										}

										this.renderChunkDebugInfo(camera);
										RenderSystem.depthMask(true);
										RenderSystem.disableBlend();
										$$55.pop();
										RenderSystem.applyModelViewMatrix();
										BackgroundRenderer.clearFog();
										return;
									}

									PreviewRenderer.ChunkInfo $$31 = (PreviewRenderer.ChunkInfo)var40.next();
									$$32 = $$31.chunk.getData().getBlockEntities();
								} while($$32.isEmpty());

								Iterator var49 = $$32.iterator();

								while(var49.hasNext()) {
									BlockEntity $$33 = (BlockEntity)var49.next();
									BlockPos $$34 = $$33.getPos();
									VertexConsumerProvider $$35 = $$21;
									matrices.push();
									matrices.translate((double)$$34.getX() - $$11, (double)$$34.getY() - $$12, (double)$$34.getZ() - $$13);
									SortedSet<BlockBreakingInfo> $$36 = (SortedSet)this.blockBreakingProgressions.get($$34.asLong());
									if ($$36 != null && !$$36.isEmpty()) {
										$$37 = ((BlockBreakingInfo)$$36.last()).getStage();
										if ($$37 >= 0) {
											MatrixStack.Entry $$38 = matrices.peek();
											VertexConsumer $$39 = new OverlayVertexConsumer(this.bufferBuilders.getEffectVertexConsumers().getBuffer((RenderLayer)ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get($$37)), $$38.getPositionMatrix(), $$38.getNormalMatrix());
											$$35 = (renderLayer) -> {
												VertexConsumer $$3 = $$21.getBuffer(renderLayer);
												return renderLayer.hasCrumbling() ? VertexConsumers.union($$39, $$3) : $$3;
											};
										}
									}

									this.blockEntityRenderDispatcher.render($$33, tickDelta, matrices, (VertexConsumerProvider)$$35);
									matrices.pop();
								}
							}
						}

						$$22 = (Entity)var26.next();
					} while(!this.entityRenderDispatcher.shouldRender($$22, $$17, $$11, $$12, $$13));
				} while($$22 == camera.getFocusedEntity() && !camera.isThirdPerson() && (!(camera.getFocusedEntity() instanceof LivingEntity) || !((LivingEntity)camera.getFocusedEntity()).isSleeping()));
			} while($$22 instanceof ClientPlayerEntity && camera.getFocusedEntity() != $$22);

			++this.regularEntityCount;
			if ($$22.age == 0) {
				$$22.lastRenderX = $$22.getX();
				$$22.lastRenderY = $$22.getY();
				$$22.lastRenderZ = $$22.getZ();
			}

			Object $$30;

				$$30 = $$21;

			this.renderEntity($$22, $$11, $$12, $$13, tickDelta, matrices, (VertexConsumerProvider)$$30);
		}
	}

	private void checkEmpty(MatrixStack matrices) {
		if (!matrices.isEmpty()) {
			throw new IllegalStateException("Pose stack not empty");
		}
	}

	private void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
		double $$7 = MathHelper.lerp((double)tickDelta, entity.lastRenderX, entity.getX());
		double $$8 = MathHelper.lerp((double)tickDelta, entity.lastRenderY, entity.getY());
		double $$9 = MathHelper.lerp((double)tickDelta, entity.lastRenderZ, entity.getZ());
		float $$10 = MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw());
		this.entityRenderDispatcher.render(entity, $$7 - cameraX, $$8 - cameraY, $$9 - cameraZ, $$10, tickDelta, matrices, vertexConsumers, this.entityRenderDispatcher.getLight(entity, tickDelta));
	}

	private void renderLayer(RenderLayer renderLayer, MatrixStack matrices, double $$2, double $$3, double $$4, Matrix4f positionMatrix) {
		RenderSystem.assertOnRenderThread();
		renderLayer.startDrawing();
		if (renderLayer == RenderLayer.getTranslucent()) {
			this.client.getProfiler().push("translucent_sort");
			double $$6 = $$2 - this.lastTranslucentSortX;
			double $$7 = $$3 - this.lastTranslucentSortY;
			double $$8 = $$4 - this.lastTranslucentSortZ;
			if ($$6 * $$6 + $$7 * $$7 + $$8 * $$8 > 1.0D) {
				this.lastTranslucentSortX = $$2;
				this.lastTranslucentSortY = $$3;
				this.lastTranslucentSortZ = $$4;
				int $$9 = 0;
				ObjectListIterator var17 = this.chunkInfos.iterator();

				while(var17.hasNext()) {
					PreviewRenderer.ChunkInfo $$10 = (PreviewRenderer.ChunkInfo)var17.next();
					if ($$9 < 15 && $$10.chunk.scheduleSort(renderLayer, this.chunkBuilder)) {
						++$$9;
					}
				}
			}

			this.client.getProfiler().pop();
		}

		this.client.getProfiler().push("filterempty");
		this.client.getProfiler().swap(() -> {
			return "render_" + renderLayer;
		});
		boolean $$11 = renderLayer != RenderLayer.getTranslucent();
		ObjectListIterator<PreviewRenderer.ChunkInfo> $$12 = this.chunkInfos.listIterator($$11 ? 0 : this.chunkInfos.size());
		VertexFormat $$13 = renderLayer.getVertexFormat();
		Shader $$14 = RenderSystem.getShader();
		BufferRenderer.unbindAll();

		for(int $$15 = 0; $$15 < 12; ++$$15) {
			int $$16 = RenderSystem.getShaderTexture($$15);
			$$14.addSampler("Sampler" + $$15, $$16);
		}

		if ($$14.modelViewMat != null) {
			$$14.modelViewMat.set(matrices.peek().getPositionMatrix());
		}

		if ($$14.projectionMat != null) {
			$$14.projectionMat.set(positionMatrix);
		}

		if ($$14.colorModulator != null) {
			$$14.colorModulator.set(RenderSystem.getShaderColor());
		}

		if ($$14.fogStart != null) {
			$$14.fogStart.set(RenderSystem.getShaderFogStart());
		}

		if ($$14.fogEnd != null) {
			$$14.fogEnd.set(RenderSystem.getShaderFogEnd());
		}

		if ($$14.fogColor != null) {
			$$14.fogColor.set(RenderSystem.getShaderFogColor());
		}

		if ($$14.textureMat != null) {
			$$14.textureMat.set(RenderSystem.getTextureMatrix());
		}

		if ($$14.gameTime != null) {
			$$14.gameTime.set(RenderSystem.getShaderGameTime());
		}

		RenderSystem.setupShaderLights($$14);
		$$14.bind();
		GlUniform $$17 = $$14.chunkOffset;
		boolean $$18 = false;

		while(true) {
			if ($$11) {
				if (!$$12.hasNext()) {
					break;
				}
			} else if (!$$12.hasPrevious()) {
				break;
			}

			PreviewRenderer.ChunkInfo $$19 = $$11 ? (PreviewRenderer.ChunkInfo)$$12.next() : (PreviewRenderer.ChunkInfo)$$12.previous();
			ChunkBuilder.BuiltChunk $$20 = $$19.chunk;
			if (!$$20.getData().isEmpty(renderLayer)) {
				VertexBuffer $$21 = $$20.getBuffer(renderLayer);
				BlockPos $$22 = $$20.getOrigin();
				if ($$17 != null) {
					$$17.set((float)((double)$$22.getX() - $$2), (float)((double)$$22.getY() - $$3), (float)((double)$$22.getZ() - $$4));
					$$17.upload();
				}

				$$21.drawVertices();
				$$18 = true;
			}
		}

		if ($$17 != null) {
			$$17.set(Vec3f.ZERO);
		}

		$$14.unbind();
		if ($$18) {
			$$13.endDrawing();
		}

		VertexBuffer.unbind();
		VertexBuffer.unbindVertexArray();
		this.client.getProfiler().pop();
		renderLayer.endDrawing();
	}

	private void renderChunkDebugInfo(Camera camera) {
		Tessellator $$1 = Tessellator.getInstance();
		BufferBuilder $$2 = $$1.getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		if (this.client.debugChunkInfo || this.client.debugChunkOcclusion) {
			double $$3 = camera.getPos().getX();
			double $$4 = camera.getPos().getY();
			double $$5 = camera.getPos().getZ();
			RenderSystem.depthMask(true);
			RenderSystem.disableCull();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableTexture();
			ObjectListIterator var10 = this.chunkInfos.iterator();

			while(var10.hasNext()) {
				PreviewRenderer.ChunkInfo $$6 = (PreviewRenderer.ChunkInfo)var10.next();
				ChunkBuilder.BuiltChunk $$7 = $$6.chunk;
				BlockPos $$8 = $$7.getOrigin();
				MatrixStack $$9 = RenderSystem.getModelViewStack();
				$$9.push();
				$$9.translate((double)$$8.getX() - $$3, (double)$$8.getY() - $$4, (double)$$8.getZ() - $$5);
				RenderSystem.applyModelViewMatrix();
				RenderSystem.setShader(GameRenderer::getRenderTypeLinesShader);
				int $$16;
				int $$12;
				int $$13;
				if (this.client.debugChunkInfo) {
					$$2.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
					RenderSystem.lineWidth(5.0F);
					$$16 = $$6.propagationLevel == 0 ? 0 : MathHelper.hsvToRgb((float)$$6.propagationLevel / 50.0F, 0.9F, 0.9F);
					int $$11 = $$16 >> 16 & 255;
					$$12 = $$16 >> 8 & 255;
					$$13 = $$16 & 255;

					for(int $$14 = 0; $$14 < DIRECTIONS.length; ++$$14) {
						if ($$6.hasDirection($$14)) {
							Direction $$15 = DIRECTIONS[$$14];
							$$2.vertex(8.0D, 8.0D, 8.0D).color($$11, $$12, $$13, 255).normal((float)$$15.getOffsetX(), (float)$$15.getOffsetY(), (float)$$15.getOffsetZ()).next();
							$$2.vertex((double)(8 - 16 * $$15.getOffsetX()), (double)(8 - 16 * $$15.getOffsetY()), (double)(8 - 16 * $$15.getOffsetZ())).color($$11, $$12, $$13, 255).normal((float)$$15.getOffsetX(), (float)$$15.getOffsetY(), (float)$$15.getOffsetZ()).next();
						}
					}

					$$1.draw();
					RenderSystem.lineWidth(1.0F);
				}

				if (this.client.debugChunkOcclusion && !$$7.getData().isEmpty()) {
					$$2.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
					RenderSystem.setShader(GameRenderer::getRenderTypeLinesShader);
					RenderSystem.lineWidth(5.0F);
					$$16 = 0;
					Direction[] var26 = DIRECTIONS;
					$$12 = var26.length;
					$$13 = 0;

					while(true) {
						if ($$13 >= $$12) {
							$$1.draw();
							RenderSystem.lineWidth(1.0F);
							RenderSystem.setShader(GameRenderer::getPositionColorShader);
							if ($$16 > 0) {
								$$2.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
								float $$20 = 0.5F;
								float $$21 = 0.2F;
								$$2.vertex(0.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(15.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(15.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(0.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(0.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(15.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(15.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(0.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(0.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(0.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(0.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(0.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(15.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(15.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(15.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(15.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(0.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(15.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(15.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(0.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(0.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(15.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(15.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$2.vertex(0.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).next();
								$$1.draw();
							}
							break;
						}

						Direction $$17 = var26[$$13];
						Direction[] var30 = DIRECTIONS;
						int var21 = var30.length;

						for(int var22 = 0; var22 < var21; ++var22) {
							Direction $$18 = var30[var22];
							boolean $$19 = $$7.getData().isVisibleThrough($$17, $$18);
							if (!$$19) {
								++$$16;
								$$2.vertex((double)(8 + 8 * $$17.getOffsetX()), (double)(8 + 8 * $$17.getOffsetY()), (double)(8 + 8 * $$17.getOffsetZ())).color(255, 0, 0, 255).normal((float)$$17.getOffsetX(), (float)$$17.getOffsetY(), (float)$$17.getOffsetZ()).next();
								$$2.vertex((double)(8 + 8 * $$18.getOffsetX()), (double)(8 + 8 * $$18.getOffsetY()), (double)(8 + 8 * $$18.getOffsetZ())).color(255, 0, 0, 255).normal((float)$$18.getOffsetX(), (float)$$18.getOffsetY(), (float)$$18.getOffsetZ()).next();
							}
						}

						++$$13;
					}
				}

				$$9.pop();
				RenderSystem.applyModelViewMatrix();
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
			RenderSystem.lineWidth(5.0F);
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			MatrixStack $$22 = RenderSystem.getModelViewStack();
			$$22.push();
			$$22.translate((double)((float)(this.capturedFrustumPosition.x - camera.getPos().x)), (double)((float)(this.capturedFrustumPosition.y - camera.getPos().y)), (double)((float)(this.capturedFrustumPosition.z - camera.getPos().z)));
			RenderSystem.applyModelViewMatrix();
			RenderSystem.depthMask(true);
			$$2.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
			this.method_22985($$2, 0, 1, 2, 3, 0, 1, 1);
			this.method_22985($$2, 4, 5, 6, 7, 1, 0, 0);
			this.method_22985($$2, 0, 1, 5, 4, 1, 1, 0);
			this.method_22985($$2, 2, 3, 7, 6, 0, 0, 1);
			this.method_22985($$2, 0, 4, 7, 3, 0, 1, 0);
			this.method_22985($$2, 1, 5, 6, 2, 1, 0, 1);
			$$1.draw();
			RenderSystem.depthMask(false);
			RenderSystem.setShader(GameRenderer::getRenderTypeLinesShader);
			$$2.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			this.method_22984($$2, 0);
			this.method_22984($$2, 1);
			this.method_22984($$2, 1);
			this.method_22984($$2, 2);
			this.method_22984($$2, 2);
			this.method_22984($$2, 3);
			this.method_22984($$2, 3);
			this.method_22984($$2, 0);
			this.method_22984($$2, 4);
			this.method_22984($$2, 5);
			this.method_22984($$2, 5);
			this.method_22984($$2, 6);
			this.method_22984($$2, 6);
			this.method_22984($$2, 7);
			this.method_22984($$2, 7);
			this.method_22984($$2, 4);
			this.method_22984($$2, 0);
			this.method_22984($$2, 4);
			this.method_22984($$2, 1);
			this.method_22984($$2, 5);
			this.method_22984($$2, 2);
			this.method_22984($$2, 6);
			this.method_22984($$2, 3);
			this.method_22984($$2, 7);
			$$1.draw();
			$$22.pop();
			RenderSystem.applyModelViewMatrix();
			RenderSystem.depthMask(true);
			RenderSystem.disableBlend();
			RenderSystem.enableCull();
			RenderSystem.enableTexture();
			RenderSystem.lineWidth(1.0F);
		}

	}

	private void method_22984(VertexConsumer $$0, int $$1) {
		$$0.vertex((double)this.capturedFrustumOrientation[$$1].getX(), (double)this.capturedFrustumOrientation[$$1].getY(), (double)this.capturedFrustumOrientation[$$1].getZ()).color(0, 0, 0, 255).normal(0.0F, 0.0F, -1.0F).next();
	}

	private void method_22985(VertexConsumer $$0, int $$1, int $$2, int $$3, int $$4, int $$5, int $$6, int $$7) {
		float $$8 = 0.25F;
		$$0.vertex((double)this.capturedFrustumOrientation[$$1].getX(), (double)this.capturedFrustumOrientation[$$1].getY(), (double)this.capturedFrustumOrientation[$$1].getZ()).color((float)$$5, (float)$$6, (float)$$7, 0.25F).next();
		$$0.vertex((double)this.capturedFrustumOrientation[$$2].getX(), (double)this.capturedFrustumOrientation[$$2].getY(), (double)this.capturedFrustumOrientation[$$2].getZ()).color((float)$$5, (float)$$6, (float)$$7, 0.25F).next();
		$$0.vertex((double)this.capturedFrustumOrientation[$$3].getX(), (double)this.capturedFrustumOrientation[$$3].getY(), (double)this.capturedFrustumOrientation[$$3].getZ()).color((float)$$5, (float)$$6, (float)$$7, 0.25F).next();
		$$0.vertex((double)this.capturedFrustumOrientation[$$4].getX(), (double)this.capturedFrustumOrientation[$$4].getY(), (double)this.capturedFrustumOrientation[$$4].getZ()).color((float)$$5, (float)$$6, (float)$$7, 0.25F).next();
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
			ObjectIterator $$0 = this.blockBreakingInfos.values().iterator();

			while($$0.hasNext()) {
				BlockBreakingInfo $$1 = (BlockBreakingInfo)$$0.next();
				int $$2 = $$1.getLastUpdateTick();
				if (this.ticks - $$2 > 400) {
					$$0.remove();
					this.removeBlockBreakingInfo($$1);
				}
			}

		}
	}

	private void removeBlockBreakingInfo(BlockBreakingInfo info) {
		long $$1 = info.getPos().asLong();
		Set<BlockBreakingInfo> $$2 = (Set)this.blockBreakingProgressions.get($$1);
		$$2.remove(info);
		if ($$2.isEmpty()) {
			this.blockBreakingProgressions.remove($$1);
		}

	}

	private void renderEndSky(MatrixStack matrices) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(false);
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, END_SKY);
		Tessellator $$1 = Tessellator.getInstance();
		BufferBuilder $$2 = $$1.getBuffer();

		for(int $$3 = 0; $$3 < 6; ++$$3) {
			matrices.push();
			if ($$3 == 1) {
				matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0F));
			}

			if ($$3 == 2) {
				matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-90.0F));
			}

			if ($$3 == 3) {
				matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(180.0F));
			}

			if ($$3 == 4) {
				matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(90.0F));
			}

			if ($$3 == 5) {
				matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(-90.0F));
			}

			Matrix4f $$4 = matrices.peek().getPositionMatrix();
			$$2.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
			$$2.vertex($$4, -100.0F, -100.0F, -100.0F).texture(0.0F, 0.0F).color(40, 40, 40, 255).next();
			$$2.vertex($$4, -100.0F, -100.0F, 100.0F).texture(0.0F, 16.0F).color(40, 40, 40, 255).next();
			$$2.vertex($$4, 100.0F, -100.0F, 100.0F).texture(16.0F, 16.0F).color(40, 40, 40, 255).next();
			$$2.vertex($$4, 100.0F, -100.0F, -100.0F).texture(16.0F, 0.0F).color(40, 40, 40, 255).next();
			$$1.draw();
			matrices.pop();
		}

		RenderSystem.depthMask(true);
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
	}

	public void renderSky(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, Runnable $$3) {
		$$3.run();
		if (WorldPreview.clientWorld.getDimensionEffects().getSkyType() == DimensionEffects.SkyType.END) {
			this.renderEndSky(matrices);
		} else if (WorldPreview.clientWorld.getDimensionEffects().getSkyType() == DimensionEffects.SkyType.NORMAL) {
			RenderSystem.disableTexture();
			Vec3d $$4 = this.world.getSkyColor(WorldPreview.camera.getPos(), tickDelta);
			float $$5 = (float)$$4.x;
			float $$6 = (float)$$4.y;
			float $$7 = (float)$$4.z;
			BackgroundRenderer.setFogBlack();
			BufferBuilder $$8 = Tessellator.getInstance().getBuffer();
			RenderSystem.depthMask(false);
			RenderSystem.setShaderColor($$5, $$6, $$7, 1.0F);
			Shader $$9 = RenderSystem.getShader();
			this.lightSkyBuffer.setShader(matrices.peek().getPositionMatrix(), projectionMatrix, $$9);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			float[] $$10 = this.world.getDimensionEffects().getFogColorOverride(this.world.getSkyAngle(tickDelta), tickDelta);
			float $$21;
			float $$23;
			float $$18;
			float $$19;
			float $$20;
			if ($$10 != null) {
				RenderSystem.setShader(GameRenderer::getPositionColorShader);
				RenderSystem.disableTexture();
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				matrices.push();
				matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0F));
				$$21 = MathHelper.sin(this.world.getSkyAngleRadians(tickDelta)) < 0.0F ? 180.0F : 0.0F;
				matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion($$21));
				matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(90.0F));
				float $$12 = $$10[0];
				$$23 = $$10[1];
				float $$14 = $$10[2];
				Matrix4f $$15 = matrices.peek().getPositionMatrix();
				$$8.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
				$$8.vertex($$15, 0.0F, 100.0F, 0.0F).color($$12, $$23, $$14, $$10[3]).next();

				for(int $$17 = 0; $$17 <= 16; ++$$17) {
					$$18 = (float)$$17 * 6.2831855F / 16.0F;
					$$19 = MathHelper.sin($$18);
					$$20 = MathHelper.cos($$18);
					$$8.vertex($$15, $$19 * 120.0F, $$20 * 120.0F, -$$20 * 40.0F * $$10[3]).color($$10[0], $$10[1], $$10[2], 0.0F).next();
				}

				$$8.end();
				BufferRenderer.draw($$8);
				matrices.pop();
			}

			RenderSystem.enableTexture();
			RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
			matrices.push();
			$$21 = 1.0F - this.world.getRainGradient(tickDelta);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, $$21);
			matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(-90.0F));
			matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(this.world.getSkyAngle(tickDelta) * 360.0F));
			Matrix4f $$22 = matrices.peek().getPositionMatrix();
			$$23 = 30.0F;
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			RenderSystem.setShaderTexture(0, SUN);
			$$8.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
			$$8.vertex($$22, -$$23, 100.0F, -$$23).texture(0.0F, 0.0F).next();
			$$8.vertex($$22, $$23, 100.0F, -$$23).texture(1.0F, 0.0F).next();
			$$8.vertex($$22, $$23, 100.0F, $$23).texture(1.0F, 1.0F).next();
			$$8.vertex($$22, -$$23, 100.0F, $$23).texture(0.0F, 1.0F).next();
			$$8.end();
			BufferRenderer.draw($$8);
			$$23 = 20.0F;
			RenderSystem.setShaderTexture(0, MOON_PHASES);
			int $$24 = this.world.getMoonPhase();
			int $$25 = $$24 % 4;
			int $$26 = $$24 / 4 % 2;
			float $$27 = (float)($$25 + 0) / 4.0F;
			$$18 = (float)($$26 + 0) / 2.0F;
			$$19 = (float)($$25 + 1) / 4.0F;
			$$20 = (float)($$26 + 1) / 2.0F;
			$$8.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
			$$8.vertex($$22, -$$23, -100.0F, $$23).texture($$19, $$20).next();
			$$8.vertex($$22, $$23, -100.0F, $$23).texture($$27, $$20).next();
			$$8.vertex($$22, $$23, -100.0F, -$$23).texture($$27, $$18).next();
			$$8.vertex($$22, -$$23, -100.0F, -$$23).texture($$19, $$18).next();
			$$8.end();
			BufferRenderer.draw($$8);
			RenderSystem.disableTexture();
			float $$31 = this.world.method_23787(tickDelta) * $$21;
			if ($$31 > 0.0F) {
				RenderSystem.setShaderColor($$31, $$31, $$31, $$31);
				BackgroundRenderer.clearFog();
				this.starsBuffer.setShader(matrices.peek().getPositionMatrix(), projectionMatrix, GameRenderer.getPositionShader());
				$$3.run();
			}

			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.disableBlend();
			matrices.pop();
			RenderSystem.disableTexture();
			RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
			double $$32 =WorldPreview.player.getCameraPosVec(tickDelta).y - this.world.getLevelProperties().getSkyDarknessHeight(this.world);
			if ($$32 < 0.0D) {
				matrices.push();
				matrices.translate(0.0D, 12.0D, 0.0D);
				this.darkSkyBuffer.setShader(matrices.peek().getPositionMatrix(), projectionMatrix, $$9);
				matrices.pop();
			}

			if (this.world.getDimensionEffects().isAlternateSkyColor()) {
				RenderSystem.setShaderColor($$5 * 0.2F + 0.04F, $$6 * 0.2F + 0.04F, $$7 * 0.6F + 0.1F, 1.0F);
			} else {
				RenderSystem.setShaderColor($$5, $$6, $$7, 1.0F);
			}

			RenderSystem.enableTexture();
			RenderSystem.depthMask(true);
		}
	}

	public void renderClouds(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, double $$3, double $$4, double $$5) {
		float $$6 = this.world.getDimensionEffects().getCloudsHeight();
		if (!Float.isNaN($$6)) {
			RenderSystem.disableCull();
			RenderSystem.enableBlend();
			RenderSystem.enableDepthTest();
			RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.depthMask(true);
			float $$7 = 12.0F;
			float $$8 = 4.0F;
			double $$9 = 2.0E-4D;
			double $$10 = (double)(((float)this.ticks + tickDelta) * 0.03F);
			double $$11 = ($$3 + $$10) / 12.0D;
			double $$12 = (double)($$6 - (float)$$4 + 0.33F);
			double $$13 = $$5 / 12.0D + 0.33000001311302185D;
			$$11 -= (double)(MathHelper.floor($$11 / 2048.0D) * 2048);
			$$13 -= (double)(MathHelper.floor($$13 / 2048.0D) * 2048);
			float $$14 = (float)($$11 - (double)MathHelper.floor($$11));
			float $$15 = (float)($$12 / 4.0D - (double)MathHelper.floor($$12 / 4.0D)) * 4.0F;
			float $$16 = (float)($$13 - (double)MathHelper.floor($$13));
			Vec3d $$17 = this.world.getCloudsColor(tickDelta);
			int $$18 = (int)Math.floor($$11);
			int $$19 = (int)Math.floor($$12 / 4.0D);
			int $$20 = (int)Math.floor($$13);
			if ($$18 != this.lastCloudsBlockX || $$19 != this.lastCloudsBlockY || $$20 != this.lastCloudsBlockZ || this.client.options.getCloudRenderMode() != this.lastCloudsRenderMode || this.lastCloudsColor.squaredDistanceTo($$17) > 2.0E-4D) {
				this.lastCloudsBlockX = $$18;
				this.lastCloudsBlockY = $$19;
				this.lastCloudsBlockZ = $$20;
				this.lastCloudsColor = $$17;
				this.lastCloudsRenderMode = this.client.options.getCloudRenderMode();
				this.cloudsDirty = true;
			}

			if (this.cloudsDirty) {
				this.cloudsDirty = false;
				BufferBuilder $$21 = Tessellator.getInstance().getBuffer();
				if (this.cloudsBuffer != null) {
					this.cloudsBuffer.close();
				}

				this.cloudsBuffer = new VertexBuffer();
				this.renderClouds($$21, $$11, $$12, $$13, $$17);
				$$21.end();
				this.cloudsBuffer.upload($$21);
			}

			RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
			RenderSystem.setShaderTexture(0, CLOUDS);
			BackgroundRenderer.setFogBlack();
			matrices.push();
			matrices.scale(12.0F, 1.0F, 12.0F);
			matrices.translate((double)(-$$14), (double)$$15, (double)(-$$16));
			if (this.cloudsBuffer != null) {
				int $$22 = this.lastCloudsRenderMode == CloudRenderMode.FANCY ? 0 : 1;

				for(int $$23 = $$22; $$23 < 2; ++$$23) {
					if ($$23 == 0) {
						RenderSystem.colorMask(false, false, false, false);
					} else {
						RenderSystem.colorMask(true, true, true, true);
					}

					Shader $$24 = RenderSystem.getShader();
					this.cloudsBuffer.setShader(matrices.peek().getPositionMatrix(), projectionMatrix, $$24);
				}
			}

			matrices.pop();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.enableCull();
			RenderSystem.disableBlend();
		}
	}

	private void renderClouds(BufferBuilder builder, double x, double y, double z, Vec3d color) {
		float $$10 = (float)MathHelper.floor(x) * 0.00390625F;
		float $$11 = (float)MathHelper.floor(z) * 0.00390625F;
		float $$12 = (float)color.x;
		float $$13 = (float)color.y;
		float $$14 = (float)color.z;
		float $$15 = $$12 * 0.9F;
		float $$16 = $$13 * 0.9F;
		float $$17 = $$14 * 0.9F;
		float $$18 = $$12 * 0.7F;
		float $$19 = $$13 * 0.7F;
		float $$20 = $$14 * 0.7F;
		float $$21 = $$12 * 0.8F;
		float $$22 = $$13 * 0.8F;
		float $$23 = $$14 * 0.8F;
		RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
		builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR_NORMAL);
		float $$24 = (float)Math.floor(y / 4.0D) * 4.0F;
		if (this.lastCloudsRenderMode == CloudRenderMode.FANCY) {
			for(int $$25 = -3; $$25 <= 4; ++$$25) {
				for(int $$26 = -3; $$26 <= 4; ++$$26) {
					float $$27 = (float)($$25 * 8);
					float $$28 = (float)($$26 * 8);
					if ($$24 > -5.0F) {
						builder.vertex((double)($$27 + 0.0F), (double)($$24 + 0.0F), (double)($$28 + 8.0F)).texture(($$27 + 0.0F) * 0.00390625F + $$10, ($$28 + 8.0F) * 0.00390625F + $$11).color($$18, $$19, $$20, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
						builder.vertex((double)($$27 + 8.0F), (double)($$24 + 0.0F), (double)($$28 + 8.0F)).texture(($$27 + 8.0F) * 0.00390625F + $$10, ($$28 + 8.0F) * 0.00390625F + $$11).color($$18, $$19, $$20, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
						builder.vertex((double)($$27 + 8.0F), (double)($$24 + 0.0F), (double)($$28 + 0.0F)).texture(($$27 + 8.0F) * 0.00390625F + $$10, ($$28 + 0.0F) * 0.00390625F + $$11).color($$18, $$19, $$20, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
						builder.vertex((double)($$27 + 0.0F), (double)($$24 + 0.0F), (double)($$28 + 0.0F)).texture(($$27 + 0.0F) * 0.00390625F + $$10, ($$28 + 0.0F) * 0.00390625F + $$11).color($$18, $$19, $$20, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
					}

					if ($$24 <= 5.0F) {
						builder.vertex((double)($$27 + 0.0F), (double)($$24 + 4.0F - 9.765625E-4F), (double)($$28 + 8.0F)).texture(($$27 + 0.0F) * 0.00390625F + $$10, ($$28 + 8.0F) * 0.00390625F + $$11).color($$12, $$13, $$14, 0.8F).normal(0.0F, 1.0F, 0.0F).next();
						builder.vertex((double)($$27 + 8.0F), (double)($$24 + 4.0F - 9.765625E-4F), (double)($$28 + 8.0F)).texture(($$27 + 8.0F) * 0.00390625F + $$10, ($$28 + 8.0F) * 0.00390625F + $$11).color($$12, $$13, $$14, 0.8F).normal(0.0F, 1.0F, 0.0F).next();
						builder.vertex((double)($$27 + 8.0F), (double)($$24 + 4.0F - 9.765625E-4F), (double)($$28 + 0.0F)).texture(($$27 + 8.0F) * 0.00390625F + $$10, ($$28 + 0.0F) * 0.00390625F + $$11).color($$12, $$13, $$14, 0.8F).normal(0.0F, 1.0F, 0.0F).next();
						builder.vertex((double)($$27 + 0.0F), (double)($$24 + 4.0F - 9.765625E-4F), (double)($$28 + 0.0F)).texture(($$27 + 0.0F) * 0.00390625F + $$10, ($$28 + 0.0F) * 0.00390625F + $$11).color($$12, $$13, $$14, 0.8F).normal(0.0F, 1.0F, 0.0F).next();
					}

					int $$32;
					if ($$25 > -1) {
						for($$32 = 0; $$32 < 8; ++$$32) {
							builder.vertex((double)($$27 + (float)$$32 + 0.0F), (double)($$24 + 0.0F), (double)($$28 + 8.0F)).texture(($$27 + (float)$$32 + 0.5F) * 0.00390625F + $$10, ($$28 + 8.0F) * 0.00390625F + $$11).color($$15, $$16, $$17, 0.8F).normal(-1.0F, 0.0F, 0.0F).next();
							builder.vertex((double)($$27 + (float)$$32 + 0.0F), (double)($$24 + 4.0F), (double)($$28 + 8.0F)).texture(($$27 + (float)$$32 + 0.5F) * 0.00390625F + $$10, ($$28 + 8.0F) * 0.00390625F + $$11).color($$15, $$16, $$17, 0.8F).normal(-1.0F, 0.0F, 0.0F).next();
							builder.vertex((double)($$27 + (float)$$32 + 0.0F), (double)($$24 + 4.0F), (double)($$28 + 0.0F)).texture(($$27 + (float)$$32 + 0.5F) * 0.00390625F + $$10, ($$28 + 0.0F) * 0.00390625F + $$11).color($$15, $$16, $$17, 0.8F).normal(-1.0F, 0.0F, 0.0F).next();
							builder.vertex((double)($$27 + (float)$$32 + 0.0F), (double)($$24 + 0.0F), (double)($$28 + 0.0F)).texture(($$27 + (float)$$32 + 0.5F) * 0.00390625F + $$10, ($$28 + 0.0F) * 0.00390625F + $$11).color($$15, $$16, $$17, 0.8F).normal(-1.0F, 0.0F, 0.0F).next();
						}
					}

					if ($$25 <= 1) {
						for($$32 = 0; $$32 < 8; ++$$32) {
							builder.vertex((double)($$27 + (float)$$32 + 1.0F - 9.765625E-4F), (double)($$24 + 0.0F), (double)($$28 + 8.0F)).texture(($$27 + (float)$$32 + 0.5F) * 0.00390625F + $$10, ($$28 + 8.0F) * 0.00390625F + $$11).color($$15, $$16, $$17, 0.8F).normal(1.0F, 0.0F, 0.0F).next();
							builder.vertex((double)($$27 + (float)$$32 + 1.0F - 9.765625E-4F), (double)($$24 + 4.0F), (double)($$28 + 8.0F)).texture(($$27 + (float)$$32 + 0.5F) * 0.00390625F + $$10, ($$28 + 8.0F) * 0.00390625F + $$11).color($$15, $$16, $$17, 0.8F).normal(1.0F, 0.0F, 0.0F).next();
							builder.vertex((double)($$27 + (float)$$32 + 1.0F - 9.765625E-4F), (double)($$24 + 4.0F), (double)($$28 + 0.0F)).texture(($$27 + (float)$$32 + 0.5F) * 0.00390625F + $$10, ($$28 + 0.0F) * 0.00390625F + $$11).color($$15, $$16, $$17, 0.8F).normal(1.0F, 0.0F, 0.0F).next();
							builder.vertex((double)($$27 + (float)$$32 + 1.0F - 9.765625E-4F), (double)($$24 + 0.0F), (double)($$28 + 0.0F)).texture(($$27 + (float)$$32 + 0.5F) * 0.00390625F + $$10, ($$28 + 0.0F) * 0.00390625F + $$11).color($$15, $$16, $$17, 0.8F).normal(1.0F, 0.0F, 0.0F).next();
						}
					}

					if ($$26 > -1) {
						for($$32 = 0; $$32 < 8; ++$$32) {
							builder.vertex((double)($$27 + 0.0F), (double)($$24 + 4.0F), (double)($$28 + (float)$$32 + 0.0F)).texture(($$27 + 0.0F) * 0.00390625F + $$10, ($$28 + (float)$$32 + 0.5F) * 0.00390625F + $$11).color($$21, $$22, $$23, 0.8F).normal(0.0F, 0.0F, -1.0F).next();
							builder.vertex((double)($$27 + 8.0F), (double)($$24 + 4.0F), (double)($$28 + (float)$$32 + 0.0F)).texture(($$27 + 8.0F) * 0.00390625F + $$10, ($$28 + (float)$$32 + 0.5F) * 0.00390625F + $$11).color($$21, $$22, $$23, 0.8F).normal(0.0F, 0.0F, -1.0F).next();
							builder.vertex((double)($$27 + 8.0F), (double)($$24 + 0.0F), (double)($$28 + (float)$$32 + 0.0F)).texture(($$27 + 8.0F) * 0.00390625F + $$10, ($$28 + (float)$$32 + 0.5F) * 0.00390625F + $$11).color($$21, $$22, $$23, 0.8F).normal(0.0F, 0.0F, -1.0F).next();
							builder.vertex((double)($$27 + 0.0F), (double)($$24 + 0.0F), (double)($$28 + (float)$$32 + 0.0F)).texture(($$27 + 0.0F) * 0.00390625F + $$10, ($$28 + (float)$$32 + 0.5F) * 0.00390625F + $$11).color($$21, $$22, $$23, 0.8F).normal(0.0F, 0.0F, -1.0F).next();
						}
					}

					if ($$26 <= 1) {
						for($$32 = 0; $$32 < 8; ++$$32) {
							builder.vertex((double)($$27 + 0.0F), (double)($$24 + 4.0F), (double)($$28 + (float)$$32 + 1.0F - 9.765625E-4F)).texture(($$27 + 0.0F) * 0.00390625F + $$10, ($$28 + (float)$$32 + 0.5F) * 0.00390625F + $$11).color($$21, $$22, $$23, 0.8F).normal(0.0F, 0.0F, 1.0F).next();
							builder.vertex((double)($$27 + 8.0F), (double)($$24 + 4.0F), (double)($$28 + (float)$$32 + 1.0F - 9.765625E-4F)).texture(($$27 + 8.0F) * 0.00390625F + $$10, ($$28 + (float)$$32 + 0.5F) * 0.00390625F + $$11).color($$21, $$22, $$23, 0.8F).normal(0.0F, 0.0F, 1.0F).next();
							builder.vertex((double)($$27 + 8.0F), (double)($$24 + 0.0F), (double)($$28 + (float)$$32 + 1.0F - 9.765625E-4F)).texture(($$27 + 8.0F) * 0.00390625F + $$10, ($$28 + (float)$$32 + 0.5F) * 0.00390625F + $$11).color($$21, $$22, $$23, 0.8F).normal(0.0F, 0.0F, 1.0F).next();
							builder.vertex((double)($$27 + 0.0F), (double)($$24 + 0.0F), (double)($$28 + (float)$$32 + 1.0F - 9.765625E-4F)).texture(($$27 + 0.0F) * 0.00390625F + $$10, ($$28 + (float)$$32 + 0.5F) * 0.00390625F + $$11).color($$21, $$22, $$23, 0.8F).normal(0.0F, 0.0F, 1.0F).next();
						}
					}
				}
			}
		} else {
		

			for(int $$35 = -32; $$35 < 32; $$35 += 32) {
				for(int $$36 = -32; $$36 < 32; $$36 += 32) {
					builder.vertex((double)($$35 + 0), (double)$$24, (double)($$36 + 32)).texture((float)($$35 + 0) * 0.00390625F + $$10, (float)($$36 + 32) * 0.00390625F + $$11).color($$12, $$13, $$14, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
					builder.vertex((double)($$35 + 32), (double)$$24, (double)($$36 + 32)).texture((float)($$35 + 32) * 0.00390625F + $$10, (float)($$36 + 32) * 0.00390625F + $$11).color($$12, $$13, $$14, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
					builder.vertex((double)($$35 + 32), (double)$$24, (double)($$36 + 0)).texture((float)($$35 + 32) * 0.00390625F + $$10, (float)($$36 + 0) * 0.00390625F + $$11).color($$12, $$13, $$14, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
					builder.vertex((double)($$35 + 0), (double)$$24, (double)($$36 + 0)).texture((float)($$35 + 0) * 0.00390625F + $$10, (float)($$36 + 0) * 0.00390625F + $$11).color($$12, $$13, $$14, 0.8F).normal(0.0F, -1.0F, 0.0F).next();
				}
			}
		}

	}

	private void updateChunks(Camera camera) {
		this.client.getProfiler().push("populate_chunks_to_compile");
		ChunkRendererRegionBuilder $$1 = new ChunkRendererRegionBuilder();
		BlockPos $$2 = camera.getBlockPos();
		List<ChunkBuilder.BuiltChunk> $$3 = Lists.newArrayList();
		ObjectListIterator var5 = this.chunkInfos.iterator();

		while(true) {
			ChunkBuilder.BuiltChunk $$5;
			ChunkPos $$6;
			do {
				do {
					if (!var5.hasNext()) {
						this.client.getProfiler().swap("upload");
						this.chunkBuilder.upload();
						this.client.getProfiler().swap("schedule_async_compile");
						Iterator var11 = $$3.iterator();

						while(var11.hasNext()) {
							ChunkBuilder.BuiltChunk $$9 = (ChunkBuilder.BuiltChunk)var11.next();
							$$9.scheduleRebuild(this.chunkBuilder, $$1);
							$$9.cancelRebuild();
						}

						this.client.getProfiler().pop();
						return;
					}

					PreviewRenderer.ChunkInfo $$4 = (PreviewRenderer.ChunkInfo)var5.next();
					$$5 = $$4.chunk;
					$$6 = new ChunkPos($$5.getOrigin());
				} while(!$$5.needsRebuild());
			} while(!this.world.getChunk($$6.x, $$6.z).shouldRenderOnUpdate());

			boolean $$7 = false;
			if (this.client.options.chunkBuilderMode != ChunkBuilderMode.NEARBY) {
				if (this.client.options.chunkBuilderMode == ChunkBuilderMode.PLAYER_AFFECTED) {
					$$7 = $$5.needsImportantRebuild();
				}
			} else {
				BlockPos $$8 = $$5.getOrigin().add(8, 8, 8);
				$$7 = $$8.getSquaredDistance($$2) < 768.0D || $$5.needsImportantRebuild();
			}

			if ($$7) {
				this.client.getProfiler().push("build_near_sync");
				this.chunkBuilder.rebuild($$5, $$1);
				this.client.getProfiler().pop();
			} else {
				$$3.add($$5);
			}
		}
	}

	private static void drawShapeOutline(MatrixStack matrices, VertexConsumer $$1, VoxelShape $$2, double $$3, double $$4, double $$5, float $$6, float $$7, float $$8, float $$9) {
		MatrixStack.Entry $$10 = matrices.peek();
		$$2.forEachEdge(($$9x, $$10x, $$11, $$12, $$13, $$14) -> {
			float $$15 = (float)($$12 - $$9x);
			float $$16 = (float)($$13 - $$10x);
			float $$17 = (float)($$14 - $$11);
			float $$18 = MathHelper.sqrt($$15 * $$15 + $$16 * $$16 + $$17 * $$17);
			$$15 /= $$18;
			$$16 /= $$18;
			$$17 /= $$18;
			$$1.vertex($$10.getPositionMatrix(), (float)($$9x + $$3), (float)($$10x + $$4), (float)($$11 + $$5)).color($$6, $$7, $$8, $$9).normal($$10.getNormalMatrix(), $$15, $$16, $$17).next();
			$$1.vertex($$10.getPositionMatrix(), (float)($$12 + $$3), (float)($$13 + $$4), (float)($$14 + $$5)).color($$6, $$7, $$8, $$9).normal($$10.getNormalMatrix(), $$15, $$16, $$17).next();
		});
	}
	


	


	


	private void scheduleChunkRender(int x, int y, int z, boolean important) {
		this.chunks.scheduleRebuild(x, y, z, important);
	}

	



	public void scheduleTerrainUpdate() {
		this.field_34810 = true;
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
			int $$3 = world.getLightLevel(LightType.SKY, pos);
			int $$4 = world.getLightLevel(LightType.BLOCK, pos);
			int $$5 = state.getLuminance();
			if ($$4 < $$5) {
				$$4 = $$5;
			}

			return $$3 << 20 | $$4 << 4;
		}
	}

	
	@Environment(EnvType.CLIENT)
	public static class ShaderException extends RuntimeException {
		public ShaderException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	@Environment(EnvType.CLIENT)
	private static class class_6600 {
		public final PreviewRenderer.ChunkInfoList field_34818;
		public final LinkedHashSet<PreviewRenderer.ChunkInfo> field_34819;

		public class_6600(int $$0) {
			this.field_34818 = new PreviewRenderer.ChunkInfoList($$0);
			this.field_34819 = new LinkedHashSet($$0);
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

		public int hashCode() {
			return this.chunk.getOrigin().hashCode();
		}

		public boolean equals(Object $$0) {
			if (!($$0 instanceof PreviewRenderer.ChunkInfo)) {
				return false;
			} else {
				PreviewRenderer.ChunkInfo $$1 = (PreviewRenderer.ChunkInfo)$$0;
				return this.chunk.getOrigin().equals($$1.chunk.getOrigin());
			}
		}
	}

	@Environment(EnvType.CLIENT)
	private static class ChunkInfoList {
		private final PreviewRenderer.ChunkInfo[] current;

		ChunkInfoList(int size) {
			this.current = new PreviewRenderer.ChunkInfo[size];
		}

		public void setInfo(ChunkBuilder.BuiltChunk chunk, PreviewRenderer.ChunkInfo info) {
			this.current[chunk.index] = info;
		}

		@Nullable
		public PreviewRenderer.ChunkInfo getInfo(ChunkBuilder.BuiltChunk chunk) {
			int $$1 = chunk.index;
			return $$1 >= 0 && $$1 < this.current.length ? this.current[$$1] : null;
		}
	}
}
