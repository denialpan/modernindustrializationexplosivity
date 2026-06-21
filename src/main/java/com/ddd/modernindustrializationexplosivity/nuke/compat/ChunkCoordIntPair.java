package com.ddd.modernindustrializationexplosivity.nuke.compat;

import net.minecraft.world.level.ChunkPos;

public class ChunkCoordIntPair {
   public final int chunkXPos;
   public final int chunkZPos;
   private static final String __OBFID = "CL_00000133";

   public ChunkCoordIntPair(int p_i1947_1_, int p_i1947_2_) {
      this.chunkXPos = p_i1947_1_;
      this.chunkZPos = p_i1947_2_;
   }

   public static long chunkXZ2Int(int p_77272_0_, int p_77272_1_) {
      return (long)p_77272_0_ & 4294967295L | ((long)p_77272_1_ & 4294967295L) << 32;
   }

   @Override
   public int hashCode() {
      int i = 1664525 * this.chunkXPos + 1013904223;
      int j = 1664525 * (this.chunkZPos ^ -559038737) + 1013904223;
      return i ^ j;
   }

   @Override
   public boolean equals(Object p_equals_1_) {
      if (this == p_equals_1_) {
         return true;
      } else {
         return !(p_equals_1_ instanceof ChunkCoordIntPair chunkcoordintpair)
            ? false
            : this.chunkXPos == chunkcoordintpair.chunkXPos && this.chunkZPos == chunkcoordintpair.chunkZPos;
      }
   }

   public int getCenterXPos() {
      return (this.chunkXPos << 4) + 8;
   }

   public int getCenterZPosition() {
      return (this.chunkZPos << 4) + 8;
   }

   public ChunkPos func_151349_a(int p_151349_1_) {
      return new ChunkPos(this.getCenterXPos(), this.getCenterZPosition());
   }

   @Override
   public String toString() {
      return "[" + this.chunkXPos + ", " + this.chunkZPos + "]";
   }
}
