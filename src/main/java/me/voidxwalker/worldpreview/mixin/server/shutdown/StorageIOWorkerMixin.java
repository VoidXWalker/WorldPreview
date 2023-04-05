package me.voidxwalker.worldpreview.mixin.server.shutdown;

import me.voidxwalker.worldpreview.IFastCloseable;
import net.minecraft.util.thread.TaskExecutor;
import net.minecraft.util.thread.TaskQueue;
import net.minecraft.world.storage.RegionBasedStorage;
import net.minecraft.world.storage.StorageIoWorker;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

@Mixin(StorageIoWorker.class)
public abstract class StorageIOWorkerMixin implements IFastCloseable {
    @Shadow @Final private AtomicBoolean closed;

    @Shadow protected abstract CompletableFuture<Void> shutdown();

    @Shadow private boolean active;

    @Shadow @Final private Thread thread;

    @Override
    public void fastClose() {
        if (this.closed.compareAndSet(false, true)) {
            this.active = false;
            LockSupport.unpark(this.thread);
        }
    }
}
