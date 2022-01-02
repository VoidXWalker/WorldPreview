package me.voidxwalker.worldpreview.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import me.voidxwalker.worldpreview.Main;
import me.voidxwalker.worldpreview.mixin.access.SpawnLocatingMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(LevelLoadingScreen.class)
public class LevelLoadingScreenMixin extends Screen {
    protected LevelLoadingScreenMixin(Text title) {
        super(title);
    }
    private ButtonWidget button;
    private boolean calculatedSpawn;
    @Inject(method = "render",at = @At(value="HEAD"),cancellable = true)
    public void renderPreview(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if(Main.world!=null&&Main.clientWord!=null&&Main.worldRenderer!=null&&Main.spawnPos!=null){
            if(!calculatedSpawn){
                this.button= this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 120 + -16, 204, 20, new TranslatableText("menu.returnToMenu"), (buttonWidgetx) -> {
                    buttonWidgetx.active = false;
                    Main.kill=true;
                }));
               calculateSpawn();
            }
            if(Main.clientWord.getChunkManager().getLoadedChunkCount()>15&&calculatedSpawn){
                MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().update(0);
                this.client.getProfiler().swap("camera");
                if(Main.camera==null){
                    Main.player.refreshPositionAndAngles(Main.player.getX(),Main.player.getY()+1.5,Main.player.getZ(),0.0F,0.0F);
                    Main.camera=new Camera();
                    Main.camera.update(Main.world,Main.player,false,false,0.2F);
                }
                MatrixStack matrixStack = new MatrixStack();
                matrixStack.peek().getModel().multiply(this.getBasicProjectionMatrix());
                Matrix4f matrix4f = matrixStack.peek().getModel();
                RenderSystem.matrixMode(5889);
                RenderSystem.loadIdentity();
                RenderSystem.multMatrix(matrix4f);
                RenderSystem.matrixMode(5888);
                MatrixStack m = new MatrixStack();
                m.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(Main.camera.getPitch()));
                m.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(Main.camera.getYaw() + 180.0F));
                Main.worldRenderer.render(m, 0.2F, 1000000, false, Main.camera, MinecraftClient.getInstance().gameRenderer, MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager(), matrix4f);
                Window window = this.client.getWindow();
                RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
                RenderSystem.matrixMode(5889);
                RenderSystem.loadIdentity();
                RenderSystem.ortho(0.0D, (double)window.getFramebufferWidth() / window.getScaleFactor(), (double)window.getFramebufferHeight() / window.getScaleFactor(), 0.0D, 1000.0D, 3000.0D);
                RenderSystem.matrixMode(5888);
                RenderSystem.loadIdentity();
                RenderSystem.translatef(0.0F, 0.0F, -2000.0F);
                DiffuseLighting.enableGuiDepthLighting();
                this.button.render(matrices,mouseX,mouseY,0);
                RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
                this.client.getProfiler().pop();
            }
        }
        ci.cancel();
    }
    private int calculateSpawnOffsetMultiplier(int horizontalSpawnArea) {
        return horizontalSpawnArea <= 16 ? horizontalSpawnArea - 1 : 17;
    }
    public Matrix4f getBasicProjectionMatrix() {
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.peek().getModel().loadIdentity();
        matrixStack.peek().getModel().multiply(Matrix4f.viewboxMatrix(client.options.fov, (float)this.client.getWindow().getFramebufferWidth() / (float)this.client.getWindow().getFramebufferHeight(), 0.05F, this.client.options.viewDistance*16 * 4.0F));
        return matrixStack.peek().getModel();
    }
    private void calculateSpawn(){
        BlockPos blockPos = Main.spawnPos;
        int i = Math.max(0, client.getServer().getSpawnRadius((ServerWorld) Main.world));
        int j = MathHelper.floor(Main.world.getWorldBorder().getDistanceInsideBorder((double)blockPos.getX(), (double)blockPos.getZ()));
        if (j < i) {
            i = j;
        }
        if (j <= 1) {
            i = 1;
        }
        long l = (long)(i * 2 + 1);
        long m = l * l;
        int k = m > 2147483647L ? Integer.MAX_VALUE : (int)m;
        int n = this.calculateSpawnOffsetMultiplier(k);
        int o = (new Random()).nextInt(k);
        Main.playerSpawn=o;
        for(int p = 0; p < k; ++p) {
            int q = (o + n * p) % k;
            int r = q % (i * 2 + 1);
            int s = q / (i * 2 + 1);
            if(true){
                BlockPos blockPos2 = SpawnLocatingMixin.callFindOverworldSpawn((ServerWorld) Main.world, blockPos.getX() + r - i, blockPos.getZ() + s - i, false);
                if (blockPos2 != null) {
                    Main.player.refreshPositionAndAngles(blockPos2, 0.0F, 0.0F);
                    if (Main.world.doesNotCollide(Main.player)) {
                        break;
                    }
                }
            }
        }
        calculatedSpawn=true;
    }

}
