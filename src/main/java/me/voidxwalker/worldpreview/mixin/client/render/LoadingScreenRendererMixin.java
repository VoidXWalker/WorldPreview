package me.voidxwalker.worldpreview.mixin.client.render;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.mixin.access.MinecraftClientMixin;
import me.voidxwalker.worldpreview.mixin.access.ScreenMixin;
import me.voidxwalker.worldpreview.mixin.access.WorldRendererMixin;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
//import net.minecraft.client.gui.WorldGenerationProgressTracker;
//import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.class_321;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.screen.Screen;
//import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
//import net.minecraft.client.options.CloudRenderMode;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.*;
import net.minecraft.client.render.debug.CameraView;
import net.minecraft.client.render.debug.StructureDebugRenderer;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.AnError;
import net.minecraft.client.util.Clipper;
import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
//import net.minecraft.fluid.FluidState;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.Level;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.Project;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.FloatBuffer;

@Mixin(LoadingScreenRenderer.class)
public abstract class LoadingScreenRendererMixin {
    @Shadow private MinecraftClient field_1029;

    @Shadow private long field_1031;

    @Shadow private String field_1030;

    @Shadow private String field_1028;

    @Shadow private boolean field_1032;

    @Shadow private Framebuffer field_7696;

    /**
     * @author Pixfumy
     * @reason render the generating world instead of the default background
     */
    @Overwrite
    public void progressStagePercentage(int percentage) {
        if (!((MinecraftClientMixin)this.field_1029).getRunning()) {
            if (!this.field_1032) {
                throw new AnError();
            }
        } else {
            long l = MinecraftClient.getTime();
            if (l - this.field_1031 >= 100L) {
                Window window = new Window(this.field_1029);
                int i = window.getScaleFactor();
                int j = window.getWidth();
                int k = window.getHeight();
                if (WorldPreview.worldRenderer == null) {
                    WorldPreview.worldRenderer = new WorldRenderer(MinecraftClient.getInstance());
                }
                if (WorldPreview.world != null && WorldPreview.clientWorld != null && WorldPreview.player != null && !WorldPreview.freezePreview) {
                    if (((me.voidxwalker.worldpreview.mixin.access.WorldRendererMixin) WorldPreview.worldRenderer).getWorld() == null && WorldPreview.calculatedSpawn) {
                        WorldPreview.worldRenderer.method_1371(WorldPreview.clientWorld);
                    }
                    if (((WorldRendererMixin) WorldPreview.worldRenderer).getWorld() != null) {
                        KeyBinding.unpressAll();
                        WorldPreview.kill = 0;
//                  if (WorldPreview.camera == null) {
//                      WorldPreview.player.refreshPositionAndAngles(WorldPreview.player.x, WorldPreview.player.y+ 1+(WorldPreview.player.getBoundingBox().y2-WorldPreview.player.getBoundingBox().y1), WorldPreview.player.z, 0.0F, 0.0F);
//                      WorldPreview.camera = new Camera();
//                      WorldPreview.camera.update(WorldPreview.world, WorldPreview.player, this.minecraft.options.perspective > 0, this.minecraft.options.perspective == 2, 0.2F);
//                      WorldPreview.player.refreshPositionAndAngles(WorldPreview.player.x, WorldPreview.player.y - 1.5, WorldPreview.player.z, 0.0F, 0.0F);
//                      WorldPreview.inPreview=true;
//                      WorldPreview.log(Level.INFO,"Starting Preview at ("+ WorldPreview.player.x + ", "+(double)Math.floor(WorldPreview.player.y)+ ", "+ WorldPreview.player.z+")");
//                  }
                        WorldPreview.inPreview=true;
                        renderWorld(0, (long) (1000000000 / 60 / 4));
                        GlStateManager.clear(256);
                        GlStateManager.matrixMode(5889);
                        GlStateManager.loadIdentity();
                        GlStateManager.ortho(0.0D, (double) window.getScaledWidth(), (double) window.getScaledWidth(), 0.0D, 1000.0D, 3000.0D);
                        GlStateManager.matrixMode(5888);
                        GlStateManager.loadIdentity();
                        GlStateManager.translatef(0.0F, 0.0F, -2000.0F);
                        if (!GLX.supportsFbo()) {
                            GlStateManager.clear(16640);
                        }
                        GlStateManager.enableBlend();
                        GlStateManager.blendFuncSeparate(770, 771, 1, 0);
                        this.field_1029.textRenderer.drawWithShadow(this.field_1030, (float)((j - this.field_1029.textRenderer.getStringWidth(this.field_1030)) / 2), (float)(k / 2 - 4 - 16), 16777215);
                        this.field_1029.textRenderer.drawWithShadow(this.field_1028, (float)((j - this.field_1029.textRenderer.getStringWidth(this.field_1028)) / 2), (float)(k / 2 - 4 + 8), 16777215);
                        this.field_7696.endWrite();
                        if (GLX.supportsFbo()) {
                            this.field_7696.draw(j * i, k * i);
                        }

                        this.field_1029.updateDisplay();

                        try {
                            Thread.yield();
                        } catch (Exception var15) {
                        }
                        //this.renderPauseMenu(mouseX,mouseY,delta);
                    }
                }
            }
        }
    }

    @Inject(method = "<init>",at = @At(value = "TAIL"))
    public void worldpreview_init(MinecraftClient client, CallbackInfo ci){
        WorldPreview.freezePreview=false;
        WorldPreview.calculatedSpawn=false;
        KeyBinding.unpressAll();
    }

    private void renderWorld(float tickDelta, long endTime) {
        GlStateManager.enableDepthTest();
        GlStateManager.enableAlphaTest();
        GlStateManager.alphaFunc(516, 0.5F);
        this.renderWorld(2, tickDelta, endTime);
    }

    private void renderWorld(int anaglyphFilter, float tickDelta, long limitTime) {
        WorldRenderer worldRenderer = WorldPreview.worldRenderer;
        ParticleManager particleManager = this.field_1029.particleManager;
        GlStateManager.enableCull();
        this.field_1029.profiler.swap("clear");
        GlStateManager.viewPort(0, 0, this.field_1029.width, this.field_1029.height);
        GlStateManager.clear(16640);
        this.field_1029.profiler.swap("camera");
        this.setupCamera(tickDelta, anaglyphFilter);
        class_321.method_804(WorldPreview.player, this.field_1029.options.perspective == 2);
        this.field_1029.profiler.swap("frustum");
        Clipper.getInstance();
        this.field_1029.profiler.swap("culling");
        CameraView cameraView = new StructureDebugRenderer();
        Entity entity = WorldPreview.player;
        double d = entity.prevTickX + (entity.x - entity.prevTickX) * (double) tickDelta;
        double e = entity.prevTickY + (entity.y - entity.prevTickY) * (double) tickDelta;
        double f = entity.prevTickZ + (entity.z - entity.prevTickZ) * (double) tickDelta;
        cameraView.setPos(d, e, f);
        if (this.field_1029.options.viewDistance >= 4) {
            this.renderFog(-1, tickDelta);
            this.field_1029.profiler.swap("sky");
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective(this.field_1029.options.fov, (float)this.field_1029.width / (float)this.field_1029.height, 0.05F, this.field_1029.options.viewDistance * 2.0F);
            GlStateManager.matrixMode(5888);
            worldRenderer.method_9891(tickDelta, anaglyphFilter);
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective(this.field_1029.options.fov, (float)this.field_1029.width / (float)this.field_1029.height, 0.05F, this.field_1029.options.viewDistance * MathHelper.SQUARE_ROOT_OF_TWO);
            GlStateManager.matrixMode(5888);
        }

        this.renderFog(0, tickDelta);
        GlStateManager.shadeModel(7425);
//        if (entity.y + (double)entity.getEyeHeight() < 128.0D) {
//            this.renderClouds(worldRenderer, tickDelta, anaglyphFilter);
//        }
//
//        this.field_1029.profiler.swap("prepareterrain");
//        this.renderFog(0, tickDelta);
//        this.field_1029.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
//        GuiLighting.disable();
//        this.field_1029.profiler.swap("terrain_setup");
//        worldRenderer.method_9906(entity, (double)tickDelta, cameraView, this.frameCount++, this.field_1029.player.isSpectator());
//        if (anaglyphFilter == 0 || anaglyphFilter == 2) {
//            this.field_1029.profiler.swap("updatechunks");
//            this.field_1029.worldRenderer.method_9892(limitTime);
//        }
//
//        this.field_1029.profiler.swap("terrain");
//        GlStateManager.matrixMode(5888);
//        GlStateManager.pushMatrix();
//        GlStateManager.disableAlphaTest();
//        worldRenderer.method_9894(RenderLayer.SOLID, (double)tickDelta, anaglyphFilter, entity);
//        GlStateManager.enableAlphaTest();
//        worldRenderer.method_9894(RenderLayer.CUTOUT_MIPPED, (double)tickDelta, anaglyphFilter, entity);
//        this.field_1029.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).pushFilter(false, false);
//        worldRenderer.method_9894(RenderLayer.CUTOUT, (double)tickDelta, anaglyphFilter, entity);
//        this.field_1029.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).pop();
//        GlStateManager.shadeModel(7424);
//        GlStateManager.alphaFunc(516, 0.1F);
//        PlayerEntity playerEntity2;
//        GlStateManager.matrixMode(5888);
//        GlStateManager.popMatrix();
//        if (bl && this.field_1029.result != null && !entity.isSubmergedIn(Material.WATER)) {
//            playerEntity2 = (PlayerEntity)entity;
//            GlStateManager.disableAlphaTest();
//            this.field_1029.profiler.swap("outline");
//            worldRenderer.method_1380(playerEntity2, this.field_1029.result, 0, tickDelta);
//            GlStateManager.enableAlphaTest();
//        }
//
//        this.field_1029.profiler.swap("destroyProgress");
//        GlStateManager.enableBlend();
//        GlStateManager.blendFuncSeparate(770, 1, 1, 0);
//        this.field_1029.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).pushFilter(false, false);
//        worldRenderer.method_9899(Tessellator.getInstance(), Tessellator.getInstance().getBuffer(), entity, tickDelta);
//        this.field_1029.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).pop();
//        GlStateManager.disableBlend();
//        GlStateManager.depthMask(false);
//        GlStateManager.enableCull();
//        this.field_1029.profiler.swap("weather");
//        this.renderWeather(tickDelta);
//        GlStateManager.depthMask(true);
//        worldRenderer.method_9907(entity, tickDelta);
//        GlStateManager.disableBlend();
//        GlStateManager.enableCull();
//        GlStateManager.blendFuncSeparate(770, 771, 1, 0);
//        GlStateManager.alphaFunc(516, 0.1F);
//        this.renderFog(0, tickDelta);
//        GlStateManager.enableBlend();
//        GlStateManager.depthMask(false);
//        this.field_1029.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
//        GlStateManager.shadeModel(7425);
//        this.field_1029.profiler.swap("translucent");
//        worldRenderer.method_9894(RenderLayer.TRANSLUCENT, (double)tickDelta, anaglyphFilter, entity);
//        GlStateManager.shadeModel(7424);
//        GlStateManager.depthMask(true);
//        GlStateManager.enableCull();
//        GlStateManager.disableBlend();
//        GlStateManager.disableFog();
//        if (entity.y + (double)entity.getEyeHeight() >= 128.0D) {
//            this.field_1029.profiler.swap("aboveClouds");
//            this.renderClouds(worldRenderer, tickDelta, anaglyphFilter);
//        }
    }

    private void setupCamera(float tickDelta, int anaglyphFilter) {
        float viewDistance = this.field_1029.options.viewDistance * 16;
        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();
        float f = 0.07F;
        Project.gluPerspective(this.field_1029.options.fov, (float)this.field_1029.width / (float)this.field_1029.height, 0.05F, viewDistance * MathHelper.SQUARE_ROOT_OF_TWO);
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();
        Entity entity = WorldPreview.player;
        f = entity.getEyeHeight();
        double d = entity.x;
        double e = entity.y + (double)f;
        double g = entity.z;
        if (entity instanceof LivingEntity && ((LivingEntity)entity).isSleeping()) {
            f = (float)((double)f + 1.0D);
            GlStateManager.translatef(0.0F, 0.3F, 0.0F);
            if (!this.field_1029.options.field_955) {
                BlockPos blockPos = new BlockPos(entity);
                BlockState blockState = WorldPreview.clientWorld.getBlockState(blockPos);
                Block block = blockState.getBlock();
                if (block == Blocks.BED) {
                    int i = ((Direction)blockState.get(BedBlock.FACING)).getHorizontal();
                    GlStateManager.rotatef((float)(i * 90), 0.0F, 1.0F, 0.0F);
                }

                GlStateManager.rotatef(entity.prevYaw + (entity.yaw - entity.prevYaw) * tickDelta + 180.0F, 0.0F, -1.0F, 0.0F);
                GlStateManager.rotatef(entity.prevPitch + (entity.pitch - entity.prevPitch) * tickDelta, -1.0F, 0.0F, 0.0F);
            }
        } else if (this.field_1029.options.perspective > 0) {
            double h = (double)(4.0 + (4.0 - 4.0) * tickDelta);
            if (this.field_1029.options.field_955) {
                GlStateManager.translatef(0.0F, 0.0F, (float)(-h));
            } else {
                float j = entity.yaw;
                float k = entity.pitch;
                if (this.field_1029.options.perspective == 2) {
                    k += 180.0F;
                }

                double l = (double)(-MathHelper.sin(j / 180.0F * 3.1415927F) * MathHelper.cos(k / 180.0F * 3.1415927F)) * h;
                double m = (double)(MathHelper.cos(j / 180.0F * 3.1415927F) * MathHelper.cos(k / 180.0F * 3.1415927F)) * h;
                double n = (double)(-MathHelper.sin(k / 180.0F * 3.1415927F)) * h;

                for(int o = 0; o < 8; ++o) {
                    float p = (float)((o & 1) * 2 - 1);
                    float q = (float)((o >> 1 & 1) * 2 - 1);
                    float r = (float)((o >> 2 & 1) * 2 - 1);
                    p *= 0.1F;
                    q *= 0.1F;
                    r *= 0.1F;
                    HitResult hitResult = WorldPreview.clientWorld.rayTrace(new Vec3d(d + (double)p, e + (double)q, g + (double)r), new Vec3d(d - l + (double)p + (double)r, e - n + (double)q, g - m + (double)r));
                    if (hitResult != null) {
                        double s = hitResult.pos.distanceTo(new Vec3d(d, e, g));
                        if (s < h) {
                            h = s;
                        }
                    }
                }
                if (this.field_1029.options.perspective == 2) {
                    GlStateManager.rotatef(180.0F, 0.0F, 1.0F, 0.0F);
                }
                GlStateManager.rotatef(entity.pitch - k, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotatef(entity.yaw - j, 0.0F, 1.0F, 0.0F);
                GlStateManager.translatef(0.0F, 0.0F, (float)(-h));
                GlStateManager.rotatef(j - entity.yaw, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotatef(k - entity.pitch, 1.0F, 0.0F, 0.0F);
            }
        } else {
            GlStateManager.translatef(0.0F, 0.0F, -0.1F);
        }
    }

    private void renderFog(int i, float tickDelta) {
        Entity entity = WorldPreview.player;
        boolean bl = false;
        if (entity instanceof PlayerEntity) {
            bl = ((PlayerEntity)entity).abilities.creativeMode;
        }
        FloatBuffer fogColorBuffer = GlAllocationUtils.allocateFloatBuffer(16);
        fogColorBuffer.clear();
        fogColorBuffer.put(255.0F).put(255.0F).put(255.0F).put(1.0F);
        fogColorBuffer.flip();
        GL11.glFog(2918, fogColorBuffer);
        GL11.glNormal3f(0.0F, -1.0F, 0.0F);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        Block block = class_321.method_9371(WorldPreview.clientWorld, entity, tickDelta);
        float g;
        if (entity instanceof LivingEntity && ((LivingEntity)entity).hasStatusEffect(StatusEffect.BLINDNESS)) {
            g = 5.0F;
            int j = ((LivingEntity)entity).getEffectInstance(StatusEffect.BLINDNESS).getDuration();
            if (j < 20) {
                g = 5.0F + (this.field_1029.options.viewDistance - 5.0F) * (1.0F - (float)j / 20.0F);
            }

            GlStateManager.fogMode(9729);
            if (i == -1) {
                GlStateManager.fogStart(0.0F);
                GlStateManager.fogEnd(g * 0.8F);
            } else {
                GlStateManager.fogStart(g * 0.25F);
                GlStateManager.fogEnd(g);
            }

            if (GLContext.getCapabilities().GL_NV_fog_distance) {
                GL11.glFogi(34138, 34139);
            }
        } else if (block.getMaterial() == Material.WATER) {
            GlStateManager.fogMode(2048);
            if (entity instanceof LivingEntity && ((LivingEntity)entity).hasStatusEffect(StatusEffect.WATER_BREATHING)) {
                GlStateManager.fogDensity(0.01F);
            } else {
                GlStateManager.fogDensity(0.1F - (float) EnchantmentHelper.method_8449(entity) * 0.03F);
            }
        } else if (block.getMaterial() == Material.LAVA) {
            GlStateManager.fogMode(2048);
            GlStateManager.fogDensity(2.0F);
        } else {
            g = this.field_1029.options.viewDistance;
            GlStateManager.fogMode(9729);
            if (i == -1) {
                GlStateManager.fogStart(0.0F);
                GlStateManager.fogEnd(g);
            } else {
                GlStateManager.fogStart(g * 0.75F);
                GlStateManager.fogEnd(g);
            }

            if (GLContext.getCapabilities().GL_NV_fog_distance) {
                GL11.glFogi(34138, 34139);
            }

            if (WorldPreview.clientWorld.dimension.isFogThick((int)entity.x, (int)entity.z)) {
                GlStateManager.fogStart(g * 0.05F);
                GlStateManager.fogEnd(Math.min(g, 192.0F) * 0.5F);
            }
        }

        GlStateManager.enableColorMaterial();
        GlStateManager.enableFog();
        GlStateManager.colorMaterial(1028, 4608);
    }

//    private void renderAboveClouds(Camera camera,  float tickDelta, double cameraX, double cameraY, double cameraZ) {
//        if (this.minecraft.options.getCloudRenderMode() != CloudRenderMode.OFF) {
//            this.minecraft.getProfiler().swap("clouds");
//            GlStateManager.matrixMode(5889);
//            GlStateManager.loadIdentity();
//            GlStateManager.multMatrix(Matrix4f.method_4929(minecraft.options.fov, (float)this.minecraft.window.getFramebufferWidth() / (float)this.minecraft.window.getFramebufferHeight(), 0.05F, minecraft.options.viewDistance *16 * 4.0F));
//            GlStateManager.matrixMode(5888);
//            GlStateManager.pushMatrix();
//           // this.backgroundRenderer.applyFog(camera, 0);
//            WorldPreview.worldRenderer.renderClouds(tickDelta, cameraX, cameraY, cameraZ);
//            GlStateManager.disableFog();
//            GlStateManager.popMatrix();
//            GlStateManager.matrixMode(5889);
//            GlStateManager.loadIdentity();
//            GlStateManager.multMatrix(Matrix4f.method_4929(minecraft.options.fov, (float)this.minecraft.window.getFramebufferWidth() / (float)this.minecraft.window.getFramebufferHeight(), 0.05F, minecraft.options.viewDistance *16* MathHelper.SQUARE_ROOT_OF_TWO));
//            GlStateManager.matrixMode(5888);
//        }
//    }

//    private void applyCameraTransformations(float tickDelta) {
//        GlStateManager.matrixMode(5889);
//        GlStateManager.loadIdentity();
//
//
//        GlStateManager.multMatrix(Matrix4f.method_4929(this.minecraft.options.fov, (float)this.minecraft.window.getFramebufferWidth() / (float)this.minecraft.window.getFramebufferHeight(), 0.05F, minecraft.options.viewDistance*16));
//        GlStateManager.matrixMode(5888);
//        GlStateManager.loadIdentity();
//    }

//    private void renderAboveClouds(Camera camera, WorldRenderer worldRenderer, float tickDelta, double cameraX, double cameraY, double cameraZ) {
//        if (this.minecraft.options.getCloudRenderMode() != CloudRenderMode.OFF) {
//            this.minecraft.getProfiler().swap("clouds");
//            GlStateManager.matrixMode(5889);
//            GlStateManager.loadIdentity();
//            GlStateManager.multMatrix(Matrix4f.method_4929(this.getFov(camera, tickDelta, true), (float)this.minecraft.window.getFramebufferWidth() / (float)this.minecraft.window.getFramebufferHeight(), 0.05F, this.minecraft.options.viewDistance * 16* 4.0F));
//            GlStateManager.matrixMode(5888);
//            GlStateManager.pushMatrix();
//            this.backgroundRenderer.applyFog(camera, 0);
//            worldRenderer.renderClouds(tickDelta, cameraX, cameraY, cameraZ);
//            GlStateManager.disableFog();
//            GlStateManager.popMatrix();
//            GlStateManager.matrixMode(5889);
//            GlStateManager.loadIdentity();
//            GlStateManager.multMatrix(Matrix4f.method_4929(this.getFov(camera, tickDelta, true), (float)this.minecraft.window.getFramebufferWidth() / (float)this.minecraft.window.getFramebufferHeight(), 0.05F, this.minecraft.options.viewDistance * 16 * MathHelper.SQUARE_ROOT_OF_TWO));
//            GlStateManager.matrixMode(5888);
//        }
//
//    }
//    private double getFov(Camera camera, float tickDelta, boolean changingFov) {
//
//          return this.minecraft.options.fov;
//
//    }
//    private void renderPauseMenu(int mouseX, int mouseY, float delta){
//        if(WorldPreview.showMenu){
//            Iterator<AbstractButtonWidget> iterator =this.buttons.listIterator();
//            while(iterator.hasNext()){
//                iterator.next().render(mouseX,mouseY,delta);
//            }
//        }
//        else {
//            this.drawCenteredString( minecraft.textRenderer, new TranslatableText("menu.paused").getString(), this.width / 2, 10, 16777215);
//        }
//    }
//
//    private void worldpreview_initWidgets(){
//        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 24 - 16, 204, 20, new TranslatableText("menu.returnToGame").getString(), (ignored) -> {}));
//        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 48 - 16, 98, 20, new TranslatableText("gui.advancements").getString(), (ignored) -> {}));
//        this.addButton(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 48 - 16, 98, 20, new TranslatableText("gui.stats").getString(), (ignored) -> {}));
//
//        this .addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 72 - 16, 98, 20, new TranslatableText("menu.sendFeedback").getString(), (ignored) -> {}));
//        this.addButton(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 72 - 16, 98, 20, new TranslatableText("menu.reportBugs").getString(), (ignored) -> {}));
//        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 96 - 16, 98, 20, new TranslatableText("menu.options").getString(), (ignored) -> {}));
//        this.addButton(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 96 - 16, 98, 20, new TranslatableText("menu.shareToLan").getString(), (ignored) -> {}));
//        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 120 - 16, 204, 20, new TranslatableText("menu.returnToMenu").getString(), (buttonWidgetX) -> {
//                WorldPreview.kill = -1;
//                buttonWidgetX.active = false;
//        }));
//    }
//
//    public void resize(MinecraftClient minecraft, int width, int height) {
//        this.init(minecraft, width, height);
//        this.worldpreview_initWidgets();
//    }
}
