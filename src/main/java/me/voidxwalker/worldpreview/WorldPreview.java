package me.voidxwalker.worldpreview;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldPreview {
   private static final ModContainer mod = FabricLoader.getInstance().getModContainer("worldpreview").orElseThrow(NullPointerException::new);
   public static String MOD_NAME = mod.getMetadata().getName();
   public static World world;
   public static CustomPlayerEntity player;
   public static ClientWorld clientWord;
   public static boolean inPreview;
   public static BlockPos spawnPos;
   public static int kill = 0;
   public static int playerSpawn;
   public static Camera camera;
   public static PreviewRenderer worldRenderer;
   public static boolean existingWorld;
   public static boolean showMenu;
   public static boolean stop;
   public static KeyBinding resetKey;
   public static KeyBinding stopKey;
   public static KeyBinding cycleChunkMapKey;
   public static int chunkMapPos;
   public static final Object lock = new Object();
   public static Logger LOGGER = LogManager.getLogger();
   
   public static void log(Level level, String message) {
      LOGGER.log(level, message);
   }
}
