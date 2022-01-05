package me.voidxwalker.worldpreview.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import me.voidxwalker.worldpreview.Main;
import me.voidxwalker.worldpreview.PreviewRenderer;
import me.voidxwalker.worldpreview.mixin.access.SpawnLocatingMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.Random;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen {
    @Shadow @Final private WorldGenerationProgressTracker progressProvider;

    @Shadow
    public static void drawChunkMap(MatrixStack matrixStack, WorldGenerationProgressTracker worldGenerationProgressTracker, int i, int j, int k, int l) {
    }

    @Shadow private long field_19101;

    protected LevelLoadingScreenMixin(Text title) {
        super(title);
    }

    private boolean calculatedSpawn;
    /**
     * @author Void_X_Walker
     */
    @Overwrite
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if(Main.world!=null&&Main.clientWord!=null&&Main.spawnPos!=null) {
            if(Main.worldRenderer==null){
                Main.worldRenderer=new PreviewRenderer(MinecraftClient.getInstance(), new BufferBuilderStorage());
                Main.worldRenderer.setWorld(Main.clientWord);
            }
            if (!calculatedSpawn) {
                Main.stopButton=false;
                this.initWidgets();
                calculateSpawn();
            }
            if (calculatedSpawn) {
                try {
                    MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().update(0);
                    this.client.getProfiler().swap("camera");
                    if (Main.camera == null) {
                        Main.player.refreshPositionAndAngles(Main.player.getX(), Main.player.getY() + 1.5, Main.player.getZ(), 0.0F, 0.0F);
                        Main.camera = new Camera();
                        Main.camera.update(Main.world, Main.player, false, false, 0.2F);
                        Main.player.refreshPositionAndAngles(Main.player.getX(), Main.player.getY() - 1.5, Main.player.getZ(), 0.0F, 0.0F);
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
                    if (!Main.world.doesNotCollide(Main.player)) {
                        calculateSpawn();
                        Main.camera.update(Main.world, Main.player, false, false, 0.2F);
                    }

                    Main.worldRenderer.render(m, 0.2F, 1000000, false, Main.camera, MinecraftClient.getInstance().gameRenderer, MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager(), matrix4f);
                    Main.worldRenderer.ticks++;
                    Window window = this.client.getWindow();
                    RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
                    RenderSystem.matrixMode(5889);
                    RenderSystem.loadIdentity();
                    RenderSystem.ortho(0.0D, (double) window.getFramebufferWidth() / window.getScaleFactor(), (double) window.getFramebufferHeight() / window.getScaleFactor(), 0.0D, 1000.0D, 3000.0D);
                    RenderSystem.matrixMode(5888);
                    RenderSystem.loadIdentity();
                    RenderSystem.translatef(0.0F, 0.0F, -2000.0F);
                    DiffuseLighting.enableGuiDepthLighting();
                    Iterator<AbstractButtonWidget> iterator =this.buttons.listIterator();
                    while(iterator.hasNext()){
                        iterator.next().render(matrices,mouseX,mouseY,delta);
                    }
                    RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
                    this.client.getProfiler().pop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
        }
        renderNormal(matrices);

    }

    private void renderNormal(MatrixStack matrices){
        this.renderBackground(matrices);
        String string = MathHelper.clamp(this.progressProvider.getProgressPercentage(), 0, 100) + "%";
        long l = Util.getMeasuringTimeMs();
        if (l - this.field_19101 > 2000L) {
            this.field_19101 = l;
            NarratorManager.INSTANCE.narrate((new TranslatableText("narrator.loading", string)).getString());
        }

        int i = this.width / 2;
        int j = this.height / 2;
        drawChunkMap(matrices, this.progressProvider, i, j + 30, 2, 0);
        TextRenderer var10002 = this.textRenderer;
        this.drawCenteredString(matrices, var10002, string, i, j - 9 / 2 - 30, 16777215);
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
    private void initWidgets(){
        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 24 - 16, 204, 20, new TranslatableText("menu.returnToGame"), (buttonWidgetx) -> {

        }));
        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 48 - 16, 98, 20, new TranslatableText("gui.advancements"), (buttonWidgetx) -> {

        }));
        this.addButton(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 48 - 16, 98, 20, new TranslatableText("gui.stats"), (buttonWidgetx) -> {

        }));

        this .addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 72 - 16, 98, 20, new TranslatableText("menu.sendFeedback"), (buttonWidgetx) -> {

        }));
        this.addButton(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 72 - 16, 98, 20, new TranslatableText("menu.reportBugs"), (buttonWidgetx) -> {

        }));
        this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 96 - 16, 98, 20, new TranslatableText("menu.options"), (buttonWidgetx) -> {

        }));
        ButtonWidget buttonWidget = this.addButton(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 96 - 16, 98, 20, new TranslatableText("menu.shareToLan"), (buttonWidgetx) -> {

        }));
        buttonWidget.active = this.client.isIntegratedServerRunning() && !this.client.getServer().isRemote();
        ButtonWidget button= this.addButton(new ButtonWidget(this.width / 2 - 102, this.height / 4 + 120 - 16, 204, 20, new TranslatableText("menu.returnToMenu"), (buttonWidgetx) -> {
            if(!Main.stopButton){
                Main.kill = true;
                client.getServer().shutdown();
                buttonWidgetx.active = false;
            }
        }));
        if (!this.client.isInSingleplayer()) {
            button.setMessage(new TranslatableText("menu.disconnect"));
        }
    }
    public void resize(MinecraftClient client, int width, int height) {
        this.init(client, width, height);
        this.initWidgets();
    }
    private void calculateSpawn(){
        BlockPos blockPos = Main.spawnPos;
        int i = Math.max(0, client.getServer().getSpawnRadius((ServerWorld) Main.world));
        int j = MathHelper.floor(Main.world.getWorldBorder().getDistanceInsideBorder(blockPos.getX(), blockPos.getZ()));
        if (j < i) {
            i = j;
        }
        if (j <= 1) {
            i = 1;
        }
        long l = i * 2L + 1;
        long m = l * l;
        int k = m > 2147483647L ? Integer.MAX_VALUE : (int)m;
        int n = this.calculateSpawnOffsetMultiplier(k);
        int o = (new Random()).nextInt(k);
        Main.playerSpawn=o;
        for(int p = 0; p < k; ++p) {
            int q = (o + n * p) % k;
            int r = q % (i * 2 + 1);
            int s = q / (i * 2 + 1);
            BlockPos blockPos2 = SpawnLocatingMixin.callFindOverworldSpawn((ServerWorld) Main.world, blockPos.getX() + r - i, blockPos.getZ() + s - i, false);
            if (blockPos2 != null) {
                Main.player.refreshPositionAndAngles(blockPos2, 0.0F, 0.0F);
                if (Main.world.doesNotCollide(Main.player)) {
                    break;
                }
            }
        }
        calculatedSpawn=true;
    }

}
