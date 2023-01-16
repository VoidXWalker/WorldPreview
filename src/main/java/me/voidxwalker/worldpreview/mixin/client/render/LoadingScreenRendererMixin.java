package me.voidxwalker.worldpreview.mixin.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import me.voidxwalker.worldpreview.ChunkSetter;
import me.voidxwalker.worldpreview.WorldPreview;

import me.voidxwalker.worldpreview.mixin.access.MinecraftClientMixin;
import me.voidxwalker.worldpreview.mixin.access.WorldRendererMixin;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.render.*;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.client.util.Window;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
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
import java.util.ArrayList;

@Mixin(LoadingScreenRenderer.class)
public abstract class LoadingScreenRendererMixin {
    @Shadow private MinecraftClient client;
    
    private long lastRenderTime;

    @Shadow private String field_1028;

    @Shadow private Window window;
    @Shadow private String title;
    private int frameCount = 0;

    private long nanoTime = 0;

    private ButtonWidget resetButton;
    private ArrayList<ButtonWidget> buttons = new ArrayList<ButtonWidget>();

    /**
     * @author Pixfumy
     * @reason This method is absolutely unrecognizable with all the changes. Any other mod targeting this method should know about this.
     */
    @Overwrite
    public void setProgressPercentage(int percentage) {
        if(WorldPreview.worldRenderer==null){
            WorldPreview.worldRenderer=new WorldRenderer(MinecraftClient.getInstance());
            ((ChunkSetter)WorldPreview.worldRenderer).setPreviewRenderer();
        }
        long l = MinecraftClient.getTime();
        Window window = this.window;
        int width = window.getWidth();
        int height = window.getHeight();
        int mouseX = Mouse.getX() * width / this.client.width;
        int mouseY = height - Mouse.getY() * height / this.client.height - 1;
        if (!WorldPreview.inPreview) {
            WorldPreview.inPreview = true;
            WorldPreview.init();
            buttons.add(this.resetButton = new ButtonWidget(1, width / 2 - 100, height / 4 + 120 - 16, I18n.translate("menu.returnToMenu")));
            buttons.add(new ButtonWidget(4, width / 2 - 100, height / 4 + 24 - 16, I18n.translate("menu.returnToGame")));
            buttons.add(new ButtonWidget(0, width / 2 - 100, height / 4 + 96 - 16, 98, 20, I18n.translate("menu.options")));
            buttons.add(new ButtonWidget(7, width / 2 + 2, height / 4 + 96 - 16, 98, 20, I18n.translate("menu.shareToLan")));
            buttons.add(new ButtonWidget(5, width / 2 - 100, height / 4 + 48 - 16, 98, 20, I18n.translate("gui.achievements")));
            buttons.add(new ButtonWidget(6, width / 2 + 2, height / 4 + 48 - 16, 98, 20, I18n.translate("gui.stats")));
        }
        if (((WorldRendererMixin) WorldPreview.worldRenderer).getWorld() != null) {
            int buttonHeight = 20;
            boolean resetHovered = mouseX >= this.resetButton.x && mouseY >= this.resetButton.y && mouseX < this.resetButton.x + this.resetButton.getWidth() && mouseY < this.resetButton.y + buttonHeight;
            if (resetHovered && Mouse.isButtonDown(0)) {
                WorldPreview.kill = 1;
                WorldPreview.inPreview = false;
                return;
            }
        }
        if (WorldPreview.world != null && WorldPreview.clientWorld != null && WorldPreview.player != null && WorldPreview.inPreview && WorldPreview.loadedSpawn) {
            // render the world
            if (((WorldRendererMixin) WorldPreview.worldRenderer).getWorld() == null) {
                WorldPreview.worldRenderer.setWorld(WorldPreview.clientWorld);
                frameCount = 0;
                WorldPreview.canFreeze = true;
            }
            if (((WorldRendererMixin) WorldPreview.worldRenderer).getWorld() != null) {
                this.field_1842 = this.field_1843;
                float h = WorldPreview.world.getBrightness(new BlockPos(WorldPreview.player));
                float x = (float) this.client.options.viewDistance / 32.0F;
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
                int p = this.client.options.maxFramerate;
                int q = Math.min(10, p);
                q = Math.max(q, 60);
                long r = System.nanoTime() - nanoTime;
                long s = Math.max((long) (1000000000 / q / 4) - r, 0L);
                if (l - this.lastRenderTime >= 1000L / 40) { // TODO: change to LOADING_SCREEN_FPS if mods decide it should be configurable
                    this.renderWorld(((MinecraftClientMixin) this.client).getTicker().tickDelta, System.nanoTime() + s);
                    GlStateManager.matrixMode(5889);
                    GlStateManager.loadIdentity();
                    GlStateManager.ortho(0.0D, window.getScaledWidth(), window.getScaledHeight(), 0.0D, 100.0D, 300.0D);
                    GlStateManager.matrixMode(5888);
                    GlStateManager.loadIdentity();
                    GlStateManager.translatef(0.0F, 0.0F, -200.0F);
                    this.renderGreyedBackground(width, height);
                    this.renderCenteredString(this.client.textRenderer, I18n.translate("menu.game"), width / 2, 40, 16777215);
                    this.renderMenuButtons(width, height, mouseX, mouseY);
                }
            }
        } else { // usual loading screen
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            GlStateManager.ortho(0.0D, window.getScaledWidth(), window.getScaledHeight(), 0.0D, 100.0D, 300.0D);
            GlStateManager.matrixMode(5888);
            GlStateManager.loadIdentity();
            GlStateManager.translatef(0.0F, 0.0F, -200.0F);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.getBuffer();
            this.client.getTextureManager().bindTexture(DrawableHelper.OPTIONS_BACKGROUND_TEXTURE);
            float f = 32.0F;
            bufferBuilder.begin(7, VertexFormats.POSITION_TEXTURE_COLOR);
            bufferBuilder.vertex(0.0D, (double) height, 0.0D).texture(0.0D, (double) ((float) height / f)).color(64, 64, 64, 255).next();
            bufferBuilder.vertex((double) width, (double) height, 0.0D).texture((double) ((float) width / f), (double) ((float) height / f)).color(64, 64, 64, 255).next();
            bufferBuilder.vertex((double) width, 0.0D, 0.0D).texture((double) ((float) width / f), 0.0D).color(64, 64, 64, 255).next();
            bufferBuilder.vertex(0.0D, 0.0D, 0.0D).texture(0.0D, 0.0D).color(64, 64, 64, 255).next();
            tessellator.draw();
            if (percentage >= 0) {
                int m = 100;
                int n = 2;
                int o = width / 2 - m / 2;
                int p = width / 2 + 16;
                GlStateManager.disableTexture();
                bufferBuilder.begin(7, VertexFormats.POSITION_COLOR);
                bufferBuilder.vertex((double) o, (double) p, 0.0D).color(128, 128, 128, 255).next();
                bufferBuilder.vertex((double) o, (double) (p + n), 0.0D).color(128, 128, 128, 255).next();
                bufferBuilder.vertex((double) (o + m), (double) (p + n), 0.0D).color(128, 128, 128, 255).next();
                bufferBuilder.vertex((double) (o + m), (double) p, 0.0D).color(128, 128, 128, 255).next();
                bufferBuilder.vertex((double) o, (double) p, 0.0D).color(128, 255, 128, 255).next();
                bufferBuilder.vertex((double) o, (double) (p + n), 0.0D).color(128, 255, 128, 255).next();
                bufferBuilder.vertex((double) (o + percentage), (double) (p + n), 0.0D).color(128, 255, 128, 255).next();
                bufferBuilder.vertex((double) (o + percentage), (double) p, 0.0D).color(128, 255, 128, 255).next();
                tessellator.draw();
                GlStateManager.enableTexture();
            }
        }
        if (l - this.lastRenderTime >= 1000L / 40) {
            this.lastRenderTime = l;
            GlStateManager.enableBlend();
            GlStateManager.blendFuncSeparate(770, 771, 1, 0);
            this.client.textRenderer.drawWithShadow(this.title, (float) ((width - this.client.textRenderer.getStringWidth(this.title)) / 2), (float) (height / 2 - 4 - 16), 16777215);
            this.client.textRenderer.drawWithShadow(this.field_1028, (float) ((width - this.client.textRenderer.getStringWidth(this.field_1028)) / 2), (float) (height / 2 - 4 + 8), 16777215);
            this.client.updateDisplay();
        }
        this.nanoTime = System.nanoTime();
        Display.processMessages();
        try {
            Thread.yield();
        } catch (Exception var15) {
        }
    }

    public void renderWorld(float tickDelta, long endTime) {
        GlStateManager.enableDepthTest();
        GlStateManager.enableAlphaTest();
        GlStateManager.alphaFunc(516, 0.5F);
        this.renderCenter(tickDelta, endTime);
    }

    private void renderCenter(float tickDelta, long endTime) {
        WorldRenderer worldRenderer =  WorldPreview.worldRenderer;
        ( (ChunkSetter)worldRenderer).setPreviewRenderer();
        GlStateManager.enableCull();

        this.client.profiler.swap("clear");
        GlStateManager.viewPort(0, 0, this.client.width, this.client.height);
        updateFog(tickDelta);
        GlStateManager.clear(16640);
        this.client.profiler.swap("camera");
        setupCamera(tickDelta, 2);
        Camera.update(WorldPreview.player, this.client.options.perspective == 2);
        this.client.profiler.swap("frustum");
        Frustum.getInstance();
        this.client.profiler.swap("culling");
        CullingCameraView cameraView = new CullingCameraView();
        Entity entity = WorldPreview.player;
        double d = entity.prevTickX + (entity.x - entity.prevTickX) * (double)tickDelta;
        double e = entity.prevTickY + (entity.y - entity.prevTickY) * (double)tickDelta;
        double f = entity.prevTickZ + (entity.z - entity.prevTickZ) * (double)tickDelta;
        cameraView.setPos(d, e, f);
        if (this.client.options.viewDistance >= 4) {
            renderFog(-1, tickDelta);
            this.client.profiler.swap("sky");
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective((float)  90.0F, (float)this.client.width / (float)this.client.height, 0.05F, (float)(this.client.options.viewDistance * 16) * 2.0F);
            GlStateManager.matrixMode(5888);
            worldRenderer.renderSky(tickDelta, 2);
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective((float)  90.0F, (float)this.client.width / (float)this.client.height, 0.05F, (float)(this.client.options.viewDistance * 16)* MathHelper.SQUARE_ROOT_OF_TWO);
            GlStateManager.matrixMode(5888);
        }

        renderFog(0, tickDelta);
        GlStateManager.shadeModel(7425);
        if (entity.y + (double)entity.getEyeHeight() < 128.0D) {
            renderClouds(worldRenderer, tickDelta, 2);
        }

        this.client.profiler.swap("prepareterrain");
        renderFog(-1, tickDelta);
        this.client.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
        DiffuseLighting.disable();
        this.client.profiler.swap("terrain_setup");
        if (WorldPreview.loadedSpawn) {
            if (WorldPreview.canReload && !WorldPreview.freezePreview) {
                worldRenderer.reload();
                WorldPreview.canReload = false;
            }
        }
        worldRenderer.setupTerrain(entity, (double)tickDelta, cameraView, frameCount++, true);
        this.client.profiler.swap("updatechunks");
        worldRenderer.updateChunks(endTime);
        this.client.profiler.swap("terrain");
        GlStateManager.matrixMode(5888);
        GlStateManager.pushMatrix();
        GlStateManager.disableAlphaTest();
        worldRenderer.renderLayer(RenderLayer.SOLID, (double)tickDelta, 2, entity);
        GlStateManager.enableAlphaTest();
        worldRenderer.renderLayer(RenderLayer.CUTOUT_MIPPED, (double)tickDelta, 2, entity);
        this.client.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).pushFilter(false, false);
        worldRenderer.renderLayer(RenderLayer.CUTOUT, (double)tickDelta, 2, entity);
        this.client.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).pop();
        GlStateManager.shadeModel(7424);
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();


        this.client.profiler.swap("translucent");
        worldRenderer.renderLayer(RenderLayer.TRANSLUCENT, tickDelta, 2, entity);
        GlStateManager.shadeModel(7424);
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.disableFog();
    }

    private void updateFog(float tickDelta) {
        World world =WorldPreview.clientWorld;
        Entity entity = WorldPreview.player;
        float f = 0.25F + 0.75F * (float)this.client.options.viewDistance / 32.0F;
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
        if (this.client.options.viewDistance >= 4) {
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

        Block block = Camera.getSubmergedBlock(WorldPreview.clientWorld, entity, tickDelta);
        if (block.getMaterial() == Material.WATER) {
            this.fogRed = 0.02F;
            this.fogGreen = 0.02F;
            this.fogBlue = 0.2F;
        } else if (block.getMaterial() == Material.LAVA) {
            this.fogRed = 0.6F;
            this.fogGreen = 0.1F;
            this.fogBlue = 0.0F;
        }


        p = this.field_1842 + (this.field_1843 - this.field_1842) * tickDelta;
        this.fogRed *= p;
        this.fogGreen *= p;
        this.fogBlue *= p;
        double e = (entity.prevTickY + (entity.y - entity.prevTickY) * (double)tickDelta) * world.dimension.method_3994();

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

        GlStateManager.clearColor(this.fogRed, this.fogGreen, this.fogBlue, 0.0F);
    }
    private void setupCamera(float tickDelta, int anaglyphFilter) {

        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();
        float f = 0.07F;




        Project.gluPerspective((float) this.getFov(tickDelta, true), (float)this.client.width / (float)this.client.height, 0.05F, this.client.options.viewDistance * 16 * MathHelper.SQUARE_ROOT_OF_TWO);
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
        if (this.client.options.perspective > 0) {
            double h = (double)(4);
            if (this.client.options.field_955) {
                GlStateManager.translatef(0.0F, 0.0F, (float)(-h));
            } else {
                float j = entity.yaw;
                float k = entity.pitch;
                if (this.client.options.perspective == 2) {
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
                    BlockHitResult hitResult = WorldPreview.clientWorld.rayTrace(new Vec3d(d + (double)p, e + (double)q, g + (double)r), new Vec3d(d - l + (double)p + (double)r, e - n + (double)q, g - m + (double)r));
                    if (hitResult != null) {
                        double s = hitResult.pos.distanceTo(new Vec3d(d, e, g));
                        if (s < h) {
                            h = s;
                        }
                    }
                }

                if (this.client.options.perspective == 2) {
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

        if (!this.client.options.field_955) {
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
        if (this.client.options.getCloudMode() != 0) {
            this.client.profiler.swap("clouds");
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective((float) this.getFov(tickDelta, true), (float)this.client.width / (float)this.client.height, 0.05F, (float)(this.client.options.viewDistance * 16) * 4.0F);
            GlStateManager.matrixMode(5888);
            GlStateManager.pushMatrix();
            this.renderFog(0, tickDelta);
            worldRenderer.renderClouds(tickDelta, anaglyphFilter);
            GlStateManager.disableFog();
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective((float) this.getFov(tickDelta, true), (float)this.client.width / (float)this.client.height, 0.05F, (float)(this.client.options.viewDistance * 16)* MathHelper.SQUARE_ROOT_OF_TWO);
            GlStateManager.matrixMode(5888);
        }

    }

    private void renderFog(int i, float tickDelta) {
        Entity entity = this.client.getCameraEntity();
        GL11.glFog(2918, (FloatBuffer)this.updateFogColorBuffer(this.fogRed, this.fogGreen, this.fogBlue, 1.0F));
        GL11.glNormal3f(0.0F, -1.0F, 0.0F);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        Block block = Camera.getSubmergedBlock(WorldPreview.clientWorld, entity, tickDelta);
        float g;
        if (block.getMaterial() == Material.WATER) {
            GlStateManager.fogMode(2048);
            GlStateManager.fogDensity(0.1F);
        } else if (block.getMaterial() == Material.LAVA) {
            GlStateManager.fogMode(2048);
            GlStateManager.fogDensity(2.0F);
        } else {
            g = this.client.options.viewDistance * 16;
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
        }

        GlStateManager.enableColorMaterial();
        GlStateManager.enableFog();
        GlStateManager.colorMaterial(1028, 4608);
    }

    private double getFov( double tickDelta, boolean changingFov) {
        return this.client.options.fov;
    }

    private void renderGreyedBackground(int width, int height) {
        float f = (float) (-1072689136 >> 24 & 255) / 255.0F;
        float g = (float) (-1072689136 >> 16 & 255) / 255.0F;
        float h = (float) (-1072689136 >> 8 & 255) / 255.0F;
        float i = (float) (-1072689136 & 255) / 255.0F;
        float j = (float) (-804253680 >> 24 & 255) / 255.0F;
        float k = (float) (-804253680 >> 16 & 255) / 255.0F;
        float l = (float) (-804253680 >> 8 & 255) / 255.0F;
        float m = (float) (-804253680 & 255) / 255.0F;
        GlStateManager.disableTexture();
        GlStateManager.enableBlend();
        GlStateManager.disableAlphaTest();
        GlStateManager.blendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(7, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex((double) width, (double) 0.0, (double) 0).color(g, h, i, f).next();
        bufferBuilder.vertex((double) 0, (double) 0.0, (double) 0).color(g, h, i, f).next();
        bufferBuilder.vertex((double) 0, (double) height, (double) 0).color(k, l, m, j).next();
        bufferBuilder.vertex((double) width, (double) height, (double) 0).color(k, l, m, j).next();
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlphaTest();
        GlStateManager.enableTexture();
    }

    private void renderMenuButtons(int width, int height, int mouseX, int mouseY) {
        TextRenderer textRenderer = this.client.textRenderer;
        for (ButtonWidget button: this.buttons) {
            int buttonWidth = button.getWidth();
            int buttonHeight = 20;
            this.client.getTextureManager().bindTexture(new Identifier("textures/gui/widgets.png"));
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            boolean hovered = mouseX >= button.x && mouseY >= button.y && mouseX < button.x + buttonWidth && mouseY < button.y + buttonHeight;
            int i = hovered ? 2 : 1;
            GlStateManager.enableBlend();
            GlStateManager.blendFuncSeparate(770, 771, 1, 0);
            GlStateManager.blendFunc(770, 771);
            this.drawTexture(button.x, button.y, 0, 46 + i * 20, buttonWidth / 2, buttonHeight);
            this.drawTexture(button.x + buttonWidth / 2, button.y, 200 - buttonWidth / 2, 46 + i * 20, buttonWidth / 2, buttonHeight);
            int j = hovered ? 16777120 : 14737632;
            this.renderCenteredString(textRenderer, button.message, button.x + buttonWidth / 2, button.y + (buttonHeight - 8) / 2, j);
        }
    }

    private void renderCenteredString(TextRenderer textRenderer, String text, int centerX, int y, int color) {
        textRenderer.drawWithShadow(text, (float)(centerX - textRenderer.getStringWidth(text) / 2), (float)y, color);
    }

    private void drawTexture(int x, int y, int u, int v, int width, int height) {
        float f = 0.00390625F;
        float g = 0.00390625F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(7, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex((double)(x + 0), (double)(y + height), (double)0).texture((double)((float)(u + 0) * f), (double)((float)(v + height) * g)).next();
        bufferBuilder.vertex((double)(x + width), (double)(y + height), (double)0).texture((double)((float)(u + width) * f), (double)((float)(v + height) * g)).next();
        bufferBuilder.vertex((double)(x + width), (double)(y + 0), (double)0).texture((double)((float)(u + width) * f), (double)((float)(v + 0) * g)).next();
        bufferBuilder.vertex((double)(x + 0), (double)(y + 0), (double)0).texture((double)((float)(u + 0) * f), (double)((float)(v + 0) * g)).next();
        tessellator.draw();
    }
}
