package com.ddd.modernindustrializationexplosivity.nuke.explosion;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public abstract class EntityExplosionChunkloading extends Entity {
   private ChunkPos loadedChunk;

   public EntityExplosionChunkloading(EntityType<?> type, Level world) {
      super(type, world);
   }

   public void init() {
      if (!this.level().isClientSide && this.loadedChunk == null) {
         ChunkPos chunkPos = this.chunkPosition();
         this.loadedChunk = chunkPos;
         this.forceChunk(chunkPos);
      }
   }

   public void loadChunk(int x, int z) {
      if (!this.level().isClientSide) {
         ChunkPos chunkPos = new ChunkPos(x, z);
         if (this.loadedChunk == null || !this.loadedChunk.equals(chunkPos)) {
            this.loadedChunk = chunkPos;
            this.forceChunk(chunkPos);
         }
      }
   }

   public void clearChunkLoader() {
      if (!this.level().isClientSide && this.loadedChunk != null) {
         ServerLevel world = (ServerLevel)this.level();
         world.getChunkSource().removeRegionTicket(TicketType.FORCED, this.loadedChunk, 1, this.loadedChunk);
         this.loadedChunk = null;
      }
   }

   private void forceChunk(ChunkPos chunkPos) {
      ServerLevel world = (ServerLevel)this.level();
      world.getChunkSource().addRegionTicket(TicketType.FORCED, chunkPos, 1, this.loadedChunk);
      world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, true);
   }

   public void remove(RemovalReason reason) {
      this.clearChunkLoader();
      super.remove(reason);
   }
}
