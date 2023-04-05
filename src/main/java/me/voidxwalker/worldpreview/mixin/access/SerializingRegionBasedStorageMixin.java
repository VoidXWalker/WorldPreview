package me.voidxwalker.worldpreview.mixin.access;

import net.minecraft.world.storage.SerializingRegionBasedStorage;
import net.minecraft.world.storage.StorageIoWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SerializingRegionBasedStorage.class)
public interface SerializingRegionBasedStorageMixin {
    @Accessor
    StorageIoWorker getWorker();
}
