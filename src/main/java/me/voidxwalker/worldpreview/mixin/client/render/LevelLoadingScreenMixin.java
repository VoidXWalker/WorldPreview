package me.voidxwalker.worldpreview.mixin.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import me.voidxwalker.worldpreview.OldSodiumCompatibility;
import me.voidxwalker.worldpreview.WorldPreview;
import me.voidxwalker.worldpreview.mixin.access.WorldRendererMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.Matrix4f;
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
        return worldpreview_getChunkMapPos().x;
    }

    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 3)
    public int moveLoadingScreen2(int i){
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
                        this.children.clear();
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
                    WorldPreview.camera.update(WorldPreview.world, WorldPreview.player, this.client.options.perspective > 0, this.client.options.perspective == 2, 0.2F);
                    WorldPreview.player.refreshPositionAndAngles(WorldPreview.player.getX(), WorldPreview.player.getY() - 1.5, WorldPreview.player.getZ(), 0.0F, 0.0F);
                    WorldPreview.inPreview=true;
                    WorldPreview.log(Level.INFO,"Starting Preview at ("+ WorldPreview.player.getX() + ", "+(double)Math.floor(WorldPreview.player.getY())+ ", "+ WorldPreview.player.getZ()+")");
                }
                MatrixStack matrixStack = new MatrixStack();
                matrixStack.peek().getModel().multiply(this.worldpreview_getBasicProjectionMatrix());
                Matrix4f matrix4f = matrixStack.peek().getModel();
                RenderSystem.matrixMode(5889);
                RenderSystem.loadIdentity();
                RenderSystem.multMatrix(matrix4f);
                RenderSystem.matrixMode(5888);
                MatrixStack m = new MatrixStack();
                m.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(WorldPreview.camera.getPitch()));
                m.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(WorldPreview.camera.getYaw() + 180.0F));
                WorldPreview.worldRenderer.render(m, 0.2F, 1000000, false, WorldPreview.camera, MinecraftClient.getInstance().gameRenderer, MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager(), matrix4f);
                Window window = this.client.getWindow();
                RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
                RenderSystem.matrixMode(5889);
                RenderSystem.loadIdentity();
                RenderSystem.ortho(0.0D, (double) window.getFramebufferWidth() / window.getScaleFactor(), (double) window.getFramebufferHeight() / window.getScaleFactor(), 0.0D, 1000.0D, 3000.0D);
                RenderSystem.matrixMode(5888);
                RenderSystem.loadIdentity();
                RenderSystem.translatef(0.0F, 0.0F, -2000.0F);
                DiffuseLighting.enableGuiDepthLighting();
                this.worldpreview_renderPauseMenu(matrices,mouseX,mouseY,delta);
            }
        }
    }

    private void worldpreview_renderPauseMenu(MatrixStack matrices, int mouseX, int mouseY, float delta){
        if(WorldPreview.showMenu){
            Iterator<AbstractButtonWidget> iterator =this.buttons.listIterator();
            while(iterator.hasNext()){
                iterator.next().render(matrices,mouseX,mouseY,delta);
            }
        }
        else {
            this.drawCenteredText(matrices, this.textRenderer, new TranslatableText("menu.paused"), this.width / 2, 10, 16777215);
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

    public Matrix4f worldpreview_getBasicProjectionMatrix() {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.peek().getModel().loadIdentity();
        matrixStack.peek().getModel().multiply(Matrix4f.viewboxMatrix(client.options.fov, (float)this.client.getWindow().getFramebufferWidth() / (float)this.client.getWindow().getFramebufferHeight(), 0.05F, this.client.options.viewDistance*16 * 4.0F));
        return matrixStack.peek().getModel();
    }

    private void worldpreview_initWidgets(){
        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 24 - 16, 204, 20, new TranslatableText("menu.returnToGame"), (ignored) -> {}));
        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 48 - 16, 98, 20, new TranslatableText("gui.advancements"), (ignored) -> {}));
        this.addButton(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 48 - 16, 98, 20, new TranslatableText("gui.stats"), (ignored) -> {}));
        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 72 - 16, 98, 20, new TranslatableText("menu.sendFeedback"), (ignored) -> {}));
        this.addButton(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 72 - 16, 98, 20, new TranslatableText("menu.reportBugs"), (ignored) -> {}));
        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 96 - 16, 98, 20, new TranslatableText("menu.options"), (ignored) -> {}));
        this.addButton(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 96 - 16, 98, 20, new TranslatableText("menu.shareToLan"), (ignored) -> {}));
        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 120 - 16, 204, 20, new TranslatableText("menu.returnToMenu"), (buttonWidgetX) -> {
           client.getSoundManager().stopAll();
            WorldPreview.kill = -1;
                buttonWidgetX.active = false;
        }));
    }

    public void resize(MinecraftClient client, int width, int height) {
        this.init(client, width, height);
        this.worldpreview_initWidgets();
    }
}
