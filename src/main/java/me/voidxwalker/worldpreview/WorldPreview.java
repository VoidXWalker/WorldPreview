package me.voidxwalker.worldpreview;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.*;
import java.util.HashMap;
import java.util.Properties;

public class WorldPreview implements ClientModInitializer {
   public static World world;
   public static ClientPlayerEntity player;
   public static ClientWorld clientWorld;
   public static boolean inPreview;
   public static BlockPos spawnPos;
   public static int kill=0;
   public static WorldRenderer worldRenderer;
   public static boolean existingWorld;
   public static boolean loadedSpawn;
   public static KeyBinding resetKey;
   public static boolean canFreeze;
   public static KeyBinding freezeKey;
   public static boolean freezePreview;
   private static final File configFile = FabricLoader.getInstance().getConfigDir().resolve("worldpreview.properties").toFile();
   public static int loadingScreenFPS;
   public static int loadingScreenPollingRate;
   public static int worldGenLogInterval;
   public static int worldGenFreezePercentage;
   public static final boolean HAS_ATUM = FabricLoader.getInstance().isModLoaded("atum");
   public static final Object lock = new Object();
   public static final Logger LOGGER = LogManager.getLogger();

   public static void log(String message) {
      LOGGER.log(Level.INFO, message);
   }

   @Override
   public void onInitializeClient() {
      init();
      loadOrCreateConfigFile();
   }

   public static void init() {
      WorldPreview.inPreview = false;
      WorldPreview.freezePreview = false;
      WorldPreview.canFreeze = false;
      WorldPreview.loadedSpawn = false;
      WorldPreview.world = null;
      if (WorldPreview.worldRenderer != null) {
          WorldPreview.worldRenderer.setWorld(null);
      }
      WorldPreview.clientWorld = null;
      WorldPreview.player = null;
      KeyBinding.unpressAll();
   }

   private static void loadOrCreateConfigFile() {
        try {
            Properties properties = new Properties();
            if (configFile.exists()) {
                FileInputStream fileInputStream = new FileInputStream(configFile);
                properties.load(fileInputStream);
                log("Found worldpreview.properties file in .minecraft/config. Reading it in.");
            } else {
                log("No worldpreview.properties file found in .minecraft/config. Creating one with default values..");
            }
            loadingScreenFPS = Math.max(5, Integer.parseInt(properties.getProperty("loading_screen_fps", "60")));
            loadingScreenPollingRate = Integer.parseInt(properties.getProperty("loading_screen_polling_rate", "30"));
            worldGenLogInterval = Math.max(50, Math.min(1000, Integer.parseInt(properties.getProperty("worldgen_log_interval", "200"))));
            worldGenFreezePercentage = Math.max(50, Math.min(100, Integer.parseInt(properties.getProperty("worldgen_freeze_percentage", "70"))));
            FileWriter fileWriter = new FileWriter(configFile);
            fileWriter.write("# FPS during the preview. Chunks are only loaded during active frames, so this also affects CPU. Minimum: 5fps\n");
            fileWriter.write("loading_screen_fps = " + loadingScreenFPS + "\n\n");
            fileWriter.write("# Input detections per second during preview. Lowers GPU usage, but inputs may not be detected if it's too low.\n");
            fileWriter.write("# Note that inputs will also be checked every frame, so this value should be greater than or equal to loading_screen_fps.\n");
            fileWriter.write("loading_screen_polling_rate = " + loadingScreenPollingRate + "\n\n");
            fileWriter.write("# time in milliseconds between each world generation percentage log. Minimum: 50, Maximum: 1000\n");
            fileWriter.write("# Smaller values allow worldgen_freeze_percentage to update more accurately.\n");
            fileWriter.write("worldgen_log_interval = " + worldGenLogInterval + "\n\n");
            fileWriter.write("# Worldgen percentage to freeze at. Must be a value between 50 and 100 since preview starts at 50%.\n");
            fileWriter.write("# Frozen previews are still rendered at loading_screen_fps, but no additional terrain is loaded.\n");
            fileWriter.write("worldgen_freeze_percentage = " + worldGenFreezePercentage);
            fileWriter.close();
        } catch (IOException e) {
            LOGGER.error("Failed to read worldpreview.properties", e);
        }
   }
}
