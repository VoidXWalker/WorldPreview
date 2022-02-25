package me.voidxwalker.worldpreview.mixin.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.mixin.access.WorldRendererMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.options.CloudRenderMode;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.Iterator;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen {
    private boolean worldpreview_showMenu;
    private BackgroundRenderer backgroundRenderer;
    protected LevelLoadingScreenMixin(Text title) {
        super(title);
    }
    @Inject(method = "<init>",at = @At(value = "TAIL"))
    public void worldpreview_init(WorldGenerationProgressTracker progressProvider, CallbackInfo ci){
        backgroundRenderer= new BackgroundRenderer(MinecraftClient.getInstance().gameRenderer);
        WorldPreview.freezePreview=false;
        KeyBinding.unpressAll();
    }
    private int field_4021;
    @Redirect(method = "render",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/screen/LevelLoadingScreen;renderBackground()V"))
    public void stopBackgroundRender(LevelLoadingScreen instance){
        if(WorldPreview.camera==null){
            instance.renderBackground();
        }
    }
    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 2)
    public int moveLoadingScreen(int i){
        if(WorldPreview.camera==null){
            return i;
        }
        return worldpreview_getChunkMapPos().x;
    }
    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 3)
    public int moveLoadingScreen2(int i){
        if(WorldPreview.camera==null){
            return i;
        }
        return worldpreview_getChunkMapPos().y;
    }
    @Inject(method = "render",at=@At("HEAD"),cancellable = true)
    public void render(int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if(WorldPreview.worldRenderer==null){
            WorldPreview.worldRenderer=new WorldRenderer(MinecraftClient.getInstance());
        }
        if(WorldPreview.world!=null&& WorldPreview.clientWord!=null&&WorldPreview.player!=null&&!WorldPreview.freezePreview) {
            if(((me.voidxwalker.worldpreview.mixin.access.WorldRendererMixin)WorldPreview.worldRenderer).getWorld()==null&& WorldPreview.calculatedSpawn){
                WorldPreview.worldRenderer.setWorld(WorldPreview.clientWord);
                WorldPreview.showMenu=true;
                this.worldpreview_showMenu=true;
                this.worldpreview_initWidgets();
            }
            if (((WorldRendererMixin)WorldPreview.worldRenderer).getWorld()!=null) {
                KeyBinding.unpressAll();
                WorldPreview.kill=0;
                if(this.worldpreview_showMenu!= WorldPreview.showMenu){
                    if(!WorldPreview.showMenu){
                        this.children.clear();
                    }
                    else {
                        this.worldpreview_initWidgets();
                    }
                    this.worldpreview_showMenu= WorldPreview.showMenu;
                }
                if (WorldPreview.camera == null) {
                    WorldPreview.player.refreshPositionAndAngles(WorldPreview.player.x, WorldPreview.player.y +(WorldPreview.player.getBoundingBox().y2-WorldPreview.player.getBoundingBox().y1), WorldPreview.player.z, 0.0F, 0.0F);
                    WorldPreview.camera = new Camera();
                    WorldPreview.camera.update(WorldPreview.world, WorldPreview.player, this.minecraft.options.perspective > 0, this.minecraft.options.perspective == 2, 0.2F);
                    WorldPreview.player.refreshPositionAndAngles(WorldPreview.player.x, WorldPreview.player.y - 1.5, WorldPreview.player.z, 0.0F, 0.0F);
                    WorldPreview.inPreview=true;
                    WorldPreview.log(Level.INFO,"Starting Preview at ("+ WorldPreview.player.x + ", "+(double)Math.floor(WorldPreview.player.y)+ ", "+ WorldPreview.player.z+")");
                }

                renderWorld(delta,(long)(1000000000 / 60 / 4) );
                Window window = this.minecraft.window;
                GlStateManager.clear(256, MinecraftClient.IS_SYSTEM_MAC);
                GlStateManager.matrixMode(5889);
                GlStateManager.loadIdentity();
                GlStateManager.ortho(0.0D, (double) window.getFramebufferWidth() / window.getScaleFactor(), (double) window.getFramebufferHeight() / window.getScaleFactor(), 0.0D, 1000.0D, 3000.0D);
                GlStateManager.matrixMode(5888);
                GlStateManager.loadIdentity();
                GlStateManager.translatef(0.0F, 0.0F, -2000.0F);
                this.renderPauseMenu(mouseX,mouseY,delta);

            }
        }
    }
    public void renderWorld(float tickDelta, long endTime) {
        GlStateManager.enableDepthTest();
        GlStateManager.enableAlphaTest();
        GlStateManager.alphaFunc(516, 0.5F);
        this.renderCenter(tickDelta, endTime);
    }

    private void renderCenter(float tickDelta, long endTime) {
       WorldRenderer worldRenderer = WorldPreview.worldRenderer;

        GlStateManager.enableCull();
        this.minecraft.getProfiler().swap("camera");
        this.applyCameraTransformations(tickDelta);
        Camera camera = WorldPreview.camera;
        camera.update(WorldPreview.clientWord, (Entity)(WorldPreview.player), this.minecraft.options.perspective > 0, this.minecraft.options.perspective == 2, tickDelta);
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
            GlStateManager.multMatrix(Matrix4f.method_4929(minecraft.options.fov, (float)this.minecraft.window.getFramebufferWidth() / (float)this.minecraft.window.getFramebufferHeight(), 0.05F, minecraft.options.viewDistance *16* 2.0F));
            GlStateManager.matrixMode(5888);
            worldRenderer.renderSky(tickDelta);
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            GlStateManager.multMatrix(Matrix4f.method_4929(minecraft.options.fov, (float)this.minecraft.window.getFramebufferWidth() / (float)this.minecraft.window.getFramebufferHeight(), 0.05F, minecraft.options.viewDistance *16* MathHelper.SQUARE_ROOT_OF_TWO));
            GlStateManager.matrixMode(5888);
        }

       this.backgroundRenderer.applyFog(camera, 0);
        GlStateManager.shadeModel(7425);
        if (camera.getPos().y < 128.0D) {
            this.renderAboveClouds(camera, tickDelta, d, e, f);
        }

        this.minecraft.getProfiler().swap("prepareterrain");
       this.backgroundRenderer.applyFog(camera, 0);
        this.minecraft.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
        DiffuseLighting.disable();
        this.minecraft.getProfiler().swap("terrain_setup");
        WorldPreview.clientWord.getChunkManager().getLightingProvider().doLightUpdates(Integer.MAX_VALUE, true, true);
        worldRenderer.setUpTerrain(camera, visibleRegion,this.field_4021++, false);
        this.minecraft.getProfiler().swap("updatechunks");
        WorldPreview.worldRenderer.updateChunks(endTime);
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
        GlStateManager.pushMatrix();
        GlStateManager.matrixMode(5888);
        GlStateManager.popMatrix();
        GlStateManager.depthMask(false);
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.alphaFunc(516, 0.1F);
        this.backgroundRenderer.applyFog(camera, 0);
        GlStateManager.enableBlend();
        GlStateManager.depthMask(false);
        this.minecraft.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
        GlStateManager.shadeModel(7425);
        this.minecraft.getProfiler().swap("translucent");
        worldRenderer.renderLayer(RenderLayer.TRANSLUCENT, camera);
        GlStateManager.shadeModel(7424);
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.disableFog();
        if (camera.getPos().y >= 128.0D) {
            this.minecraft.getProfiler().swap("aboveClouds");
            this.renderAboveClouds(camera,tickDelta, d, e, f);
        }



    }
    private void renderAboveClouds(Camera camera,  float tickDelta, double cameraX, double cameraY, double cameraZ) {
        if (this.minecraft.options.getCloudRenderMode() != CloudRenderMode.OFF) {
            this.minecraft.getProfiler().swap("clouds");
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            GlStateManager.multMatrix(Matrix4f.method_4929(minecraft.options.fov, (float)this.minecraft.window.getFramebufferWidth() / (float)this.minecraft.window.getFramebufferHeight(), 0.05F, minecraft.options.viewDistance *16 * 4.0F));
            GlStateManager.matrixMode(5888);
            GlStateManager.pushMatrix();
            this.backgroundRenderer.applyFog(camera, 0);
            WorldPreview.worldRenderer.renderClouds(tickDelta, cameraX, cameraY, cameraZ);
            GlStateManager.disableFog();
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5889);
            GlStateManager.loadIdentity();
            GlStateManager.multMatrix(Matrix4f.method_4929(minecraft.options.fov, (float)this.minecraft.window.getFramebufferWidth() / (float)this.minecraft.window.getFramebufferHeight(), 0.05F, minecraft.options.viewDistance *16* MathHelper.SQUARE_ROOT_OF_TWO));
            GlStateManager.matrixMode(5888);
        }

    }
    private void applyCameraTransformations(float tickDelta) {
        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();


        GlStateManager.multMatrix(Matrix4f.method_4929(this.minecraft.options.fov, (float)this.minecraft.window.getFramebufferWidth() / (float)this.minecraft.window.getFramebufferHeight(), 0.05F, minecraft.options.viewDistance*16));
        GlStateManager.matrixMode(5888);
        GlStateManager.loadIdentity();




    }
    private void renderPauseMenu(int mouseX, int mouseY, float delta){
        if(WorldPreview.showMenu){
            Iterator<AbstractButtonWidget> iterator =this.buttons.listIterator();
            while(iterator.hasNext()){
                iterator.next().render(mouseX,mouseY,delta);
            }
        }
        else {
            this.drawCenteredString( minecraft.textRenderer, new TranslatableText("menu.paused").getString(), this.width / 2, 10, 16777215);
        }
    }




    private Point worldpreview_getChunkMapPos(){
        switch (WorldPreview.chunkMapPos){
            case 1:
                return new Point(this.width -45,this.height -75);
            case 2:
                return new Point(this.width -45,105);
            case 3:
                return new Point(45,105);
            default:
                return new Point(45,this.height -75);
        }
    }

    private void worldpreview_initWidgets(){
        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 24 - 16, 204, 20, new TranslatableText("menu.returnToGame").getString(), (ignored) -> {}));
        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 48 - 16, 98, 20, new TranslatableText("gui.advancements").getString(), (ignored) -> {}));
        this.addButton(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 48 - 16, 98, 20, new TranslatableText("gui.stats").getString(), (ignored) -> {}));

        this .addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 72 - 16, 98, 20, new TranslatableText("menu.sendFeedback").getString(), (ignored) -> {}));
        this.addButton(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 72 - 16, 98, 20, new TranslatableText("menu.reportBugs").getString(), (ignored) -> {}));
        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 96 - 16, 98, 20, new TranslatableText("menu.options").getString(), (ignored) -> {}));
        this.addButton(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 96 - 16, 98, 20, new TranslatableText("menu.shareToLan").getString(), (ignored) -> {}));
        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 120 - 16, 204, 20, new TranslatableText("menu.returnToMenu").getString(), (buttonWidgetX) -> {
                WorldPreview.kill = -1;
                buttonWidgetX.active = false;
        }));
    }

    public void resize(MinecraftClient client, int width, int height) {
        this.init(client, width, height);
        this.worldpreview_initWidgets();
    }
}
