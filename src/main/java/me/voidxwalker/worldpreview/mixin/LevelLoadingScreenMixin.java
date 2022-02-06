package me.voidxwalker.worldpreview.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import me.voidxwalker.worldpreview.PreviewRenderer;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import org.apache.logging.log4j.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.ListIterator;
import java.util.Objects;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen {

    private boolean drawingPreview=false;

    private boolean showMenu;

    protected LevelLoadingScreenMixin(Text title) {
        super(title);
    }
    @Redirect(method = "render",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/gui/screen/LevelLoadingScreen;renderBackground(Lnet/minecraft/client/util/math/MatrixStack;)V"))
    public void stopBackgroundRender(LevelLoadingScreen instance, MatrixStack matrixStack){
        if(!drawingPreview){
            instance.renderBackground(matrixStack);
        }
    }
    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 2)
    public int moveLoadingScreen(int i){
        if(!drawingPreview){
            return i;
        }
        return getChunkMapPos().x;
    }
    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 3)
    public int moveLoadingScreen2(int i){
        if(!drawingPreview){
            return i;
        }
        return getChunkMapPos().y;
    }
    @Inject(method = "render",at=@At("HEAD"),cancellable = true)
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if(WorldPreview.stop){
            drawingPreview=false;
        }
        if(WorldPreview.world!=null&& WorldPreview.clientWord!=null&&!WorldPreview.stop) {

            if(WorldPreview.worldRenderer==null){
                WorldPreview.worldRenderer=new PreviewRenderer(MinecraftClient.getInstance(), new BufferBuilderStorage());
            }
            if(WorldPreview.worldRenderer.world==null&& WorldPreview.player.calculatedSpawn){
                WorldPreview.worldRenderer.loadWorld(WorldPreview.clientWord);
                WorldPreview.showMenu=true;
                this.showMenu=true;
                this.initWidgets();
            }
            if (WorldPreview.worldRenderer.world!=null) {
                drawingPreview=true;
                if(this.showMenu!= WorldPreview.showMenu){
                    if(!WorldPreview.showMenu){
                        this.clearChildren();
                    }
                    else {
                        this.initWidgets();

                    }
                    this.showMenu= WorldPreview.showMenu;
                }
                MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().update(0);
                if (WorldPreview.camera == null) {
                    WorldPreview.player.refreshPositionAndAngles(WorldPreview.player.getX(), WorldPreview.player.getY() + 1.5, WorldPreview.player.getZ(), 0.0F, 0.0F);
                    WorldPreview.camera = new Camera();
                    WorldPreview.camera.update(WorldPreview.world, WorldPreview.player, false, false, 0.2F);
                    WorldPreview.player.refreshPositionAndAngles(WorldPreview.player.getX(), WorldPreview.player.getY() - 1.5, WorldPreview.player.getZ(), 0.0F, 0.0F);
                    WorldPreview.inPreview=true;
                    WorldPreview.log(Level.INFO,"Starting Preview at ("+ WorldPreview.player.getX() + ", "+WorldPreview.player.getY()+ ", "+ WorldPreview.player.getZ()+")");
                }
                MatrixStack matrixStack = new MatrixStack();
                matrixStack.peek().getModel().multiply(this.getBasicProjectionMatrix());
                Matrix4f matrix4f = matrixStack.peek().getModel();
                RenderSystem.setProjectionMatrix(matrix4f);
                MatrixStack m = new MatrixStack();
                m.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(WorldPreview.camera.getPitch()));
                m.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(WorldPreview.camera.getYaw() + 180.0F));
                WorldPreview.worldRenderer.render(m, 0.2F, 1000000, false, WorldPreview.camera, MinecraftClient.getInstance().gameRenderer, MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager(), matrix4f);
                WorldPreview.worldRenderer.ticks++;
                Window window = this.client.getWindow();
                RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
                Matrix4f matrix4f2 = Matrix4f.projectionMatrix(0.0F, (float)((double)window.getFramebufferWidth() / window.getScaleFactor()), 0.0F, (float)((double)window.getFramebufferHeight() / window.getScaleFactor()), 1000.0F, 3000.0F);
                RenderSystem.setProjectionMatrix(matrix4f2);
                MatrixStack matrixStack2 = RenderSystem.getModelViewStack();
                matrixStack2.loadIdentity();
                matrixStack2.translate(0.0D, 0.0D, -2000.0D);
                RenderSystem.applyModelViewMatrix();
                DiffuseLighting.enableGuiDepthLighting();
                this.renderPauseMenu(matrices,mouseX,mouseY,delta);
                RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);

            }
        }
    }

    private void renderPauseMenu(MatrixStack matrices, int mouseX, int mouseY, float delta){
        if(WorldPreview.showMenu){
            ListIterator<? extends Element> iterator =this.children().listIterator();
            while(iterator.hasNext()){
                Object next = iterator.next();
                if(next instanceof ButtonWidget){
                   ((ButtonWidget)(next)).render(matrices,mouseX,mouseY,delta);
                }

            }
        }
        else {
            this.drawCenteredText(matrices, this.textRenderer, new TranslatableText("menu.paused"), this.width / 2, 10, 16777215);
        }
    }

    private Point getChunkMapPos(){
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

    public Matrix4f getBasicProjectionMatrix() {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.peek().getModel().loadIdentity();
        matrixStack.peek().getModel().multiply(Matrix4f.viewboxMatrix(client.options.fov, (float)this.client.getWindow().getFramebufferWidth() / (float)this.client.getWindow().getFramebufferHeight(), 0.05F, this.client.options.viewDistance*16 * 4.0F));
        return matrixStack.peek().getModel();
    }

    private void initWidgets(){
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 24 - 16, 204, 20, new TranslatableText("menu.returnToGame"), (ignored) -> {}));
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 48 - 16, 98, 20, new TranslatableText("gui.advancements"), (ignored) -> {}));
        this.addDrawableChild(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 48 - 16, 98, 20, new TranslatableText("gui.stats"), (ignored) -> {}));

        this.addDrawableChild(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 72 - 16, 98, 20, new TranslatableText("menu.sendFeedback"), (ignored) -> {}));
        this.addDrawableChild(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 72 - 16, 98, 20, new TranslatableText("menu.reportBugs"), (ignored) -> {}));
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 96 - 16, 98, 20, new TranslatableText("menu.options"), (ignored) -> {}));
        this.addDrawableChild(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 96 - 16, 98, 20, new TranslatableText("menu.shareToLan"), (ignored) -> {}));
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 120 - 16, 204, 20, new TranslatableText("menu.returnToMenu"), (buttonWidgetX) -> {
                WorldPreview.kill = -1;
                buttonWidgetX.active = false;
        }));
    }

    public void resize(MinecraftClient client, int width, int height) {
        this.init(client, width, height);
        this.initWidgets();
    }
}
