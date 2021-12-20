package me.voidxwalker.worldpreview.mixin;


import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import me.voidxwalker.worldpreview.Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.UserCache;
import net.minecraft.util.registry.RegistryTracker;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.Proxy;

@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin extends MinecraftServer{
    @Shadow private boolean paused;

    public IntegratedServerMixin(Thread thread, RegistryTracker.Modifiable modifiable, LevelStorage.Session session, SaveProperties saveProperties, ResourcePackManager<ResourcePackProfile> resourcePackManager, Proxy proxy, DataFixer dataFixer, ServerResourceManager serverResourceManager, MinecraftSessionService minecraftSessionService, GameProfileRepository gameProfileRepository, UserCache userCache, WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory) {
        super(thread, modifiable, session, saveProperties, resourcePackManager, proxy, dataFixer, serverResourceManager, minecraftSessionService, gameProfileRepository, userCache, worldGenerationProgressListenerFactory);
    }

    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/server/integrated/IntegratedServer;paused:Z", opcode = Opcodes.PUTFIELD))
    private void injected(IntegratedServer instance, boolean value) {

       if(Main.forcedPaused){
           this.paused=true;
           if(MinecraftClient.getInstance().currentScreen==null){
               MinecraftClient.getInstance().openPauseMenu(false);
           }

           this.getNetworkIo().tick();
           ServerWorld serverWorld = this.getOverworld();
           ServerChunkManager serverChunkManager = serverWorld.getChunkManager();
           serverChunkManager.getLightingProvider().setTaskBatchSize(500);
           if(serverChunkManager.getTotalChunksLoadedCount() >= 441){
               Main.forcedPaused=false;
           }
       }
       else {
           paused=MinecraftClient.getInstance().getNetworkHandler() != null && MinecraftClient.getInstance().isPaused();
       }

    }
}
