package me.voidxwalker.worldpreview.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.texture.SpriteAtlasTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.OptionalDouble;

@Mixin(RenderPhase.class)
public abstract class RenderPhaseMixin {
    @Shadow @Final @Mutable protected static RenderPhase.LineWidth FULL_LINE_WIDTH;

    @Shadow @Final @Mutable protected static RenderPhase.Target ITEM_TARGET;

    @Shadow @Final @Mutable protected static RenderPhase.Target CLOUDS_TARGET;

    @Shadow @Final @Mutable protected static RenderPhase.Target WEATHER_TARGET;

    @Shadow @Final @Mutable protected static RenderPhase.Target PARTICLES_TARGET;

    @Shadow @Final @Mutable protected static RenderPhase.Target TRANSLUCENT_TARGET;

    @Shadow @Final @Mutable protected static RenderPhase.Target MAIN_TARGET;

    @Shadow @Final @Mutable protected static RenderPhase.Cull ENABLE_CULLING;

    @Shadow @Final @Mutable protected static RenderPhase.Cull DISABLE_CULLING;

    @Shadow @Final @Mutable protected static RenderPhase.Lightmap DISABLE_LIGHTMAP;

    @Shadow @Final @Mutable protected static RenderPhase.Lightmap ENABLE_LIGHTMAP;

    @Shadow @Final @Mutable protected static RenderPhase.Overlay ENABLE_OVERLAY_COLOR;

    @Shadow @Final @Mutable protected static RenderPhase.DiffuseLighting DISABLE_DIFFUSE_LIGHTING;

    @Shadow @Final @Mutable protected static RenderPhase.Transparency ADDITIVE_TRANSPARENCY;

    @Shadow @Final @Mutable protected static RenderPhase.Overlay DISABLE_OVERLAY_COLOR;

    @Shadow @Final @Mutable protected static RenderPhase.DepthTest EQUAL_DEPTH_TEST;

    @Shadow @Final @Mutable protected static RenderPhase.DiffuseLighting ENABLE_DIFFUSE_LIGHTING;

    @Shadow @Final @Mutable protected static RenderPhase.Texturing ENTITY_GLINT_TEXTURING;

    @Shadow @Final @Mutable protected static RenderPhase.DepthTest ALWAYS_DEPTH_TEST;

    @Shadow @Final @Mutable protected static RenderPhase.Texture BLOCK_ATLAS_TEXTURE;

    @Shadow @Final @Mutable protected static RenderPhase.Transparency CRUMBLING_TRANSPARENCY;

    @Shadow @Final @Mutable protected static RenderPhase.WriteMaskState DEPTH_MASK;

    @Shadow @Final @Mutable protected static RenderPhase.WriteMaskState COLOR_MASK;

    @Shadow @Final @Mutable protected static RenderPhase.Layering NO_LAYERING;

    @Shadow @Final @Mutable protected static RenderPhase.WriteMaskState ALL_MASK;

    @Shadow @Final @Mutable protected static RenderPhase.DepthTest LEQUAL_DEPTH_TEST;

    @Shadow @Final @Mutable protected static RenderPhase.Layering POLYGON_OFFSET_LAYERING;

    @Shadow @Final @Mutable protected static RenderPhase.Layering VIEW_OFFSET_Z_LAYERING;

    @Shadow @Final @Mutable protected static RenderPhase.Fog NO_FOG;

    @Shadow @Final @Mutable protected static RenderPhase.Fog FOG;

    @Shadow @Final @Mutable protected static RenderPhase.Fog BLACK_FOG;

    @Shadow @Final @Mutable protected static RenderPhase.Target OUTLINE_TARGET;

    @Shadow
    protected static void setupGlintTexturing(float scale) {
    }

    @Shadow @Final @Mutable protected static RenderPhase.Texturing GLINT_TEXTURING;

    @Shadow @Final @Mutable protected static RenderPhase.Texture MIPMAP_BLOCK_ATLAS_TEXTURE;

    @Shadow @Final @Mutable protected static RenderPhase.Texture NO_TEXTURE;

    @Shadow @Final @Mutable protected static RenderPhase.Transparency NO_TRANSPARENCY;

    @Shadow @Final @Mutable protected static RenderPhase.Texturing OUTLINE_TEXTURING;

    @Shadow @Final @Mutable protected static RenderPhase.Texturing DEFAULT_TEXTURING;

    @Inject(method = "<clinit>",at = @At(value = "HEAD"),cancellable = true)
    private static void e(CallbackInfo ci){
            MIPMAP_BLOCK_ATLAS_TEXTURE = new RenderPhase.Texture(SpriteAtlasTexture.BLOCK_ATLAS_TEX, false, true);
            BLOCK_ATLAS_TEXTURE = new RenderPhase.Texture(SpriteAtlasTexture.BLOCK_ATLAS_TEX, false, false);
            NO_TEXTURE = new RenderPhase.Texture();
            DEFAULT_TEXTURING = new RenderPhase.Texturing("default_texturing", () -> {
            }, () -> {
            });
            OUTLINE_TEXTURING = new RenderPhase.Texturing("outline_texturing", () -> {
                RenderSystem.setupOutline();
            }, () -> {
                RenderSystem.teardownOutline();
            });
            GLINT_TEXTURING = new RenderPhase.Texturing("glint_texturing", () -> {
                setupGlintTexturing(8.0F);
            }, () -> {
                RenderSystem.matrixMode(5890);
                RenderSystem.popMatrix();
                RenderSystem.matrixMode(5888);
            });
            ENTITY_GLINT_TEXTURING = new RenderPhase.Texturing("entity_glint_texturing", () -> {
                setupGlintTexturing(0.16F);
            }, () -> {
                RenderSystem.matrixMode(5890);
                RenderSystem.popMatrix();
                RenderSystem.matrixMode(5888);
            });
            ENABLE_LIGHTMAP = new RenderPhase.Lightmap(true);
            DISABLE_LIGHTMAP = new RenderPhase.Lightmap(false);
            ENABLE_OVERLAY_COLOR = new RenderPhase.Overlay(true);
            DISABLE_OVERLAY_COLOR = new RenderPhase.Overlay(false);
            ENABLE_DIFFUSE_LIGHTING = new RenderPhase.DiffuseLighting(true);
            DISABLE_DIFFUSE_LIGHTING = new RenderPhase.DiffuseLighting(false);
            ENABLE_CULLING = new RenderPhase.Cull(true);
            DISABLE_CULLING = new RenderPhase.Cull(false);
            ALWAYS_DEPTH_TEST = new RenderPhase.DepthTest("always", 519);
            EQUAL_DEPTH_TEST = new RenderPhase.DepthTest("==", 514);
            LEQUAL_DEPTH_TEST = new RenderPhase.DepthTest("<=", 515);
            ALL_MASK = new RenderPhase.WriteMaskState(true, true);
            COLOR_MASK = new RenderPhase.WriteMaskState(true, false);
            DEPTH_MASK = new RenderPhase.WriteMaskState(false, true);
            NO_LAYERING = new RenderPhase.Layering("no_layering", () -> {
            }, () -> {
            });
            POLYGON_OFFSET_LAYERING = new RenderPhase.Layering("polygon_offset_layering", () -> {
                RenderSystem.polygonOffset(-1.0F, -10.0F);
                RenderSystem.enablePolygonOffset();
            }, () -> {
                RenderSystem.polygonOffset(0.0F, 0.0F);
                RenderSystem.disablePolygonOffset();
            });
            VIEW_OFFSET_Z_LAYERING = new RenderPhase.Layering("view_offset_z_layering", () -> {
                RenderSystem.pushMatrix();
                RenderSystem.scalef(0.99975586F, 0.99975586F, 0.99975586F);
            }, RenderSystem::popMatrix);
            NO_FOG = new RenderPhase.Fog("no_fog", () -> {
            }, () -> {
            });
            FOG = new RenderPhase.Fog("fog", () -> {
                BackgroundRenderer.setFogBlack();
                RenderSystem.enableFog();
            }, () -> {
                RenderSystem.disableFog();
            });
            BLACK_FOG = new RenderPhase.Fog("black_fog", () -> {
                RenderSystem.fog(2918, 0.0F, 0.0F, 0.0F, 1.0F);
                RenderSystem.enableFog();
            }, () -> {
                BackgroundRenderer.setFogBlack();
                RenderSystem.disableFog();
            });
            MAIN_TARGET = new RenderPhase.Target("main_target", () -> {
            }, () -> {
            });
            OUTLINE_TARGET = new RenderPhase.Target("outline_target", () -> {
                MinecraftClient.getInstance().worldRenderer.getEntityOutlinesFramebuffer().beginWrite(false);
            }, () -> {
                MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
            });
            TRANSLUCENT_TARGET = new RenderPhase.Target("translucent_target", () -> {
                if (MinecraftClient.isFabulousGraphicsOrBetter()) {
                    MinecraftClient.getInstance().worldRenderer.getTranslucentFramebuffer().beginWrite(false);
                }

            }, () -> {
                if (MinecraftClient.isFabulousGraphicsOrBetter()) {
                    MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
                }

            });
            PARTICLES_TARGET = new RenderPhase.Target("particles_target", () -> {
                if (MinecraftClient.isFabulousGraphicsOrBetter()) {
                    MinecraftClient.getInstance().worldRenderer.getParticlesFramebuffer().beginWrite(false);
                }

            }, () -> {
                if (MinecraftClient.isFabulousGraphicsOrBetter()) {
                    MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
                }

            });
            WEATHER_TARGET = new RenderPhase.Target("weather_target", () -> {
                if (MinecraftClient.isFabulousGraphicsOrBetter()) {
                    MinecraftClient.getInstance().worldRenderer.getWeatherFramebuffer().beginWrite(false);
                }

            }, () -> {
                if (MinecraftClient.isFabulousGraphicsOrBetter()) {
                    MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
                }

            });
            CLOUDS_TARGET = new RenderPhase.Target("clouds_target", () -> {
                if (MinecraftClient.isFabulousGraphicsOrBetter()) {
                    MinecraftClient.getInstance().worldRenderer.getCloudsFramebuffer().beginWrite(false);
                }

            }, () -> {
                if (MinecraftClient.isFabulousGraphicsOrBetter()) {
                    MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
                }

            });
            ITEM_TARGET = new RenderPhase.Target("item_entity_target", () -> {
                if (MinecraftClient.isFabulousGraphicsOrBetter()) {
                    MinecraftClient.getInstance().worldRenderer.getEntityFramebuffer().beginWrite(false);
                }

            }, () -> {
                if (MinecraftClient.isFabulousGraphicsOrBetter()) {
                    MinecraftClient.getInstance().getFramebuffer().beginWrite(false);
                }

            });
            FULL_LINE_WIDTH = new RenderPhase.LineWidth(OptionalDouble.of(1.0D));
            ci.cancel();
        }
}
