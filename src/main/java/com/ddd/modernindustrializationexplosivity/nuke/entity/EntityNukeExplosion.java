package com.ddd.modernindustrializationexplosivity.nuke.entity;

import java.util.ArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.ddd.modernindustrializationexplosivity.ModernIndustrializationExplosivity;
import com.ddd.modernindustrializationexplosivity.nuke.NukeEntities;
import com.ddd.modernindustrializationexplosivity.nuke.explosion.EntityExplosionChunkloading;
import com.ddd.modernindustrializationexplosivity.nuke.explosion.ExplosionNuke;
import com.ddd.modernindustrializationexplosivity.nuke.compat.ChunkCoordIntPair;

public class EntityNukeExplosion extends EntityExplosionChunkloading {
   private static final int TARGET_RAY_PREPARATION_TICKS = 300;
   public int strength;
   public int speed;
   public int length;
   private final ArrayList<Integer> processed = new ArrayList<>();
   ExplosionNuke explosion;
   private boolean nukeDone = false;
   private boolean damageDone = false;
   private Entity cause;

   public EntityNukeExplosion(Level world) {
      super(NukeEntities.NUKE.get(), world);
   }

   public EntityNukeExplosion(EntityType<?> type, Level world) {
      super(type, world);
   }

   protected void defineSynchedData(Builder builder) {
   }

   public void tick() {
      super.tick();
      if (this.strength == 0) {
         this.clearChunkLoader();
         this.discard();
      } else {
         if (!this.level().isClientSide) {
            int cx = (int)Math.floor(this.getX() / 16.0);
            int cz = (int)Math.floor(this.getZ() / 16.0);
            this.loadChunk(cx, cz);
         }

         if (this.explosion == null) {
            this.explosion = new ExplosionNuke(this.level(), (int)this.getX(), (int)this.getY(), (int)this.getZ(), this.strength, this.speed, this.length);
         }

         if (!this.nukeDone) {
            if (!this.explosion.isAusf3Complete) {
               this.explosion.collectTip(this.speed * 10);
            } else if (!this.explosion.perChunk.isEmpty() || this.explosion.hasFluidCleanupRemaining() || this.explosion.hasScorchRemaining()) {
               long start = System.currentTimeMillis();

               while (System.currentTimeMillis() < start + 30L) {
                  if (!this.explosion.perChunk.isEmpty()) {
                     ChunkCoordIntPair chunk = this.explosion.orderedChunks.get(0);
                     this.loadChunk(chunk.chunkXPos, chunk.chunkZPos);
                     this.explosion.processChunk();
                  } else if (this.explosion.hasFluidCleanupRemaining()) {
                     ChunkCoordIntPair chunk = this.explosion.getNextFluidCleanupChunk();
                     this.loadChunk(chunk.chunkXPos, chunk.chunkZPos);
                     this.explosion.processFluidCleanupChunk();
                  } else if (this.explosion.hasScorchRemaining()) {
                     ChunkCoordIntPair chunk = this.explosion.getNextScorchChunk();
                     this.loadChunk(chunk.chunkXPos, chunk.chunkZPos);
                     this.explosion.processScorchChunk();
                     break;
                  } else {
                     break;
                  }
               }
            } else {
               this.nukeDone = true;
            }
         }

         if (!this.damageDone) {
            this.processEnts(this.level(), 200.0, 400.0F);
            if (((double)this.tickCount * 1.5 + 1.0) * 1.5 > 200.0) {
               this.damageDone = true;
            }
         }

         if (this.nukeDone && this.damageDone) {
            this.clearChunkLoader();
            this.discard();
         }
      }
   }

   protected void readAdditionalSaveData(CompoundTag compoundTag) {
   }

   protected void addAdditionalSaveData(CompoundTag compoundTag) {
   }

   private void processEnts(Level world, double radius, float maxDamage) {
      double x = this.getX();
      double y = this.getY();
      double z = this.getZ();
      AABB aabb = new AABB(x, y, z, x, y, z).inflate(radius);

      for (Entity entity : world.getEntities(null, aabb)) {
         if (!this.processed.contains(entity.getId())) {
            double dist = entity.position().distanceTo(new Vec3(x, y, z));
            if (dist < radius) {
               entity.setRemainingFireTicks(5);
               if (dist < ((double)this.tickCount * 1.5 + 1.0) * 1.5) {
                  double damage = (double)getDamage(dist, radius, (double)maxDamage);
                  DamageSource damageSource = world.damageSources().source(ModernIndustrializationExplosivity.NUCLEAR_BLAST, this.cause);
                  entity.hurt(damageSource, (float)damage);
                  Vec3 pushDir = entity.position().subtract(this.position()).normalize();
                  pushDir = pushDir.normalize().scale(3.0);
                  pushDir = new Vec3(pushDir.x, 1.0, pushDir.z);
                  entity.push(pushDir);
                  this.processed.add(entity.getId());
               }
            }
         }
      }
   }

   public static float getDamage(double distance, double radius, double maxDamage) {
      return Math.max((float)(maxDamage * (radius - distance) / radius), 0.0F);
   }

   public static EntityNukeExplosion statFac(Level world, int r, double x, double y, double z, Entity cause) {
      if (r == 0) {
         r = 25;
      }

      r *= 2;
      EntityNukeExplosion mk5 = new EntityNukeExplosion(world);
      mk5.strength = r;
      mk5.cause = cause;
      int baseRayBudget = (int)Math.ceil(100000.0 / (double)r);
      int rayCount = (int)((Math.PI * 5.0 / 2.0) * Math.pow((double)r, 2.0));
      int adaptiveRayBudget = (int)Math.ceil((double)rayCount / (double)(TARGET_RAY_PREPARATION_TICKS * 10));
      mk5.speed = Math.max(baseRayBudget, adaptiveRayBudget);
      mk5.setPos(x, y, z);
      mk5.length = r / 2;
      return mk5;
   }
}
