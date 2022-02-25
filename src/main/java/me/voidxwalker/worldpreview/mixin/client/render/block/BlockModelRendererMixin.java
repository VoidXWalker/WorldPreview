package me.voidxwalker.worldpreview.mixin.client.render.block;

import me.voidxwalker.worldpreview.OldSodiumCompatibility;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

@Mixin(BlockModelRenderer.class)
public abstract class BlockModelRendererMixin implements OldSodiumCompatibility {
    @Shadow public abstract boolean renderSmooth(BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, MatrixStack buffer, VertexConsumer vertexConsumer, boolean cull, Random random, long seed, int overlay);

    @Shadow public abstract boolean renderFlat(BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, MatrixStack buffer, VertexConsumer vertexConsumer, boolean cull, Random random, long l, int i);

    @Override
    public boolean worldpreview_renderBlockSafe(BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, MatrixStack matrix, VertexConsumer vertexConsumer, boolean cull, Random random, long seed, int overlay) {
        boolean bl = MinecraftClient.isAmbientOcclusionEnabled() && state.getLuminance() == 0 && model.useAmbientOcclusion();
        Vec3d vec3d = state.getModelOffset(world, pos);
        matrix.translate(vec3d.x, vec3d.y, vec3d.z);

        try {
            return bl ? this.renderSmooth(world, model, state, pos, matrix, vertexConsumer, cull, random, seed, overlay) : this.renderFlat(world, model, state, pos, matrix, vertexConsumer, cull, random, seed, overlay);
        } catch (Throwable var17) {
            CrashReport crashReport = CrashReport.create(var17, "Tesselating block model");
            CrashReportSection crashReportSection = crashReport.addElement("Block model being tesselated");
            CrashReportSection.addBlockInfo(crashReportSection, world, pos, state);
            crashReportSection.add("Using AO", (Object)bl);
            throw new CrashException(crashReport);
        }
    }
    @Override
    public void worldpreview_setWorldSafe(ClientWorld world){

    }
    
}
