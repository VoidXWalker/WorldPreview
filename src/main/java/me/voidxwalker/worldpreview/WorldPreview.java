package me.voidxwalker.worldpreview;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

public class WorldPreview implements ModInitializer {
   public static World world;
   public static CustomPlayerEntity player;
   public static ClientWorld clientWord;
   public static boolean inPreview;
   public static BlockPos spawnPos;
   public static int kill=0;
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
   public static final Object lock= new Object();
   public static Logger LOGGER = LogManager.getLogger();
   public static void log(Level level, String message) {
      LOGGER.log(level, message);
   }

   @Override
   public void onInitialize() {
      resetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
              translate("Reset","key.world_preview.reset"),
              InputUtil.Type.KEYSYM,
              GLFW.GLFW_KEY_H,
              "key.categories.misc"
      ));

      stopKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
              translate("Stop","key.world_preview.stop"),
              InputUtil.Type.KEYSYM,
              GLFW.GLFW_KEY_J,
              "key.categories.misc"
      ));
      cycleChunkMapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
              translate("Cycle Chunk Map","key.world_preview.cycle_chunkmap"),
              InputUtil.Type.KEYSYM,
              GLFW.GLFW_KEY_K,
              "key.categories.misc"
      ));
   }
   private String translate(String replacement,String key ){
      TranslatableText t=new TranslatableText(key,replacement);
      if(t.getString().equals(key)){
         return replacement;
      }
      return t.asString();
   }

}
