package me.voidxwalker.worldpreview;

import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Main {
   public static World world;
   public static Entity player;
   public static ClientWorld clientWord;
   public static BlockPos spawnPos;
   public static int kill=0;
   public static int playerSpawn;
   public static Camera camera;
   public static PreviewRenderer worldRenderer;
   public static boolean existingWorld;
   public static boolean showMenu;
   public static KeyBinding resetKey;
   public static KeyBinding cycleChunkMapKey;
   public static int chunkMapPos;

}
