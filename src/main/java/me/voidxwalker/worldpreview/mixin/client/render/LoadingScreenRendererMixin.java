package me.voidxwalker.worldpreview.mixin.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import me.voidxwalker.worldpreview.ChunkSetter;
import me.voidxwalker.worldpreview.WorldPreview;

import me.voidxwalker.worldpreview.mixin.access.WorldRendererMixin;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.class_321;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.render.GuiLighting;
import net.minecraft.client.render.LoadingScreenRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.debug.CameraView;
import net.minecraft.client.render.debug.StructureDebugRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.Clipper;
import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.client.util.Window;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.Project;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
     * @author
     */
    @Overwrite
    public void progressStagePercentage(int percentage) {

        if(WorldPreview.worldRenderer==null){

            WorldPreview.worldRenderer=new WorldRenderer(MinecraftClient.getInstance());
            ((ChunkSetter)WorldPreview.worldRenderer).setPreviewRenderer();
        }
        long l = MinecraftClient.getTime();
        if (l - this.field_1031 >= 100L) {
            Window window = new Window(this.field_1029);
            int i = window.getScaleFactor();
            int j = window.getWidth();
            int k = window.getHeight();
            if (WorldPreview.world != null && WorldPreview.clientWorld != null && WorldPreview.player != null && !WorldPreview.freezePreview) {
                if (((WorldRendererMixin) WorldPreview.worldRenderer).getWorld() == null && WorldPreview.calculatedSpawn) {
                    WorldPreview.worldRenderer.method_1371(WorldPreview.clientWorld);
                }
                if (((WorldRendererMixin) WorldPreview.worldRenderer).getWorld() != null) {
                    //this.renderBackground();
                    this.field_1842 = this.field_1843;
                    float h = WorldPreview.world.getBrightness(new BlockPos(WorldPreview.player));
                    float x = (float) this.field_1029.options.viewDistance / 32.0F;
                    float y = h * (1.0F - x) + x;
                    this.field_1843 += (y - this.field_1843) * 0.1F;
                    this.lastSkyDarkness = this.skyDarkness;
                    if (BossBar.darkenSky) {
                        this.skyDarkness += 0.05F;
                        if (this.skyDarkness > 1.0F) {
                            this.skyDarkness = 1.0F;
                        }
                        BossBar.darkenSky = false;
                    } else if (this.skyDarkness > 0.0F) {
                        this.skyDarkness -= 0.0125F;
                    }
                    renderWorld2(0.5F,(1000000000 / 60 / 4));
                    this.field_1029.updateDisplay();

                }
            }
        }
    }


    public void renderWorld2(float tickDelta, long endTime) {
        GlStateManager.enableDepthTest();
        GlStateManager.enableAlphaTest();
        GlStateManager.alphaFunc(516, 0.5F);
        this.renderCenter(tickDelta, endTime);

    }
    private void renderCenter(float tickDelta, long endTime) {
        WorldRenderer worldRenderer =  WorldPreview.worldRenderer;
        ( (ChunkSetter)worldRenderer).setPreviewRenderer();
        GlStateManager.enableCull();

        this.field_1029.profiler.swap("clear");
        GlStateManager.viewPort(0, 0, this.field_1029.width, this.field_1029.height);
        updateFog(tickDelta);
        GlStateManager.clear(16640);
        this.field_1029.profiler.swap("camera");
        setupCamera(tickDelta, 2);
        class_321.method_804((PlayerEntity) WorldPreview.player, this.field_1029.options.perspective == 2);
        Clipper.getInstance();
        this.field_1029.profiler.swap("culling");
        CameraView cameraView = new StructureDebugRenderer();
        Entity entity = WorldPreview.player;
        double d = entity.prevTickX + (entity.x - entity.prevTickX) * (double)tickDelta;
        double e = entity.prevTickY + (entity.y - entity.prevTickY) * (double)tickDelta;
        double f = entity.prevTickZ + (entity.z - entity.prevTickZ) * (double)tickDelta;
        cameraView.setPos(d, e, f);
        if (this.field_1029.options.viewDistance >= 4) {
            renderFog(-1, tickDelta);
            this.field_1029.profiler.swap("sky");
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective((float)  90.0F, (float)this.field_1029.width / (float)this.field_1029.height, 0.05F, (float)(this.field_1029.options.viewDistance * 16) * 2.0F);
            GlStateManager.matrixMode(5888);
            worldRenderer.method_9891(tickDelta, 2);
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective((float)  90.0F, (float)this.field_1029.width / (float)this.field_1029.height, 0.05F, (float)(this.field_1029.options.viewDistance * 16)* MathHelper.SQUARE_ROOT_OF_TWO);
            GlStateManager.matrixMode(5888);
        }

        renderFog(0, tickDelta);
        GlStateManager.shadeModel(7425);
        if (entity.y + (double)entity.getEyeHeight() < 128.0D) {
            renderClouds(worldRenderer, tickDelta, 2);
        }

        this.field_1029.profiler.swap("prepareterrain");
        renderFog(0, tickDelta);
        this.field_1029.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
        GuiLighting.disable();
        this.field_1029.profiler.swap("terrain_setup");
        worldRenderer.method_9906(entity, (double)tickDelta, cameraView,100, false);
        this.field_1029.profiler.swap("updatechunks");
        worldRenderer.method_9892(endTime);

        this.field_1029.profiler.swap("terrain");
        GlStateManager.matrixMode(5888);
        GlStateManager.pushMatrix();
        GlStateManager.disableAlphaTest();
        worldRenderer.method_9894(RenderLayer.SOLID, (double)tickDelta, 2, entity);
        GlStateManager.enableAlphaTest();
        worldRenderer.method_9894(RenderLayer.CUTOUT_MIPPED, (double)tickDelta, 2, entity);
        this.field_1029.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).pushFilter(false, false);
        worldRenderer.method_9894(RenderLayer.CUTOUT, (double)tickDelta, 2, entity);
        this.field_1029.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).pop();
        GlStateManager.shadeModel(7424);
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();


        this.field_1029.profiler.swap("translucent");
        worldRenderer.method_9894(RenderLayer.TRANSLUCENT, tickDelta, 2, entity);
        GlStateManager.shadeModel(7424);
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.disableFog();





    }
    private void updateFog(float tickDelta) {
        World world =WorldPreview.clientWorld;
        Entity entity = WorldPreview.player;
        float f = 0.25F + 0.75F * (float)this.field_1029.options.viewDistance / 32.0F;
        f = 1.0F - (float)Math.pow((double)f, 0.25D);
        Vec3d vec3d = world.method_3631(WorldPreview.player, tickDelta);
        float g = (float)vec3d.x;
        float h = (float)vec3d.y;
        float i = (float)vec3d.z;
        Vec3d vec3d2 = world.getFogColor(tickDelta);
        this.fogRed = (float)vec3d2.x;
        this.fogGreen = (float)vec3d2.y;
        this.fogBlue = (float)vec3d2.z;
        float p;
        if (this.field_1029.options.viewDistance >= 4) {
            double d = -1.0D;
            Vec3d vec3d3 = MathHelper.sin(world.getSkyAngleRadians(tickDelta)) > 0.0F ? new Vec3d(d, 0.0D, 0.0D) : new Vec3d(1.0D, 0.0D, 0.0D);
            p = (float)entity.getRotationVector(tickDelta).dotProduct(vec3d3);
            if (p < 0.0F) {
                p = 0.0F;
            }

            if (p > 0.0F) {
                float[] fs = world.dimension.getBackgroundColor(world.getSkyAngle(tickDelta), tickDelta);
                if (fs != null) {
                    p *= fs[3];
                    this.fogRed = this.fogRed * (1.0F - p) + fs[0] * p;
                    this.fogGreen = this.fogGreen * (1.0F - p) + fs[1] * p;
                    this.fogBlue = this.fogBlue * (1.0F - p) + fs[2] * p;
                }
            }
        }

        this.fogRed += (g - this.fogRed) * f;
        this.fogGreen += (h - this.fogGreen) * f;
        this.fogBlue += (i - this.fogBlue) * f;
        float k = world.getRainGradient(tickDelta);
        float n;
        float o;
        if (k > 0.0F) {
            n = 1.0F - k * 0.5F;
            o = 1.0F - k * 0.4F;
            this.fogRed *= n;
            this.fogGreen *= n;
            this.fogBlue *= o;
        }

        n = world.getThunderGradient(tickDelta);
        if (n > 0.0F) {
            o = 1.0F - n * 0.5F;
            this.fogRed *= o;
            this.fogGreen *= o;
            this.fogBlue *= o;
        }

        Block block = class_321.method_9371(WorldPreview.clientWorld, entity, tickDelta);


        p = this.field_1842 + (this.field_1843 - this.field_1842) * tickDelta;
        this.fogRed *= p;
        this.fogGreen *= p;
        this.fogBlue *= p;
        double e = (entity.prevTickY + (entity.y - entity.prevTickY) * (double)tickDelta) * world.dimension.method_3994();
        if (entity instanceof LivingEntity && ((LivingEntity)entity).hasStatusEffect(StatusEffect.BLINDNESS)) {
            int r = ((LivingEntity)entity).getEffectInstance(StatusEffect.BLINDNESS).getDuration();
            if (r < 20) {
                e *= (double)(1.0F - (float)r / 20.0F);
            } else {
                e = 0.0D;
            }
        }

        if (e < 1.0D) {
            if (e < 0.0D) {
                e = 0.0D;
            }

            e *= e;
            this.fogRed = (float)((double)this.fogRed * e);
            this.fogGreen = (float)((double)this.fogGreen * e);
            this.fogBlue = (float)((double)this.fogBlue * e);
        }

        float t;
        if (this.skyDarkness > 0.0F) {
            t = this.lastSkyDarkness + (this.skyDarkness - this.lastSkyDarkness) * tickDelta;
            this.fogRed = this.fogRed * (1.0F - t) + this.fogRed * 0.7F * t;
            this.fogGreen = this.fogGreen * (1.0F - t) + this.fogGreen * 0.6F * t;
            this.fogBlue = this.fogBlue * (1.0F - t) + this.fogBlue * 0.6F * t;
        }

        float u;


        if (this.field_1029.options.anaglyph3d) {
            t = (this.fogRed * 30.0F + this.fogGreen * 59.0F + this.fogBlue * 11.0F) / 100.0F;
            u = (this.fogRed * 30.0F + this.fogGreen * 70.0F) / 100.0F;
            float x = (this.fogRed * 30.0F + this.fogBlue * 70.0F) / 100.0F;
            this.fogRed = t;
            this.fogGreen = u;
            this.fogBlue = x;
        }

        GlStateManager.clearColor(this.fogRed, this.fogGreen, this.fogBlue, 0.0F);
    }
    private void setupCamera(float tickDelta, int anaglyphFilter) {

        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();
        float f = 0.07F;




        Project.gluPerspective((float) this.getFov(tickDelta, true), (float)this.field_1029.width / (float)this.field_1029.height, 0.05F, this.field_1029.options.viewDistance * 16 * MathHelper.SQUARE_ROOT_OF_TWO);
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();






        this.transformCamera(tickDelta);


    }


    private void transformCamera(float tickDelta) {
        Entity entity = WorldPreview.player;
        float f = entity.getEyeHeight();
        double d = entity.prevX + (entity.x - entity.prevX) * (double)tickDelta;
        double e = entity.prevY + (entity.y - entity.prevY) * (double)tickDelta + (double)f;
        double g = entity.prevZ + (entity.z - entity.prevZ) * (double)tickDelta;
        if (this.field_1029.options.perspective > 0) {
            double h = (double)(4);
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

        if (!this.field_1029.options.field_955) {
            GlStateManager.rotatef(entity.prevPitch + (entity.pitch - entity.prevPitch) * tickDelta, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotatef(entity.prevYaw + (entity.yaw - entity.prevYaw) * tickDelta + 180.0F, 0.0F, 1.0F, 0.0F);
        }

        GlStateManager.translatef(0.0F, -f, 0.0F);


    }    private float fogRed;
    private float fogGreen;
    private float fogBlue;
    private FloatBuffer fogColorBuffer = GlAllocationUtils.allocateFloatBuffer(16);
    private float field_1843 = 0;
    private float field_1842 = 0;
    private float lastSkyDarkness;
    private float skyDarkness;

    private FloatBuffer updateFogColorBuffer(float red, float green, float blue, float alpha) {
        this.fogColorBuffer.clear();
        this.fogColorBuffer.put(red).put(green).put(blue).put(alpha);
        this.fogColorBuffer.flip();
        return this.fogColorBuffer;
    }
    private void renderClouds(WorldRenderer worldRenderer, float tickDelta, int anaglyphFilter) {
        if (this.field_1029.options.getCloudMode() != 0) {
            this.field_1029.profiler.swap("clouds");
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective((float) this.getFov(tickDelta, true), (float)this.field_1029.width / (float)this.field_1029.height, 0.05F, (float)(this.field_1029.options.viewDistance * 16) * 4.0F);
            GlStateManager.matrixMode(5888);
            GlStateManager.pushMatrix();
            this.renderFog(0, tickDelta);
            worldRenderer.method_9910(tickDelta, anaglyphFilter);
            //  GlStateManager.disableFog();
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective((float) this.getFov(tickDelta, true), (float)this.field_1029.width / (float)this.field_1029.height, 0.05F, (float)(this.field_1029.options.viewDistance * 16)* MathHelper.SQUARE_ROOT_OF_TWO);
            GlStateManager.matrixMode(5888);
        }

    }
    private void renderFog(int i, float tickDelta) {



        GL11.glFog(2918, this.updateFogColorBuffer(this.fogRed, this.fogGreen, this.fogBlue, 1.0F));
        GL11.glNormal3f(0.0F, -1.0F, 0.0F);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        float g;

        g = (float)(this.field_1029.options.viewDistance * 16);
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


        GlStateManager.enableColorMaterial();
        GlStateManager.enableFog();
        GlStateManager.colorMaterial(1028, 4608);
    }

    private double getFov( double tickDelta, boolean changingFov) {

        return this.field_1029.options.fov;

    }


    @Inject(method = "<init>",at = @At(value = "TAIL"))
    public void worldpreview_init(MinecraftClient field_1029, CallbackInfo ci){
        WorldPreview.freezePreview=false;
        WorldPreview.calculatedSpawn=false;
        KeyBinding.unpressAll();
    }
private boolean bbb = false;
   /* public void renderWorld(float tickDelta, long limitTime) {
       // this.field_1029.gameRenderer.updateLightmap(tickDelta);



        GlStateManager.enableDepthTest();
        GlStateManager.enableAlphaTest();
        GlStateManager.alphaFunc(516, 0.5F);

            this.renderWorld(2, tickDelta, limitTime);


    }

    private void renderWorld(int anaglyphFilter, float tickDelta, long limitTime) {

        WorldRenderer worldRenderer = WorldPreview.worldRenderer;
        GlStateManager.enableCull();
        this.minecraft.getProfiler().swap("camera");
        this.applyCameraTransformations(tickDelta);
        Camera camera = WorldPreview.camera;
        camera.update(WorldPreview.world, WorldPreview.player, this.minecraft.options.perspective > 0, this.minecraft.options.perspective == 2, tickDelta);
        Frustum frustum = GlMatrixFrustum.get();
        worldRenderer.method_21595(camera);
        this.minecraft.getProfiler().swap("clear");
        GlStateManager.viewport(0, 0, this.minecraft.window.getFramebufferWidth(), this.minecraft.window.getFramebufferHeight());
        this.backgroundRenderer.renderBackground(camera, tickDelta);
        GlStateManager.clear(16640, MinecraftClient.IS_SYSTEM_MAC);
        this.minecraft.getProfiler().swap("culling");
        VisibleRegion visibleRegion = new FrustumWithOrigin(frustum);
        double d = camera.getPos().x;
        double e = camera.getPos().y;
        double f = camera.getPos().z;
        visibleRegion.setOrigin(d, e, f);
        if (this.minecraft.options.viewDistance >= 4) {
            this.backgroundRenderer.applyFog(camera, -1);
            this.minecraft.getProfiler().swap("sky");
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            GlStateManager.multMatrix(Matrix4f.method_4929(this.getFov(camera, tickDelta, true), (float)this.minecraft.window.getFramebufferWidth() / (float)this.minecraft.window.getFramebufferHeight(), 0.05F, this.minecraft.options.viewDistance * 16 * 2.0F));
            GlStateManager.matrixMode(5888);
            worldRenderer.renderSky(tickDelta);
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            GlStateManager.multMatrix(Matrix4f.method_4929(this.getFov(camera, tickDelta, true), (float)this.minecraft.window.getFramebufferWidth() / (float)this.minecraft.window.getFramebufferHeight(), 0.05F, this.minecraft.options.viewDistance * 16 * MathHelper.SQUARE_ROOT_OF_TWO));
            GlStateManager.matrixMode(5888);
        }

        this.backgroundRenderer.applyFog(camera, 0);
        GlStateManager.shadeModel(7425);
        if (camera.getPos().y < 128.0D) {
            this.renderAboveClouds(camera, worldRenderer, tickDelta, d, e, f);
        }

        this.minecraft.getProfiler().swap("prepareterrain");
        this.backgroundRenderer.applyFog(camera, 0);
        this.minecraft.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
        DiffuseLighting.disable();
        this.minecraft.getProfiler().swap("terrain_setup");
        WorldPreview.clientWord.getChunkManager().getLightingProvider().doLightUpdates(2147483647, true, true);
        worldRenderer.setUpTerrain(camera, visibleRegion, this.field_4021++, false);
        this.minecraft.getProfiler().swap("updatechunks");
        worldRenderer.updateChunks(endTime);
        this.minecraft.getProfiler().swap("terrain");
        GlStateManager.matrixMode(5888);
        GlStateManager.pushMatrix();
        GlStateManager.disableAlphaTest();
        worldRenderer.renderLayer(RenderLayer.SOLID, camera);
        GlStateManager.enableAlphaTest();
        worldRenderer.renderLayer(RenderLayer.CUTOUT_MIPPED, camera);
        this.minecraft.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).pushFilter(false, false);
        worldRenderer.renderLayer(RenderLayer.CUTOUT, camera);
        this.minecraft.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).popFilter();
        GlStateManager.shadeModel(7424);
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();


        this.minecraft.getProfiler().swap("translucent");
        worldRenderer.renderLayer(RenderLayer.TRANSLUCENT, camera);
        GlStateManager.shadeModel(7424);
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.disableFog();
        if (camera.getPos().y >= 128.0D) {
            this.minecraft.getProfiler().swap("aboveClouds");
            this.renderAboveClouds(camera, worldRenderer, tickDelta, d, e, f);
        }

    }*/






}
