package me.voidxwalker.worldpreview.mixin.client;

//import net.minecraft.client.world.ClientChunkManager;
//import net.minecraft.world.chunk.WorldChunk;

//@Mixin(ClientChunkManager.ClientChunkMap.class)
public class ClientChunkMapMixin { //implements ChunkSetter {
//    @Shadow private int field_19143;
//
//    @Shadow @Final private AtomicReferenceArray<WorldChunk> chunks;
//
//    @Override
//    public void set(int index, @Nullable WorldChunk chunk) {
//        WorldChunk worldChunk = (WorldChunk)this.chunks.getAndSet(index, chunk);
//        if (worldChunk != null) {
//            --this.field_19143;
//            ((ClientChunkManagerMixin)this).getWorld().unloadBlockEntities(worldChunk);
//        }
//
//        if (chunk != null) {
//            ++this.field_19143;
//        }
//    }
}
