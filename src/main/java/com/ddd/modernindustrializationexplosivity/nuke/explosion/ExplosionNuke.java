package com.ddd.modernindustrializationexplosivity.nuke.explosion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.phys.Vec3;
import com.ddd.modernindustrializationexplosivity.ExplosivityConfig;
import com.ddd.modernindustrializationexplosivity.nuke.compat.ChunkCoordIntPair;

public class ExplosionNuke {
   private static final double VERTICAL_BLAST_MULTIPLIER = 2.5;
   public static final double SCORCH_RADIUS_MULTIPLIER = 2.0;
   public static final double EXTENDED_EFFECT_RADIUS_MULTIPLIER = SCORCH_RADIUS_MULTIPLIER * 0.75;
   private static final int FLUID_CLEANUP_PASSES = 2;
   private static final int SCORCH_DEPTH_BELOW_ORIGIN = 32;
   private static final int SCORCH_HEIGHT_ABOVE_ORIGIN = 64;
   public HashMap<ChunkCoordIntPair, List<ExplosionNuke.FloatTriplet>> perChunk = new HashMap<>();
   public List<ChunkCoordIntPair> orderedChunks = new ArrayList<>();
   private final List<ChunkCoordIntPair> fluidCleanupChunks = new ArrayList<>();
   private final List<ChunkCoordIntPair> scorchBandChunks = new ArrayList<>();
   private int nextFluidCleanupChunk;
   private int fluidCleanupPass;
   private int nextScorchChunk;
   private int scorchBandInnerRadius;
   private int scorchBandOuterRadius;
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
      this.gspNumMax = getRayCountForStrength(this.strength);
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

   private double distanceToChunkSqr(ChunkCoordIntPair chunk) {
      int minX = chunk.chunkXPos << 4;
      int minZ = chunk.chunkZPos << 4;
      double nearestX = Math.clamp((double)this.posX, (double)minX, (double)(minX + 16));
      double nearestZ = Math.clamp((double)this.posZ, (double)minZ, (double)(minZ + 16));
      double dx = nearestX - (double)this.posX;
      double dz = nearestZ - (double)this.posZ;
      return dx * dx + dz * dz;
   }

   public static double getExtendedEffectRadius(int destructionRadius) {
      return (double)destructionRadius * EXTENDED_EFFECT_RADIUS_MULTIPLIER;
   }

   public static int getRayCountForStrength(int strength) {
      long uncappedRayCount = (long)Math.ceil(Math.PI * 5.0 / 2.0 * (double)strength * (double)strength);
      return (int)Math.min(uncappedRayCount, (long)ExplosivityConfig.MAX_NUKE_RAY_COUNT.get());
   }

   private void prepareNextScorchBand() {
      this.scorchBandChunks.clear();
      this.nextScorchChunk = 0;
      int radius = (int)Math.ceil((double)this.length * SCORCH_RADIUS_MULTIPLIER);
      this.scorchBandOuterRadius = Math.min(this.scorchBandInnerRadius + 16, radius);
      int minChunkX = Math.floorDiv(this.posX - this.scorchBandOuterRadius, 16);
      int maxChunkX = Math.floorDiv(this.posX + this.scorchBandOuterRadius, 16);
      int minChunkZ = Math.floorDiv(this.posZ - this.scorchBandOuterRadius, 16);
      int maxChunkZ = Math.floorDiv(this.posZ + this.scorchBandOuterRadius, 16);
      double innerRadiusSquared = (double)(this.scorchBandInnerRadius * this.scorchBandInnerRadius);
      double outerRadiusSquared = (double)(this.scorchBandOuterRadius * this.scorchBandOuterRadius);

      for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
         for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            ChunkCoordIntPair chunk = new ChunkCoordIntPair(chunkX, chunkZ);
            if (this.distanceToChunkSqr(chunk) <= outerRadiusSquared && this.farthestDistanceToChunkSqr(chunk) > innerRadiusSquared) {
               this.scorchBandChunks.add(chunk);
            }
         }
      }
      this.scorchBandChunks.sort(Comparator.comparingDouble(this::distanceToChunkSqr));
   }

   private double farthestDistanceToChunkSqr(ChunkCoordIntPair chunk) {
      int minX = chunk.chunkXPos << 4;
      int minZ = chunk.chunkZPos << 4;
      double farthestX = Math.abs((double)this.posX - (double)minX) > Math.abs((double)this.posX - (double)(minX + 16)) ? minX : minX + 16;
      double farthestZ = Math.abs((double)this.posZ - (double)minZ) > Math.abs((double)this.posZ - (double)(minZ + 16)) ? minZ : minZ + 16;
      double dx = farthestX - (double)this.posX;
      double dz = farthestZ - (double)this.posZ;
      return dx * dx + dz * dz;
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
      return new Vec3(dx, dy * VERTICAL_BLAST_MULTIPLIER, dz);
   }

   public void collectTip(int count) {
      int amountProcessed = 0;

      while (this.gspNumMax >= this.gspNum) {
         Vec3 vec = this.getSpherical2cartesian();
         int length = (int)Math.ceil((double)this.strength);
         float res = (float)this.strength;
         ExplosionNuke.FloatTriplet tip = null;

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
               tip = new ExplosionNuke.FloatTriplet(x0, y0, z0, 0, 0, true);
            }

            if (res <= 0.0F || i + 1 >= this.length || i == length - 1) {
               break;
            }
         }

         if (tip != null) {
            this.storeNormalizedRaySegments(tip);
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

   /** Splits the original normalized tip-to-origin traversal into local chunk segments once. */
   private void storeNormalizedRaySegments(ExplosionNuke.FloatTriplet tip) {
      Vec3 ray = new Vec3((double)(tip.xCoord - (float)this.posX), (double)(tip.yCoord - (float)this.posY), (double)(tip.zCoord - (float)this.posZ));
      double rayLength = ray.length();
      if (rayLength == 0.0) {
         return;
      }

      float directionX = (float)(ray.x / rayLength);
      float directionY = (float)(ray.y / rayLength);
      float directionZ = (float)(ray.z / rayLength);
      List<ExplosionNuke.FloatTriplet> segments = new ArrayList<>();
      int currentChunkX = Integer.MIN_VALUE;
      int currentChunkZ = Integer.MIN_VALUE;
      int segmentStart = 0;
      boolean segmentContainsSolid = false;

      for (int i = 0; (double)i < rayLength; i++) {
         int x = (int)Math.floor((double)this.posX + (double)directionX * (double)i);
         int y = (int)Math.floor((double)this.posY + (double)directionY * (double)i);
         int z = (int)Math.floor((double)this.posZ + (double)directionZ * (double)i);
         int chunkX = x >> 4;
         int chunkZ = z >> 4;
         if (currentChunkX == Integer.MIN_VALUE) {
            currentChunkX = chunkX;
            currentChunkZ = chunkZ;
            segmentStart = i;
         } else if (currentChunkX != chunkX || currentChunkZ != chunkZ) {
            segments.add(new ExplosionNuke.FloatTriplet(directionX, directionY, directionZ, segmentStart, i - 1, segmentContainsSolid));
            currentChunkX = chunkX;
            currentChunkZ = chunkZ;
            segmentStart = i;
            segmentContainsSolid = false;
         }

         if (!this.world.isEmptyBlock(new BlockPos(x, y, z))) {
            segmentContainsSolid = true;
         }
      }
      if (currentChunkX != Integer.MIN_VALUE) {
         segments.add(new ExplosionNuke.FloatTriplet(directionX, directionY, directionZ, segmentStart, (int)Math.ceil(rayLength) - 1, segmentContainsSolid));
      }

      for (ExplosionNuke.FloatTriplet segment : segments) {
         if (!segment.containsSolid) {
            continue;
         }
         int x = (int)Math.floor((double)this.posX + (double)segment.xCoord * (double)segment.startIndex);
         int z = (int)Math.floor((double)this.posZ + (double)segment.zCoord * (double)segment.startIndex);
         ChunkCoordIntPair chunk = new ChunkCoordIntPair(x >> 4, z >> 4);
         this.perChunk.computeIfAbsent(chunk, ignored -> new ArrayList<>()).add(segment);
      }
   }

   public static float masqueradeResistance(Block block) {
      if (block == Blocks.BEDROCK) {
         return Float.MAX_VALUE;
      } else if (block == Blocks.SANDSTONE) {
         return Blocks.STONE.getExplosionResistance();
      } else {
         return block == Blocks.OBSIDIAN ? Blocks.STONE.getExplosionResistance() * 3.0F : block.getExplosionResistance();
      }
   }

   public void processChunk() {
      if (!this.perChunk.isEmpty()) {
         ChunkCoordIntPair coord = this.orderedChunks.get(0);
         List<ExplosionNuke.FloatTriplet> list = this.perChunk.get(coord);
         HashSet<Long> toRem = new HashSet<>();
         HashSet<Long> toDisplace = new HashSet<>();
         BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

         for (ExplosionNuke.FloatTriplet segment : list) {
            for (int i = segment.startIndex; i <= segment.endIndex; i++) {
               int x0 = (int)Math.floor((double)this.posX + (double)segment.xCoord * (double)i);
               int y0 = (int)Math.floor((double)this.posY + (double)segment.yCoord * (double)i);
               int z0 = (int)Math.floor((double)this.posZ + (double)segment.zCoord * (double)i);
               BlockState state = this.world.getBlockState(mutablePos.set(x0, y0, z0));
               if (!state.isAir() && !state.is(Blocks.BEDROCK)) {
                  if (!this.shouldDestroyAt(x0, y0, z0)) {
                     if (toDisplace.size() < 64 && this.shouldDisplaceAt(x0, y0, z0, state)) {
                        toDisplace.add(BlockPos.asLong(x0, y0, z0));
                     }
                     continue;
                  }
                  toRem.add(BlockPos.asLong(x0, y0, z0));
               }
            }
         }

         for (long packedPos : toRem) {
            BlockPos pos = BlockPos.of(packedPos);
            this.world.setBlock(pos, Blocks.AIR.defaultBlockState(), this.destructionUpdateFlags());
         }

         for (long packedPos : toDisplace) {
            if (!toRem.contains(packedPos)) {
               this.displaceBlock(BlockPos.of(packedPos));
            }
         }

         this.perChunk.remove(coord);
         this.orderedChunks.remove(0);
      }
   }

   protected void handleTip(int x, int y, int z) {
      this.world.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), this.destructionUpdateFlags());
   }

   public boolean hasFluidCleanupRemaining() {
      return !this.fluidCleanupChunks.isEmpty() && this.fluidCleanupPass < FLUID_CLEANUP_PASSES;
   }

   public ChunkCoordIntPair getNextFluidCleanupChunk() {
      return this.hasFluidCleanupRemaining() ? this.fluidCleanupChunks.get(this.nextFluidCleanupChunk) : null;
   }

   public void processFluidCleanupChunk() {
      if (this.hasFluidCleanupRemaining()) {
         ChunkCoordIntPair chunk = this.fluidCleanupChunks.get(this.nextFluidCleanupChunk++);
         this.clearChunkFluids(chunk.chunkXPos, chunk.chunkZPos);
         if (this.nextFluidCleanupChunk >= this.fluidCleanupChunks.size()) {
            this.fluidCleanupPass++;
            this.nextFluidCleanupChunk = 0;
         }
      }
   }

   public boolean hasScorchRemaining() {
      int radius = (int)Math.ceil((double)this.length * SCORCH_RADIUS_MULTIPLIER);
      return this.nextScorchChunk < this.scorchBandChunks.size() || this.scorchBandInnerRadius < radius;
   }

   public ChunkCoordIntPair getNextScorchChunk() {
      if (!this.hasScorchRemaining()) {
         return null;
      }
      if (this.nextScorchChunk >= this.scorchBandChunks.size()) {
         this.prepareNextScorchBand();
      }
      return this.scorchBandChunks.get(this.nextScorchChunk);
   }

   /** Processes only the exposed surface of a chunk, keeping the extended vegetation cleanup inexpensive. */
   public void processScorchChunk() {
      if (!this.hasScorchRemaining()) {
         return;
      }

      ChunkCoordIntPair chunk = this.scorchBandChunks.get(this.nextScorchChunk++);
      int radius = (int)Math.ceil((double)this.length * SCORCH_RADIUS_MULTIPLIER);
      double innerRadiusSquared = (double)(this.scorchBandInnerRadius * this.scorchBandInnerRadius);
      double outerRadiusSquared = (double)(this.scorchBandOuterRadius * this.scorchBandOuterRadius);
      int minX = chunk.chunkXPos << 4;
      int minZ = chunk.chunkZPos << 4;
      int minY = Math.max(this.world.getMinBuildHeight(), this.posY - SCORCH_DEPTH_BELOW_ORIGIN);
      int maxY = Math.min(this.world.getMaxBuildHeight() - 1, this.posY + SCORCH_HEIGHT_ABOVE_ORIGIN);
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
      for (int x = minX; x < minX + 16; x++) {
         for (int z = minZ; z < minZ + 16; z++) {
            double dx = (double)x + 0.5 - (double)this.posX;
            double dz = (double)z + 0.5 - (double)this.posZ;
            double distanceSquared = dx * dx + dz * dz;
            if (distanceSquared <= innerRadiusSquared || distanceSquared > outerRadiusSquared || distanceSquared > (double)(radius * radius)) {
               continue;
            }
            if (!this.shouldScorchColumn(x, z, radius)) {
               continue;
            }

            for (int y = minY; y <= maxY; y++) {
               pos.set(x, y, z);
               BlockState state = this.world.getBlockState(pos);
               if (this.isScorchableVegetation(state)) {
                  this.world.setBlock(pos, Blocks.AIR.defaultBlockState(), this.destructionUpdateFlags());
               }
            }
         }
      }
      this.igniteScorchedChunk(chunk.chunkXPos, chunk.chunkZPos, innerRadiusSquared, outerRadiusSquared);
      if (this.nextScorchChunk >= this.scorchBandChunks.size()) {
         this.scorchBandInnerRadius = this.scorchBandOuterRadius;
      }
   }

   private boolean isScorchableVegetation(BlockState state) {
      return state.is(BlockTags.LEAVES)
         || state.is(BlockTags.ICE)
         || state.is(BlockTags.FLOWERS)
         || state.is(BlockTags.CROPS)
         || state.is(BlockTags.SAPLINGS)
         || state.is(Blocks.GRASS_BLOCK)
         || state.is(Blocks.SHORT_GRASS)
         || state.is(Blocks.TALL_GRASS)
         || state.is(Blocks.FERN)
         || state.is(Blocks.LARGE_FERN)
         || state.is(Blocks.DEAD_BUSH)
         || state.is(Blocks.VINE)
         || state.is(Blocks.GLOW_LICHEN)
         || state.is(Blocks.BROWN_MUSHROOM)
         || state.is(Blocks.RED_MUSHROOM)
         || state.is(Blocks.BROWN_MUSHROOM_BLOCK)
         || state.is(Blocks.RED_MUSHROOM_BLOCK)
         || state.is(Blocks.MUSHROOM_STEM)
         || state.is(Blocks.SNOW)
         || state.is(Blocks.SNOW_BLOCK);
   }

   private void clearChunkFluids(int chunkX, int chunkZ) {
      int verticalRadius = (int)Math.ceil((double)this.length * VERTICAL_BLAST_MULTIPLIER);
      int minY = Math.max(this.world.getMinBuildHeight(), this.posY - verticalRadius);
      int maxY = Math.min(this.world.getMaxBuildHeight() - 1, this.posY + verticalRadius);
      int minX = chunkX << 4;
      int minZ = chunkZ << 4;
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
      LevelChunk chunk = this.world.getChunk(chunkX, chunkZ);

      for (int sectionIndex = 0; sectionIndex < chunk.getSections().length; sectionIndex++) {
         LevelChunkSection section = chunk.getSections()[sectionIndex];
         if (!section.maybeHas(state -> !state.getFluidState().isEmpty())) {
            continue;
         }

         int sectionMinY = SectionPos.sectionToBlockCoord(this.world.getSectionYFromSectionIndex(sectionIndex));
         int scanMinY = Math.max(minY, sectionMinY);
         int scanMaxY = Math.min(maxY, sectionMinY + 15);
         if (scanMinY > scanMaxY) {
            continue;
         }

         for (int y = scanMinY; y <= scanMaxY; y++) {
            int localY = y - sectionMinY;
            for (int x = minX; x < minX + 16; x++) {
               int localX = x & 15;
               for (int z = minZ; z < minZ + 16; z++) {
                  double dx = (double)x + 0.5 - (double)this.posX;
                  double dy = ((double)y + 0.5 - (double)this.posY) / VERTICAL_BLAST_MULTIPLIER;
                  double dz = (double)z + 0.5 - (double)this.posZ;
                  if (dx * dx + dy * dy + dz * dz <= (double)(this.length * this.length)
                     && !section.getFluidState(localX, localY, z & 15).isEmpty()) {
                     this.world.setBlock(pos.set(x, y, z), Blocks.AIR.defaultBlockState(), this.destructionUpdateFlags());
                  }
               }
            }
         }
      }
   }

   /** Uses a deterministic dither so the expanded scorch radius tapers rather than ending abruptly. */
   private boolean shouldScorchColumn(int x, int z, int radius) {
      double dx = (double)x + 0.5 - (double)this.posX;
      double dz = (double)z + 0.5 - (double)this.posZ;
      double normalizedDistance = Math.sqrt(dx * dx + dz * dz) / (double)radius;
      if (normalizedDistance <= 0.65) {
         return true;
      }
      if (normalizedDistance >= 1.0) {
         return false;
      }

      double fade = (normalizedDistance - 0.65) / 0.35;
      double scorchChance = (1.0 - fade) * (1.0 - fade);
      return this.positionDither(x, 0, z) < scorchChance;
   }

   private boolean shouldDestroyAt(int x, int y, int z) {
      double dx = (double)x + 0.5 - (double)this.posX;
      double dy = ((double)y + 0.5 - (double)this.posY) / VERTICAL_BLAST_MULTIPLIER;
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

   private boolean shouldDisplaceAt(int x, int y, int z, BlockState state) {
      if (state.is(Blocks.BEDROCK) || state.liquid() || state.hasBlockEntity()) {
         return false;
      }
      double dx = (double)x + 0.5 - (double)this.posX;
      double dy = ((double)y + 0.5 - (double)this.posY) / VERTICAL_BLAST_MULTIPLIER;
      double dz = (double)z + 0.5 - (double)this.posZ;
      double normalizedDistance = Math.sqrt(dx * dx + dy * dy + dz * dz) / (double)this.length;
      if (normalizedDistance <= 0.65 || normalizedDistance >= 1.0) {
         return false;
      }
      double edgeFactor = (normalizedDistance - 0.65) / 0.35;
      double chance = 0.05 + edgeFactor * 0.25;
      return this.positionDither(x, y, z) < chance;
   }

   private void displaceBlock(BlockPos pos) {
      BlockState state = this.world.getBlockState(pos);
      if (state.is(Blocks.BEDROCK) || state.isAir() || state.liquid() || state.hasBlockEntity()) {
         return;
      }
      FallingBlockEntity fallingBlock = FallingBlockEntity.fall(this.world, pos, state);
      Vec3 direction = new Vec3((double)pos.getX() + 0.5 - (double)this.posX, 0.0, (double)pos.getZ() + 0.5 - (double)this.posZ).normalize();
      fallingBlock.setDeltaMovement(direction.x * 0.45, 0.25, direction.z * 0.45);
      fallingBlock.dropItem = false;
   }

   private double positionDither(BlockPos pos) {
      return this.positionDither(pos.getX(), pos.getY(), pos.getZ());
   }

   private double positionDither(int x, int y, int z) {
      long hash = (long)x * 73428767L ^ (long)y * 91227137L ^ (long)z * 43828913L;
      hash ^= hash >>> 33;
      hash *= -49064778989728563L;
      hash ^= hash >>> 33;
      return (double)(hash & 65535L) / 65535.0;
   }

   private int destructionUpdateFlags() {
      return ExplosivityConfig.UPDATE_NEIGHBORS_DURING_DESTRUCTION.get() ? Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS : Block.UPDATE_CLIENTS;
   }

   private void igniteScorchedChunk(int chunkX, int chunkZ, double innerRadiusSquared, double outerRadiusSquared) {
      int minX = chunkX << 4;
      int minZ = chunkZ << 4;
      double fireRadius = getExtendedEffectRadius(this.length);
      for (int x = minX; x < minX + 16; x++) {
         for (int z = minZ; z < minZ + 16; z++) {
            double dx = (double)x + 0.5 - (double)this.posX;
            double dz = (double)z + 0.5 - (double)this.posZ;
            double distanceSquared = dx * dx + dz * dz;
            if (distanceSquared <= innerRadiusSquared || distanceSquared > outerRadiusSquared) continue;
            double normalizedDistance = Math.sqrt(dx * dx + dz * dz) / fireRadius;
            if (normalizedDistance >= 1.0) continue;

            double radialBand = 0.4 + 0.6 * Math.pow(Math.cos(normalizedDistance * Math.PI * 4.0), 2.0);
            double ignitionChance = 0.50 * Math.pow(1.0 - normalizedDistance, 1.35) * radialBand;
            if (this.positionDither(x, 0, z) >= ignitionChance) continue;

            BlockPos surface = new BlockPos(x, this.world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
            BlockPos firePos = surface.above();
            BlockState fire = Blocks.FIRE.defaultBlockState();
            if (this.world.isEmptyBlock(firePos) && fire.canSurvive(this.world, firePos)) {
               this.world.setBlock(firePos, fire, 3);
            }
         }
      }
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
      public int startIndex;
      public int endIndex;
      public boolean containsSolid;

      public FloatTriplet(float x, float y, float z, int startIndex, int endIndex, boolean containsSolid) {
         this.xCoord = x;
         this.yCoord = y;
         this.zCoord = z;
         this.startIndex = startIndex;
         this.endIndex = endIndex;
         this.containsSolid = containsSolid;
      }
   }
}
