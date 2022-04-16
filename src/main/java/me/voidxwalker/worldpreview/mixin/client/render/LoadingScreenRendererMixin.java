package me.voidxwalker.worldpreview.mixin.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.mixin.access.ScreenMixin;
import me.voidxwalker.worldpreview.mixin.access.WorldRendererMixin;
import net.minecraft.block.Material;
import net.minecraft.client.MinecraftClient;
//import net.minecraft.client.gui.WorldGenerationProgressTracker;
//import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.class_321;
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
import net.minecraft.client.util.Clipper;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
//import net.minecraft.fluid.FluidState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.Level;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.glu.Project;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingScreenRenderer.class)
public abstract class LoadingScreenRendererMixin {
    @Shadow private MinecraftClient field_1029;

    @Shadow private long field_1031;

    @Redirect(method = "progressStagePercentage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/TextureManager;bindTexture(Lnet/minecraft/util/Identifier;)V"))
    private void removeBackgroundTexture(TextureManager instance, Identifier id) {

    }

    @Inject(method = "progressStagePercentage", at = @At("HEAD"), cancellable = true)
    private void render(int percentage, CallbackInfo ci) {
        if(WorldPreview.worldRenderer==null){
            WorldPreview.worldRenderer=new WorldRenderer(MinecraftClient.getInstance());
        }
        if(WorldPreview.world != null && WorldPreview.clientWorld != null && WorldPreview.player != null && !WorldPreview.freezePreview) {
            if(((me.voidxwalker.worldpreview.mixin.access.WorldRendererMixin)WorldPreview.worldRenderer).getWorld() == null && WorldPreview.calculatedSpawn){
                WorldPreview.worldRenderer.method_1371(WorldPreview.clientWorld);
            }
            if (((WorldRendererMixin)WorldPreview.worldRenderer).getWorld()!=null) {
                KeyBinding.unpressAll();
                WorldPreview.kill=0;
//                if (WorldPreview.camera == null) {
//                    WorldPreview.player.refreshPositionAndAngles(WorldPreview.player.x, WorldPreview.player.y+ 1+(WorldPreview.player.getBoundingBox().y2-WorldPreview.player.getBoundingBox().y1), WorldPreview.player.z, 0.0F, 0.0F);
//                    WorldPreview.camera = new Camera();
//                    WorldPreview.camera.update(WorldPreview.world, WorldPreview.player, this.minecraft.options.perspective > 0, this.minecraft.options.perspective == 2, 0.2F);
//                    WorldPreview.player.refreshPositionAndAngles(WorldPreview.player.x, WorldPreview.player.y - 1.5, WorldPreview.player.z, 0.0F, 0.0F);
//                    WorldPreview.inPreview=true;
//                    WorldPreview.log(Level.INFO,"Starting Preview at ("+ WorldPreview.player.x + ", "+(double)Math.floor(WorldPreview.player.y)+ ", "+ WorldPreview.player.z+")");
//                }
                renderWorld(0, (long)(1000000000 / 60 / 4) );
                Window window = new Window(MinecraftClient.getInstance());
                GlStateManager.clear(256);
                GlStateManager.matrixMode(5889);
                GlStateManager.loadIdentity();
                GlStateManager.ortho(0.0D, (double) window.getScaledWidth() / window.getScaleFactor(), (double) window.getScaledWidth() / window.getScaleFactor(), 0.0D, 1000.0D, 3000.0D);
                GlStateManager.matrixMode(5888);
                GlStateManager.loadIdentity();
                GlStateManager.translatef(0.0F, 0.0F, -2000.0F);
                //this.renderPauseMenu(mouseX,mouseY,delta);
                this.field_1029.textRenderer.drawWithShadow(this.field_1030, (float)((j - this.field_1029.textRenderer.getStringWidth(this.field_1030)) / 2), (float)(k / 2 - 4 - 16), 16777215);
                this.field_1029.textRenderer.drawWithShadow(this.field_1028, (float)((j - this.field_1029.textRenderer.getStringWidth(this.field_1028)) / 2), (float)(k / 2 - 4 + 8), 16777215);
                ci.cancel();
            }
        }
    }

    @Inject(method = "<init>",at = @At(value = "TAIL"))
    public void worldpreview_init(MinecraftClient client, CallbackInfo ci){
        WorldPreview.freezePreview=false;
        WorldPreview.calculatedSpawn=false;
        KeyBinding.unpressAll();
    }

//    @Inject(method = "render",at=@At("HEAD"),cancellable = true)
//    public void render(int mouseX, int mouseY, float delta, CallbackInfo ci) {
//        if(WorldPreview.worldRenderer==null){
//            WorldPreview.worldRenderer=new WorldRenderer(MinecraftClient.getInstance());
//        }
//        if(this.backgroundRenderer==null){
//            backgroundRenderer= new BackgroundRenderer(MinecraftClient.getInstance().gameRenderer);
//        }
//        if(WorldPreview.world!=null&& WorldPreview.clientWord!=null&&WorldPreview.player!=null&&!WorldPreview.freezePreview) {
//            if(((me.voidxwalker.worldpreview.mixin.access.WorldRendererMixin)WorldPreview.worldRenderer).getWorld()==null&& WorldPreview.calculatedSpawn){
//                WorldPreview.worldRenderer.setWorld(WorldPreview.clientWord);
//                WorldPreview.showMenu=true;
//                this.worldpreview_showMenu=true;
//                this.worldpreview_initWidgets();
//            }
//            if (((WorldRendererMixin)WorldPreview.worldRenderer).getWorld()!=null) {
//                KeyBinding.unpressAll();
//                WorldPreview.kill=0;
//                if(this.worldpreview_showMenu!= WorldPreview.showMenu){
//                    if(!WorldPreview.showMenu){
//                        this.children.clear();
//                    }
//                    else {
//                        this.worldpreview_initWidgets();
//                    }
//                    this.worldpreview_showMenu= WorldPreview.showMenu;
//                }
//                if (WorldPreview.camera == null) {
//                    WorldPreview.player.refreshPositionAndAngles(WorldPreview.player.x, WorldPreview.player.y+ 1+(WorldPreview.player.getBoundingBox().y2-WorldPreview.player.getBoundingBox().y1), WorldPreview.player.z, 0.0F, 0.0F);
//                    WorldPreview.camera = new Camera();
//                    WorldPreview.camera.update(WorldPreview.world, WorldPreview.player, this.minecraft.options.perspective > 0, this.minecraft.options.perspective == 2, 0.2F);
//                    WorldPreview.player.refreshPositionAndAngles(WorldPreview.player.x, WorldPreview.player.y - 1.5, WorldPreview.player.z, 0.0F, 0.0F);
//                    WorldPreview.inPreview=true;
//                    WorldPreview.log(Level.INFO,"Starting Preview at ("+ WorldPreview.player.x + ", "+(double)Math.floor(WorldPreview.player.y)+ ", "+ WorldPreview.player.z+")");
//                }
//
//                renderWorld(delta,(long)(1000000000 / 60 / 4) );
//                Window window = this.minecraft.window;
//                GlStateManager.clear(256, MinecraftClient.IS_SYSTEM_MAC);
//                GlStateManager.matrixMode(5889);
//                GlStateManager.loadIdentity();
//                GlStateManager.ortho(0.0D, (double) window.getFramebufferWidth() / window.getScaleFactor(), (double) window.getFramebufferHeight() / window.getScaleFactor(), 0.0D, 1000.0D, 3000.0D);
//                GlStateManager.matrixMode(5888);
//                GlStateManager.loadIdentity();
//                GlStateManager.translatef(0.0F, 0.0F, -2000.0F);
//                this.renderPauseMenu(mouseX,mouseY,delta);
//
//            }
//        }
//    }

    public void renderWorld(float tickDelta, long endTime) {
        GlStateManager.enableDepthTest();
        GlStateManager.enableAlphaTest();
        GlStateManager.alphaFunc(516, 0.5F);
        this.renderWorld(tickDelta, endTime);
    }

    private void renderWorld(int anaglyphFilter, float tickDelta, long limitTime) {
        WorldRenderer worldRenderer = this.field_1029.worldRenderer;
        ParticleManager particleManager = this.field_1029.particleManager;
        boolean bl = this.shouldRenderBlockOutline();
        GlStateManager.enableCull();
        this.field_1029.profiler.swap("clear");
        GlStateManager.viewPort(0, 0, this.field_1029.width, this.field_1029.height);
        this.updateFog(tickDelta);
        GlStateManager.clear(16640);
        this.field_1029.profiler.swap("camera");
        this.setupCamera(tickDelta, anaglyphFilter);
        class_321.method_804(this.field_1029.player, this.field_1029.options.perspective == 2);
        this.field_1029.profiler.swap("frustum");
        Clipper.getInstance();
        this.field_1029.profiler.swap("culling");
        CameraView cameraView = new StructureDebugRenderer();
        Entity entity = this.field_1029.getCameraEntity();
        double d = entity.prevTickX + (entity.x - entity.prevTickX) * (double)tickDelta;
        double e = entity.prevTickY + (entity.y - entity.prevTickY) * (double)tickDelta;
        double f = entity.prevTickZ + (entity.z - entity.prevTickZ) * (double)tickDelta;
        cameraView.setPos(d, e, f);
        if (this.field_1029.options.viewDistance >= 4) {
            this.renderFog(-1, tickDelta);
            this.field_1029.profiler.swap("sky");
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective(this.getFov(tickDelta, true), (float)this.field_1029.width / (float)this.field_1029.height, 0.05F, this.viewDistance * 2.0F);
            GlStateManager.matrixMode(5888);
            worldRenderer.method_9891(tickDelta, anaglyphFilter);
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            Project.gluPerspective(this.getFov(tickDelta, true), (float)this.field_1029.width / (float)this.field_1029.height, 0.05F, this.viewDistance * MathHelper.SQUARE_ROOT_OF_TWO);
            GlStateManager.matrixMode(5888);
        }

        this.renderFog(0, tickDelta);
        GlStateManager.shadeModel(7425);
        if (entity.y + (double)entity.getEyeHeight() < 128.0D) {
            this.renderClouds(worldRenderer, tickDelta, anaglyphFilter);
        }

        this.field_1029.profiler.swap("prepareterrain");
        this.renderFog(0, tickDelta);
        this.field_1029.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
        GuiLighting.disable();
        this.field_1029.profiler.swap("terrain_setup");
        worldRenderer.method_9906(entity, (double)tickDelta, cameraView, this.frameCount++, this.field_1029.player.isSpectator());
        if (anaglyphFilter == 0 || anaglyphFilter == 2) {
            this.field_1029.profiler.swap("updatechunks");
            this.field_1029.worldRenderer.method_9892(limitTime);
        }

        this.field_1029.profiler.swap("terrain");
        GlStateManager.matrixMode(5888);
        GlStateManager.pushMatrix();
        GlStateManager.disableAlphaTest();
        worldRenderer.method_9894(RenderLayer.SOLID, (double)tickDelta, anaglyphFilter, entity);
        GlStateManager.enableAlphaTest();
        worldRenderer.method_9894(RenderLayer.CUTOUT_MIPPED, (double)tickDelta, anaglyphFilter, entity);
        this.field_1029.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).pushFilter(false, false);
        worldRenderer.method_9894(RenderLayer.CUTOUT, (double)tickDelta, anaglyphFilter, entity);
        this.field_1029.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).pop();
        GlStateManager.shadeModel(7424);
        GlStateManager.alphaFunc(516, 0.1F);
        PlayerEntity playerEntity2;
        if (!this.renderingPanorama) {
            GlStateManager.matrixMode(5888);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GuiLighting.enableNormally();
            this.field_1029.profiler.swap("entities");
            worldRenderer.method_9908(entity, cameraView, tickDelta);
            GuiLighting.disable();
            this.disableLightmap();
            GlStateManager.matrixMode(5888);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            if (this.field_1029.result != null && entity.isSubmergedIn(Material.WATER) && bl) {
                playerEntity2 = (PlayerEntity)entity;
                GlStateManager.disableAlphaTest();
                this.field_1029.profiler.swap("outline");
                worldRenderer.method_1380(playerEntity2, this.field_1029.result, 0, tickDelta);
                GlStateManager.enableAlphaTest();
            }
        }

        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();
        if (bl && this.field_1029.result != null && !entity.isSubmergedIn(Material.WATER)) {
            playerEntity2 = (PlayerEntity)entity;
            GlStateManager.disableAlphaTest();
            this.field_1029.profiler.swap("outline");
            worldRenderer.method_1380(playerEntity2, this.field_1029.result, 0, tickDelta);
            GlStateManager.enableAlphaTest();
        }

        this.field_1029.profiler.swap("destroyProgress");
        GlStateManager.enableBlend();
        GlStateManager.blendFuncSeparate(770, 1, 1, 0);
        this.field_1029.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).pushFilter(false, false);
        worldRenderer.method_9899(Tessellator.getInstance(), Tessellator.getInstance().getBuffer(), entity, tickDelta);
        this.field_1029.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).pop();
        GlStateManager.disableBlend();
        if (!this.renderingPanorama) {
            this.enableLightmap();
            this.field_1029.profiler.swap("litParticles");
            particleManager.method_1299(entity, tickDelta);
            GuiLighting.disable();
            this.renderFog(0, tickDelta);
            this.field_1029.profiler.swap("particles");
            particleManager.method_1296(entity, tickDelta);
            this.disableLightmap();
        }

        GlStateManager.depthMask(false);
        GlStateManager.enableCull();
        this.field_1029.profiler.swap("weather");
        this.renderWeather(tickDelta);
        GlStateManager.depthMask(true);
        worldRenderer.method_9907(entity, tickDelta);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.blendFuncSeparate(770, 771, 1, 0);
        GlStateManager.alphaFunc(516, 0.1F);
        this.renderFog(0, tickDelta);
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        this.field_1029.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
        GlStateManager.shadeModel(7425);
        this.field_1029.profiler.swap("translucent");
        worldRenderer.method_9894(RenderLayer.TRANSLUCENT, (double)tickDelta, anaglyphFilter, entity);
        GlStateManager.shadeModel(7424);
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.disableFog();
        if (entity.y + (double)entity.getEyeHeight() >= 128.0D) {
            this.field_1029.profiler.swap("aboveClouds");
            this.renderClouds(worldRenderer, tickDelta, anaglyphFilter);
        }

        this.field_1029.profiler.swap("hand");
        if (this.renderHand) {
            GlStateManager.clear(256);
            this.renderHand(tickDelta, anaglyphFilter);
            this.renderDebugCrosshair(tickDelta);
        }
    }
//    private void renderCenter(float tickDelta, long endTime) {
//        WorldRenderer worldRenderer = WorldPreview.worldRenderer;
//        GlStateManager.enableCull();
//        this.minecraft.getProfiler().swap("camera");
//        this.applyCameraTransformations(tickDelta);
//        Camera camera = WorldPreview.camera;
//        camera.update(WorldPreview.world, WorldPreview.player, this.minecraft.options.perspective > 0, this.minecraft.options.perspective == 2, tickDelta);
//        Frustum frustum = GlMatrixFrustum.get();
//        worldRenderer.method_21595(camera);
//        this.minecraft.getProfiler().swap("clear");
//        GlStateManager.viewport(0, 0, this.minecraft.window.getFramebufferWidth(), this.minecraft.window.getFramebufferHeight());
//        this.backgroundRenderer.renderBackground(camera, tickDelta);
//        GlStateManager.clear(16640, MinecraftClient.IS_SYSTEM_MAC);
//        this.minecraft.getProfiler().swap("culling");
//        VisibleRegion visibleRegion = new FrustumWithOrigin(frustum);
//        double d = camera.getPos().x;
//        double e = camera.getPos().y;
//        double f = camera.getPos().z;
//        visibleRegion.setOrigin(d, e, f);
//        if (this.minecraft.options.viewDistance >= 4) {
//            this.backgroundRenderer.applyFog(camera, -1);
//            this.minecraft.getProfiler().swap("sky");
//            GlStateManager.matrixMode(5889);
//            GlStateManager.loadIdentity();
//            GlStateManager.multMatrix(Matrix4f.method_4929(this.getFov(camera, tickDelta, true), (float)this.minecraft.window.getFramebufferWidth() / (float)this.minecraft.window.getFramebufferHeight(), 0.05F, this.minecraft.options.viewDistance * 16 * 2.0F));
//            GlStateManager.matrixMode(5888);
//            worldRenderer.renderSky(tickDelta);
//            GlStateManager.matrixMode(5889);
//            GlStateManager.loadIdentity();
//            GlStateManager.multMatrix(Matrix4f.method_4929(this.getFov(camera, tickDelta, true), (float)this.minecraft.window.getFramebufferWidth() / (float)this.minecraft.window.getFramebufferHeight(), 0.05F, this.minecraft.options.viewDistance * 16 * MathHelper.SQUARE_ROOT_OF_TWO));
//            GlStateManager.matrixMode(5888);
//        }
//
//        this.backgroundRenderer.applyFog(camera, 0);
//        GlStateManager.shadeModel(7425);
//        if (camera.getPos().y < 128.0D) {
//            this.renderAboveClouds(camera, worldRenderer, tickDelta, d, e, f);
//        }
//
//        this.minecraft.getProfiler().swap("prepareterrain");
//        this.backgroundRenderer.applyFog(camera, 0);
//        this.minecraft.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
//        DiffuseLighting.disable();
//        this.minecraft.getProfiler().swap("terrain_setup");
//        WorldPreview.clientWord.getChunkManager().getLightingProvider().doLightUpdates(2147483647, true, true);
//        worldRenderer.setUpTerrain(camera, visibleRegion, this.field_4021++, false);
//        this.minecraft.getProfiler().swap("updatechunks");
//        worldRenderer.updateChunks(endTime);
//        this.minecraft.getProfiler().swap("terrain");
//        GlStateManager.matrixMode(5888);
//        GlStateManager.pushMatrix();
//        GlStateManager.disableAlphaTest();
//        worldRenderer.renderLayer(RenderLayer.SOLID, camera);
//        GlStateManager.enableAlphaTest();
//        worldRenderer.renderLayer(RenderLayer.CUTOUT_MIPPED, camera);
//        this.minecraft.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).pushFilter(false, false);
//        worldRenderer.renderLayer(RenderLayer.CUTOUT, camera);
//        this.minecraft.getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).popFilter();
//        GlStateManager.shadeModel(7424);
//        GlStateManager.alphaFunc(516, 0.1F);
//        GlStateManager.matrixMode(5888);
//        GlStateManager.popMatrix();
//
//
//        this.minecraft.getProfiler().swap("translucent");
//        worldRenderer.renderLayer(RenderLayer.TRANSLUCENT, camera);
//        GlStateManager.shadeModel(7424);
//        GlStateManager.depthMask(true);
//        GlStateManager.enableCull();
//        GlStateManager.disableBlend();
//        GlStateManager.disableFog();
//        if (camera.getPos().y >= 128.0D) {
//            this.minecraft.getProfiler().swap("aboveClouds");
//            this.renderAboveClouds(camera, worldRenderer, tickDelta, d, e, f);
//        }
//    }

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
//
//    }
//    private void applyCameraTransformations(float tickDelta) {
//        GlStateManager.matrixMode(5889);
//        GlStateManager.loadIdentity();
//
//
//        GlStateManager.multMatrix(Matrix4f.method_4929(this.minecraft.options.fov, (float)this.minecraft.window.getFramebufferWidth() / (float)this.minecraft.window.getFramebufferHeight(), 0.05F, minecraft.options.viewDistance*16));
//        GlStateManager.matrixMode(5888);
//        GlStateManager.loadIdentity();
//
//
//
//
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
