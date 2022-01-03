package me.voidxwalker.worldpreview;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CustomPlayerEntity extends Entity  {
    public CustomPlayerEntity(EntityType<?> type, World world, BlockPos pos,float yaw, float pitch) {
        super(type, world);
        this.setPos(pos.getX(),pos.getY(),pos.getZ());
        this.yaw=yaw;
        this.pitch=pitch;
        this.prevX=pos.getX();
        this.prevY=pos.getY();
        this.prevZ=pos.getZ();
        this.chunkX=pos.getX()%8;
        this.chunkY=pos.getY()%8;
        this.chunkZ=pos.getZ()%8;
    }
    @Override
    protected void initDataTracker() {}
    @Override
    protected void readCustomDataFromTag(CompoundTag tag) {}
    @Override
    protected void writeCustomDataToTag(CompoundTag tag) {}
    @Override
    public Packet<?> createSpawnPacket() {
        return null;
    }
}
