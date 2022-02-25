
package me.voidxwalker.worldpreview.mixin.client;

import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import net.minecraft.world.dimension.Dimension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.BiFunction;

@Mixin(World.class)
public class WorldMixin<R,T,U> {
    @Shadow @Final public Dimension dimension;

    @Redirect(method = "<init>",at = @At(value = "INVOKE",target = "Ljava/util/function/BiFunction;apply(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    public R sodiumCompatibility(BiFunction instance, T t, U u){
        if(WorldPreview.camera==null&& WorldPreview.world!=null&& WorldPreview.spawnPos!=null){

            return  (R)new ClientChunkManager((ClientWorld) (Object)this, 16);
        }
        return (R) instance.apply(this, this.dimension);
    }
}
