package me.voidxwalker.worldpreview;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CustomPlayerEntity extends Entity  {
    public boolean calculatedSpawn;
    public CustomPlayerEntity(EntityType<?> type, World world, BlockPos pos,float yaw, float pitch) {
        super(type, world);
        this.x=pos.getX();
        this.y=pos.getY();
        this.z=pos.getZ();
        calculatedSpawn=false;
        this.yaw=yaw;
        this.pitch=pitch;
        this.prevX=pos.getX();
        this.prevY=pos.getY();
        this.prevZ=pos.getZ();
    }

    @Override protected void initDataTracker() {}

    @Override protected void readCustomDataFromTag(CompoundTag tag) {}

    @Override protected void writeCustomDataToTag(CompoundTag tag) {}

    @Override public Packet<?> createSpawnPacket() {
        return null;
    }
}
