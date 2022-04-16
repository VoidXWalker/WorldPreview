package me.voidxwalker.worldpreview.mixin.server;

//import net.minecraft.client.gui.screen.LevelLoadingScreen;
import com.jcraft.jorbis.Block;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
        import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
        import org.spongepowered.asm.mixin.injection.Redirect;

        import java.util.List;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin  {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;getTopPosition(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos setSpawnPos(ServerWorld instance, BlockPos blockPos) {
        return WorldPreview.spawnPos;
    }
}