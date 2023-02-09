package me.voidxwalker.worldpreview.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import me.voidxwalker.worldpreview.OldSodiumCompatibility;
import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.mixin.access.WorldRendererMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import org.apache.logging.log4j.Level;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.Iterator;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen {

    @Shadow public abstract void render(MatrixStack matrices, int mouseX, int mouseY, float delta);

    private boolean worldpreview_showMenu;
    protected LevelLoadingScreenMixin(Text title) {
        super(title);
    }
    @Inject(method = "<init>",at = @At(value = "TAIL"))
    public void worldpreview_init(WorldGenerationProgressTracker progressProvider, CallbackInfo ci){
        WorldPreview.calculatedSpawn=true;
        WorldPreview.freezePreview=false;
        KeyBinding.unpressAll();
    }
    @Redirect(method = "render",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/screen/LevelLoadingScreen;renderBackground(Lnet/minecraft/client/util/math/MatrixStack;)V"))
    public void worldpreview_stopBackgroundRender(LevelLoadingScreen instance, MatrixStack matrixStack){
        if(WorldPreview.camera==null){
            instance.renderBackground(matrixStack);
        }
    }
    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 2)
    public int worldpreview_moveLoadingScreen(int i){
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
    @Inject(method = "render",at=@At("HEAD"))
    public void worldpreview_render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if(WorldPreview.world!=null&& WorldPreview.clientWord!=null&&WorldPreview.player!=null&&!WorldPreview.freezePreview) {
            if(((WorldRendererMixin)WorldPreview.worldRenderer).getWorld()==null&& WorldPreview.calculatedSpawn){
                ((OldSodiumCompatibility)WorldPreview.worldRenderer).worldpreview_setWorldSafe(WorldPreview.clientWord);
                WorldPreview.showMenu=true;
                this.worldpreview_showMenu=true;
                this.worldpreview_initWidgets();
            }
            if (((WorldRendererMixin)WorldPreview.worldRenderer).getWorld()!=null) {
                KeyBinding.unpressAll();
                WorldPreview.kill=0;
                if(this.worldpreview_showMenu!= WorldPreview.showMenu){
                    if(!WorldPreview.showMenu){
                        this.clearChildren();
                    }
                    else {
                        this.worldpreview_initWidgets();
                    }
                    this.worldpreview_showMenu= WorldPreview.showMenu;
                }
                MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().update(0);
                if (WorldPreview.camera == null) {
                    WorldPreview.player.refreshPositionAndAngles(WorldPreview.player.getX(), WorldPreview.player.getY() +(WorldPreview.player.getBoundingBox().maxY-WorldPreview.player.getBoundingBox().minY), WorldPreview.player.getZ(), 0.0F, 0.0F);
                    WorldPreview.camera = new Camera();
                    WorldPreview.camera.update(WorldPreview.world, WorldPreview.player, !this.client.options.getPerspective().isFirstPerson(), this.client.options.getPerspective().isFrontView(), 0.2F);
                    WorldPreview.player.refreshPositionAndAngles(WorldPreview.player.getX(), WorldPreview.player.getY() - 1.5, WorldPreview.player.getZ(), 0.0F, 0.0F);
                    WorldPreview.inPreview=true;
                    WorldPreview.log(Level.INFO,"Starting Preview at ("+ WorldPreview.player.getX() + ", "+(double)Math.floor(WorldPreview.player.getY())+ ", "+ WorldPreview.player.getZ()+")");
                }
                MatrixStack matrixStack = new MatrixStack();
                matrixStack.multiplyPositionMatrix(this.worldpreview_getBasicProjectionMatrix());
                Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();
                RenderSystem.setProjectionMatrix(matrix4f);
                MatrixStack m = new MatrixStack();
                m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(WorldPreview.camera.getPitch()));
                m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(WorldPreview.camera.getYaw() + 180.0F));
                WorldPreview.worldRenderer.render(m, 0.2F, 1000000, false, WorldPreview.camera, MinecraftClient.getInstance().gameRenderer, MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager(), matrix4f);
                Window window = this.client.getWindow();
                RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
                Matrix4f matrix4f2 = new Matrix4f().setOrtho(0.0f, (float)((double)window.getFramebufferWidth() / window.getScaleFactor()), (float)((double)window.getFramebufferHeight() / window.getScaleFactor()), 0.0f, 1000.0f, 3000.0f);
                RenderSystem.setProjectionMatrix(matrix4f2);
                MatrixStack matrixStack2 = RenderSystem.getModelViewStack();
                matrixStack2.loadIdentity();
                matrixStack2.translate(0.0f, 0.0f, -2000.0f);
                RenderSystem.applyModelViewMatrix();
                DiffuseLighting.enableGuiDepthLighting();
                this.worldpreview_renderPauseMenu(matrices,mouseX,mouseY,delta);
            }
        }
    }

    private void worldpreview_renderPauseMenu(MatrixStack matrices, int mouseX, int mouseY, float delta){
        if(WorldPreview.showMenu){
            render(matrices,mouseX,mouseY,delta);
        }
        else {
            drawCenteredText(matrices, this.textRenderer, Text.translatable("menu.paused"), this.width / 2, 10, 16777215);
        }
    }

    private Point worldpreview_getChunkMapPos(){
        return switch (WorldPreview.chunkMapPos) {
            case 1 -> new Point(this.width - 45, this.height - 75);
            case 2 -> new Point(this.width - 45, 105);
            case 3 -> new Point(45, 105);
            default -> new Point(45, this.height - 75);
        };
    }

    public Matrix4f worldpreview_getBasicProjectionMatrix() {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.peek().getPositionMatrix().identity();
        matrixStack.peek().getPositionMatrix().mul(new Matrix4f().setPerspective(client.options.getFov().getValue(), (float)this.client.getWindow().getFramebufferWidth() / (float)this.client.getWindow().getFramebufferHeight(), 0.05F, this.client.options.getViewDistance().getValue()*16 * 4.0F));
        return matrixStack.peek().getPositionMatrix();
    }

    private void worldpreview_initWidgets(){
        this.addDrawableChild( ButtonWidget.builder(Text.translatable("menu.returnToGame"), (ignored) -> {}).dimensions(this.width / 2 - 102, this.height / 4 + 24 - 16, 204, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.advancements"), (ignored) -> {}).dimensions(this.width / 2 - 102, this.height / 4 + 48 - 16, 98, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.stats"), (ignored) -> {}).dimensions(this.width / 2 + 4, this.height / 4 + 48 - 16, 98, 20 ).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("menu.sendFeedback"), (ignored) -> {}).dimensions( this.width / 2 - 102, this.height / 4 + 72 - 16, 98, 20 ).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("menu.reportBugs"), (ignored) -> {}).dimensions (this.width / 2 + 4, this.height / 4 + 72 - 16, 98, 20).build() );
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("menu.options"), (ignored) -> {}).dimensions (this.width / 2 - 102, this.height / 4 + 96 - 16, 98, 20).build() );
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("menu.shareToLan"), (ignored) -> {}).dimensions(this.width / 2 + 4, this.height / 4 + 96 - 16, 98, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("menu.returnToMenu"), (buttonWidgetX) -> {
            client.getSoundManager().stopAll();
            WorldPreview.kill = -1;
            buttonWidgetX.active = false;
        }).dimensions(this.width / 2 - 102, this.height / 4 + 120 - 16, 204, 20).build() );
    }

    public void resize(MinecraftClient client, int width, int height) {
        this.init(client, width, height);
        this.worldpreview_initWidgets();
    }
}
