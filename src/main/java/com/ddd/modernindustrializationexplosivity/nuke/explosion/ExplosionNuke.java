package com.ddd.modernindustrializationexplosivity.nuke.explosion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.phys.Vec3;
import com.ddd.modernindustrializationexplosivity.nuke.compat.ChunkCoordIntPair;

public class ExplosionNuke {
   public HashMap<ChunkCoordIntPair, List<ExplosionNuke.FloatTriplet>> perChunk = new HashMap<>();
   public List<ChunkCoordIntPair> orderedChunks = new ArrayList<>();
   private final List<ChunkCoordIntPair> fluidCleanupChunks = new ArrayList<>();
   private int nextFluidCleanupChunk;
   private ExplosionNuke.CoordComparator comparator = new ExplosionNuke.CoordComparator();
   int posX;
   int posY;
   int posZ;
   Level world;
   int strength;
   int length;
   int gspNumMax;
   int gspNum;
   double gspX;
   double gspY;
   public boolean isAusf3Complete = false;

   public ExplosionNuke(Level world, int x, int y, int z, int strength, int speed, int length) {
      this.world = world;
      this.posX = x;
      this.posY = y;
      this.posZ = z;
      this.strength = strength;
      this.length = length;
      this.gspNumMax = (int)((Math.PI * 5.0 / 2.0) * Math.pow((double)this.strength, 2.0));
      this.gspNum = 1;
      this.gspX = Math.PI;
      this.gspY = 0.0;
      this.collectFluidCleanupChunks();
   }

   private void collectFluidCleanupChunks() {
      int minChunkX = Math.floorDiv(this.posX - this.length, 16);
      int maxChunkX = Math.floorDiv(this.posX + this.length, 16);
      int minChunkZ = Math.floorDiv(this.posZ - this.length, 16);
      int maxChunkZ = Math.floorDiv(this.posZ + this.length, 16);
      double radiusSquared = (double)(this.length * this.length);

      for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
         for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            double nearestX = Math.clamp((double)this.posX, (double)(chunkX << 4), (double)((chunkX << 4) + 16));
            double nearestZ = Math.clamp((double)this.posZ, (double)(chunkZ << 4), (double)((chunkZ << 4) + 16));
            double dx = nearestX - (double)this.posX;
            double dz = nearestZ - (double)this.posZ;
            if (dx * dx + dz * dz <= radiusSquared) {
               this.fluidCleanupChunks.add(new ChunkCoordIntPair(chunkX, chunkZ));
            }
         }
      }
   }

   private void generateGspUp() {
      if (this.gspNum < this.gspNumMax) {
         int k = this.gspNum + 1;
         double hk = -1.0 + 2.0 * ((double)k - 1.0) / ((double)this.gspNumMax - 1.0);
         this.gspX = Math.acos(hk);
         double prev_lon = this.gspY;
         double lon = prev_lon + 3.6 / Math.sqrt((double)this.gspNumMax) / Math.sqrt(1.0 - hk * hk);
         this.gspY = lon % (Math.PI * 2);
      } else {
         this.gspX = 0.0;
         this.gspY = 0.0;
      }

      this.gspNum++;
   }

   private Vec3 getSpherical2cartesian() {
      double dx = Math.sin(this.gspX) * Math.cos(this.gspY);
      double dz = Math.sin(this.gspX) * Math.sin(this.gspY);
      double dy = Math.cos(this.gspX);
      return new Vec3(dx, dz, dy);
   }

   public void collectTip(int count) {
      int amountProcessed = 0;

      while (this.gspNumMax >= this.gspNum) {
         Vec3 vec = this.getSpherical2cartesian();
         int length = (int)Math.ceil((double)this.strength);
         float res = (float)this.strength;
         ExplosionNuke.FloatTriplet lastPos = null;
         HashSet<ChunkCoordIntPair> chunkCoords = new HashSet<>();

         for (int i = 0; i < length && i <= this.length; i++) {
            float x0 = (float)((double)this.posX + vec.x * (double)i);
            float y0 = (float)((double)this.posY + vec.y * (double)i);
            float z0 = (float)((double)this.posZ + vec.z * (double)i);
            int iX = (int)Math.floor((double)x0);
            int iY = (int)Math.floor((double)y0);
            int iZ = (int)Math.floor((double)z0);
            double fac = 100.0 - (double)i / (double)length * 100.0;
            fac *= 0.07;
            BlockState block = this.world.getBlockState(new BlockPos(iX, iY, iZ));
            if (!block.liquid()) {
               res = (float)((double)res - Math.pow((double)masqueradeResistance(block.getBlock()), 7.5 - fac));
            }

            if (res > 0.0F && block.getBlock() != Blocks.AIR) {
               lastPos = new ExplosionNuke.FloatTriplet(x0, y0, z0);
               ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(iX >> 4, iZ >> 4);
               chunkCoords.add(chunkPos);
            }

            if (res <= 0.0F || i + 1 >= this.length || i == length - 1) {
               break;
            }
         }

         for (ChunkCoordIntPair pos : chunkCoords) {
            List<ExplosionNuke.FloatTriplet> triplets = this.perChunk.get(pos);
            if (triplets == null) {
               triplets = new ArrayList<>();
               this.perChunk.put(pos, triplets);
            }

            triplets.add(lastPos);
         }

         this.generateGspUp();
         if (++amountProcessed >= count) {
            return;
         }
      }

      this.orderedChunks.addAll(this.perChunk.keySet());
      this.orderedChunks.sort(this.comparator);
      this.isAusf3Complete = true;
   }

   public static float masqueradeResistance(Block block) {
      if (block == Blocks.SANDSTONE) {
         return Blocks.STONE.getExplosionResistance();
      } else {
         return block == Blocks.OBSIDIAN ? Blocks.STONE.getExplosionResistance() * 3.0F : block.getExplosionResistance();
      }
   }

   public void processChunk() {
      if (!this.perChunk.isEmpty()) {
         ChunkCoordIntPair coord = this.orderedChunks.get(0);
         List<ExplosionNuke.FloatTriplet> list = this.perChunk.get(coord);
         HashSet<BlockPos> toRem = new HashSet<>();
         HashSet<BlockPos> toRemTips = new HashSet<>();
         HashSet<BlockPos> toDisplace = new HashSet<>();
         int chunkX = coord.chunkXPos;
         int chunkZ = coord.chunkZPos;
         // The prior distance estimate could begin after a ray had already crossed a
         // chunk edge, leaving fluid strips on chunk borders. Every recorded ray is
         // now scanned from its origin and only its matching chunk segment is used.
         int enter = 0;

         for (ExplosionNuke.FloatTriplet triplet : list) {
            float x = triplet.xCoord;
            float y = triplet.yCoord;
            float z = triplet.zCoord;
            Vec3 vec = new Vec3((double)(x - (float)this.posX), (double)(y - (float)this.posY), (double)(z - (float)this.posZ));
            double pX = vec.x / vec.length();
            double pY = vec.y / vec.length();
            double pZ = vec.z / vec.length();
            int tipX = (int)Math.floor((double)x);
            int tipY = (int)Math.floor((double)y);
            int tipZ = (int)Math.floor((double)z);
            boolean inChunk = false;

            for (int i = enter; (double)i < vec.length(); i++) {
               int x0 = (int)Math.floor((double)this.posX + pX * (double)i);
               int y0 = (int)Math.floor((double)this.posY + pY * (double)i);
               int z0 = (int)Math.floor((double)this.posZ + pZ * (double)i);
               if (x0 >> 4 == chunkX && z0 >> 4 == chunkZ) {
                  inChunk = true;
                  if (!this.world.isEmptyBlock(new BlockPos(x0, y0, z0))) {
                     BlockPos pos = new BlockPos(x0, y0, z0);
                     if (!this.shouldDestroyAt(x0, y0, z0)) {
                        if (toDisplace.size() < 64 && this.shouldDisplaceAt(pos, this.world.getBlockState(pos))) {
                           toDisplace.add(pos);
                        }
                        continue;
                     }
                     if (x0 == tipX && y0 == tipY && z0 == tipZ) {
                        toRemTips.add(pos);
                     }

                     toRem.add(pos);
                  }
               } else if (inChunk) {
                  break;
               }
            }
         }

         for (BlockPos pos : toRem) {
            if (toRemTips.contains(pos)) {
               this.handleTip(pos.getX(), pos.getY(), pos.getZ());
            } else {
               this.world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
         }

         for (BlockPos pos : toDisplace) {
            if (!toRem.contains(pos)) {
               this.displaceBlock(pos);
            }
         }

         this.perChunk.remove(coord);
         this.orderedChunks.remove(0);
      }
   }

   protected void handleTip(int x, int y, int z) {
      this.world.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
   }

   public boolean hasFluidCleanupRemaining() {
      return this.nextFluidCleanupChunk < this.fluidCleanupChunks.size();
   }

   public ChunkCoordIntPair getNextFluidCleanupChunk() {
      return this.hasFluidCleanupRemaining() ? this.fluidCleanupChunks.get(this.nextFluidCleanupChunk) : null;
   }

   public void processFluidCleanupChunk() {
      if (this.hasFluidCleanupRemaining()) {
         ChunkCoordIntPair chunk = this.fluidCleanupChunks.get(this.nextFluidCleanupChunk++);
         this.clearChunkFluids(chunk.chunkXPos, chunk.chunkZPos);
      }
   }

   private void clearChunkFluids(int chunkX, int chunkZ) {
      int minY = Math.max(this.world.getMinBuildHeight(), this.posY - this.length);
      int maxY = Math.min(this.world.getMaxBuildHeight() - 1, this.posY + this.length);
      int minX = chunkX << 4;
      int minZ = chunkZ << 4;
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

      for (int y = minY; y <= maxY; y++) {
         for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
               this.clearFluidIfInsideBlast(pos.set(x, y, z));
            }
         }
      }
   }

   private void clearFluidIfInsideBlast(BlockPos pos) {
      double dx = (double)pos.getX() + 0.5 - (double)this.posX;
      double dy = (double)pos.getY() + 0.5 - (double)this.posY;
      double dz = (double)pos.getZ() + 0.5 - (double)this.posZ;
      if (dx * dx + dy * dy + dz * dz <= (double)(this.length * this.length)
         && this.shouldDestroyAt(pos.getX(), pos.getY(), pos.getZ())
         && !this.world.getFluidState(pos).isEmpty()) {
         this.world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
      }
   }

   private boolean shouldDestroyAt(int x, int y, int z) {
      double dx = (double)x + 0.5 - (double)this.posX;
      double dy = (double)y + 0.5 - (double)this.posY;
      double dz = (double)z + 0.5 - (double)this.posZ;
      double normalizedDistance = Math.sqrt(dx * dx + dy * dy + dz * dz) / (double)this.length;
      if (normalizedDistance <= 0.65) {
         return true;
      }
      if (normalizedDistance >= 1.0) {
         return false;
      }

      double fade = (normalizedDistance - 0.65) / 0.35;
      double destroyChance = (1.0 - fade) * (1.0 - fade);
      long hash = (long)x * 73428767L ^ (long)y * 91227137L ^ (long)z * 43828913L;
      hash ^= hash >>> 33;
      hash *= -49064778989728563L;
      hash ^= hash >>> 33;
      double dither = (double)(hash & 65535L) / 65535.0;
      return dither < destroyChance;
   }

   private boolean shouldDisplaceAt(BlockPos pos, BlockState state) {
      if (state.liquid() || state.hasBlockEntity()) {
         return false;
      }
      double dx = (double)pos.getX() + 0.5 - (double)this.posX;
      double dy = (double)pos.getY() + 0.5 - (double)this.posY;
      double dz = (double)pos.getZ() + 0.5 - (double)this.posZ;
      double normalizedDistance = Math.sqrt(dx * dx + dy * dy + dz * dz) / (double)this.length;
      if (normalizedDistance <= 0.65 || normalizedDistance >= 1.0) {
         return false;
      }
      double edgeFactor = (normalizedDistance - 0.65) / 0.35;
      double chance = 0.05 + edgeFactor * 0.25;
      return this.positionDither(pos) < chance;
   }

   private void displaceBlock(BlockPos pos) {
      BlockState state = this.world.getBlockState(pos);
      if (state.isAir() || state.liquid() || state.hasBlockEntity()) {
         return;
      }
      FallingBlockEntity fallingBlock = FallingBlockEntity.fall(this.world, pos, state);
      Vec3 direction = new Vec3((double)pos.getX() + 0.5 - (double)this.posX, 0.0, (double)pos.getZ() + 0.5 - (double)this.posZ).normalize();
      fallingBlock.setDeltaMovement(direction.x * 0.45, 0.25, direction.z * 0.45);
      fallingBlock.dropItem = false;
   }

   private double positionDither(BlockPos pos) {
      long hash = (long)pos.getX() * 73428767L ^ (long)pos.getY() * 91227137L ^ (long)pos.getZ() * 43828913L;
      hash ^= hash >>> 33;
      hash *= -49064778989728563L;
      hash ^= hash >>> 33;
      return (double)(hash & 65535L) / 65535.0;
   }

   public class CoordComparator implements Comparator<ChunkCoordIntPair> {
      public int compare(ChunkCoordIntPair o1, ChunkCoordIntPair o2) {
         int chunkX = ExplosionNuke.this.posX >> 4;
         int chunkZ = ExplosionNuke.this.posZ >> 4;
         int diff1 = Math.abs(chunkX - o1.chunkXPos) + Math.abs(chunkZ - o1.chunkZPos);
         int diff2 = Math.abs(chunkX - o2.chunkXPos) + Math.abs(chunkZ - o2.chunkZPos);
         return diff1 - diff2;
      }
   }

   public class FloatTriplet {
      public float xCoord;
      public float yCoord;
      public float zCoord;

      public FloatTriplet(float x, float y, float z) {
         this.xCoord = x;
         this.yCoord = y;
         this.zCoord = z;
      }
   }
}
