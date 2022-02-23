package me.voidxwalker.worldpreview;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.WorldRenderer;

public class PreviewRenderer extends WorldRenderer {
    public boolean tgraete;
    public PreviewRenderer(MinecraftClient client, BufferBuilderStorage bufferBuilders) {

        super(client, bufferBuilders);
        tgraete=true;
    }
}
