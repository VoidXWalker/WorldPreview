package me.voidxwalker.worldpreview;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CustomPlayerEntity extends Entity  {
    public boolean calculatedSpawn;
    public CustomPlayerEntity(EntityType<?> type, World world, BlockPos pos,float yaw, float pitch) {
        super(type, world);
        this.setPos(pos.getX(),pos.getY(),pos.getZ());
        calculatedSpawn=false;
        this.setYaw(yaw);
        this.setPitch(pitch);
        this.prevX=pos.getX();
        this.prevY=pos.getY();
        this.prevZ=pos.getZ();
    }

    @Override protected void initDataTracker() {}

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {

    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {

    }


    @Override public Packet<?> createSpawnPacket() {
        return null;
    }
}
