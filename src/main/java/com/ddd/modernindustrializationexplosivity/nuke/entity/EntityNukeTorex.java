package com.ddd.modernindustrializationexplosivity.nuke.entity;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import com.ddd.modernindustrializationexplosivity.nuke.NukeEntities;
import com.ddd.modernindustrializationexplosivity.nuke.Util;
import com.ddd.modernindustrializationexplosivity.nuke.compat.Vec3Old;

public class EntityNukeTorex extends Entity {
   public static final float MAX_SHOCK_RING_DISTANCE = 150.0F * 1.5F * 1.5F;
   private static final Map<UUID, EntityNukeTorex> CLIENT_CLOUDS = new ConcurrentHashMap<>();
   public double coreHeight = 3.0;
   public double convectionHeight = 3.0;
   public double torusWidth = 3.0;
   public double rollerSize = 1.0;
   public double heat = 1.0;
   public double lastSpawnY = -1.0;
   public ArrayList<EntityNukeTorex.Cloudlet> cloudlets = new ArrayList<>();
   private int age = 0;
   public boolean didPlaySound = false;
   public boolean didShake = false;
   public boolean didIrradiate = false;
   public static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.FLOAT);
   public static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> RENDER_RADIUS = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> RADIATION_RADIUS = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> RADIATION_DURATION = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Long> START_TIME = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.LONG);

   public EntityNukeTorex(Level world) {
      super(NukeEntities.TOREX.get(), world);
      this.noCulling = true;
   }

   public EntityNukeTorex(EntityType<?> type, Level world) {
      super(type, world);
   }

   protected void defineSynchedData(Builder builder) {
      builder.define(SCALE, 1.0F);
      builder.define(TYPE, 0);
      builder.define(RENDER_RADIUS, 64.0F);
      builder.define(RADIATION_RADIUS, 200.0F);
      builder.define(RADIATION_DURATION, 48000);
      builder.define(START_TIME, -1L);
   }

   public boolean shouldRenderAtSqrDistance(double distance) {
      return distance <= (double)(this.getRenderRadius() * this.getRenderRadius());
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide && this.entityData.get(START_TIME) < 0L) {
         this.entityData.set(START_TIME, this.level().getGameTime());
      }

      long startTime = this.entityData.get(START_TIME);
      if (this.level().isClientSide && startTime < 0L) {
         return;
      }
      this.age = startTime >= 0L ? Math.max(0, (int)(this.level().getGameTime() - startTime)) : this.tickCount;
      double s = 1.5;
      double cs = 1.5;
      int maxAge = this.getMaxAge();
      if (this.level().isClientSide) {
         if (this.age == 1) {
            this.setScale((float)s);
         }

         if (this.lastSpawnY == -1.0) {
            this.lastSpawnY = this.getY() - 3.0;
         }

         int spawnTarget = Math.max(
            this.level().getChunk((int)this.getX() >> 4, (int)this.getZ() >> 4).getHeight(Types.WORLD_SURFACE, (int)this.getX() & 15, (int)this.getZ() & 15)
               - 3,
            1
         );
         double moveSpeed = 0.5;
         if (Math.abs((double)spawnTarget - this.lastSpawnY) < moveSpeed) {
            this.lastSpawnY = (double)spawnTarget;
         } else {
            this.lastSpawnY = this.lastSpawnY + moveSpeed * Math.signum((double)spawnTarget - this.lastSpawnY);
         }

         double range = (this.torusWidth - this.rollerSize) * 0.25;
         double simSpeed = this.getSimulationSpeed();
         int toSpawn = (int)Math.ceil(10.0 * simSpeed * simSpeed);
         int lifetime = Math.min(this.age * this.age + 200, maxAge - this.age + 200);

         for (int i = 0; i < toSpawn; i++) {
            double x = this.getX() + this.random.nextGaussian() * range;
            double z = this.getZ() + this.random.nextGaussian() * range;
            EntityNukeTorex.Cloudlet cloud = new EntityNukeTorex.Cloudlet(x, this.lastSpawnY, z, (float)(this.random.nextDouble() * 2.0 * Math.PI), 0, lifetime);
            cloud.setScale(1.0F + (float)this.age * 0.005F * (float)cs, 5.0F * (float)cs);
            this.cloudlets.add(cloud);
         }

         if (this.age < 150) {
            int cloudCount = this.age * 5;
            int shockLife = Math.max(600 - this.age * 8, 240);

            for (int i = 0; i < cloudCount; i++) {
               Vec3 vec = new Vec3(((double)this.age * 1.5 + this.random.nextDouble()) * 1.5, 0.0, 0.0);
               float rot = (float)((Math.PI * 2) * this.random.nextDouble());
               vec = vec.yRot(rot);
               BlockPos pos = new BlockPos((int)(this.getX() + vec.x), (int)this.getY(), (int)(this.getZ() + vec.z));
               int height = this.level().getChunk(pos).getHeight(Types.WORLD_SURFACE, pos.getX() & 15, pos.getZ() & 15);
               EntityNukeTorex.Cloudlet cloud = new EntityNukeTorex.Cloudlet(
                  vec.x + this.getX(), (double)height, vec.z + this.getZ(), rot, 0, shockLife, EntityNukeTorex.TorexType.SHOCK
               );
               cloud.setScale(7.0F, 2.0F).setMotion(this.age > 15 ? 0.75 : 0.0);
               this.cloudlets.add(cloud);
            }
         }

         if ((double)this.age < 130.0 * s) {
            lifetime = (int)((double)lifetime * s);

            for (int i = 0; i < 2; i++) {
               EntityNukeTorex.Cloudlet cloud = new EntityNukeTorex.Cloudlet(
                  this.getX(),
                  this.getY() + this.coreHeight,
                  this.getZ(),
                  (float)(this.random.nextDouble() * 2.0 * Math.PI),
                  0,
                  lifetime,
                  EntityNukeTorex.TorexType.RING
               );
               cloud.setScale(1.0F + (float)this.age * 0.0025F * (float)(cs * cs), 3.0F * (float)(cs * cs));
               this.cloudlets.add(cloud);
            }
         }

         if ((double)this.age > 130.0 * s && (double)this.age < 600.0 * s) {
            for (int i = 0; i < 20; i++) {
               for (int j = 0; j < 4; j++) {
                  float angle = (float)((Math.PI * 2) * this.random.nextDouble());
                  Vec3 vec = new Vec3(this.torusWidth + this.rollerSize * (5.0 + this.random.nextDouble()), 0.0, 0.0)
                     .zRot((float)(0.06981317007977318 * (double)j))
                     .yRot(angle);
                  EntityNukeTorex.Cloudlet cloud = new EntityNukeTorex.Cloudlet(
                     this.getX() + vec.x,
                     this.getY() + this.coreHeight - 5.0 + (double)j * s,
                     this.getZ() + vec.z,
                     angle,
                     0,
                     (int)((double)(20 + this.age / 10) * (1.0 + this.random.nextDouble() * 0.1)),
                     EntityNukeTorex.TorexType.CONDENSATION
                  );
                  cloud.setScale(0.125F * (float)cs, 3.0F * (float)cs);
                  this.cloudlets.add(cloud);
               }
            }
         }

         if ((double)this.age > 200.0 * s && (double)this.age < 600.0 * s) {
            for (int i = 0; i < 20; i++) {
               for (int j = 0; j < 4; j++) {
                  float angle = (float)((Math.PI * 2) * this.random.nextDouble());
                  Vec3 vec = new Vec3(this.torusWidth + this.rollerSize * (3.0 + this.random.nextDouble() * 0.5), 0.0, 0.0)
                     .zRot((float)(0.06981317007977318 * (double)j))
                     .yRot(angle);
                  EntityNukeTorex.Cloudlet cloud = new EntityNukeTorex.Cloudlet(
                     this.getX() + vec.x,
                     this.getY() + this.coreHeight + 25.0 + (double)j * cs,
                     this.getZ() + vec.z,
                     angle,
                     0,
                     (int)((double)(20 + this.age / 10) * (1.0 + this.random.nextDouble() * 0.1)),
                     EntityNukeTorex.TorexType.CONDENSATION
                  );
                  cloud.setScale(0.125F * (float)cs, 3.0F * (float)cs);
                  this.cloudlets.add(cloud);
               }
            }
         }

         this.cloudlets.removeIf(cloudx -> {
            cloudx.update();
            return cloudx.isDead;
         });
         this.coreHeight += 0.15 / s;
         this.torusWidth += 0.05 / s;
         this.rollerSize = this.torusWidth * 0.35;
         this.convectionHeight = this.coreHeight + this.rollerSize;
         int maxHeat = (int)(50.0 * cs);
         this.heat = (double)maxHeat - Math.pow((double)(maxHeat * this.age / maxAge), 1.0);
      }

      if (!this.level().isClientSide && this.age > maxAge) {
         this.discard();
      }
   }

   protected void readAdditionalSaveData(CompoundTag compoundTag) {
      if (compoundTag.contains("StartTime")) {
         this.entityData.set(START_TIME, compoundTag.getLong("StartTime"));
      }
      if (compoundTag.contains("Scale")) {
         this.entityData.set(SCALE, compoundTag.getFloat("Scale"));
      }
      if (compoundTag.contains("CloudType")) {
         this.entityData.set(TYPE, compoundTag.getInt("CloudType"));
      }
      if (compoundTag.contains("RenderRadius")) {
         this.entityData.set(RENDER_RADIUS, compoundTag.getFloat("RenderRadius"));
      }
      if (compoundTag.contains("RadiationRadius")) {
         this.entityData.set(RADIATION_RADIUS, compoundTag.getFloat("RadiationRadius"));
      }
      if (compoundTag.contains("RadiationDuration")) {
         this.entityData.set(RADIATION_DURATION, compoundTag.getInt("RadiationDuration"));
      }
   }

   protected void addAdditionalSaveData(CompoundTag compoundTag) {
      compoundTag.putLong("StartTime", this.entityData.get(START_TIME));
      compoundTag.putFloat("Scale", this.entityData.get(SCALE));
      compoundTag.putInt("CloudType", this.entityData.get(TYPE));
      compoundTag.putFloat("RenderRadius", this.entityData.get(RENDER_RADIUS));
      compoundTag.putFloat("RadiationRadius", this.entityData.get(RADIATION_RADIUS));
      compoundTag.putInt("RadiationDuration", this.entityData.get(RADIATION_DURATION));
   }

   @Override
   public void remove(RemovalReason reason) {
      super.remove(reason);
   }

   @Override
   public void onClientRemoval() {
      if (this.level().isClientSide && this.getAge() < this.getMaxAge() && !this.cloudlets.isEmpty()) {
         CLIENT_CLOUDS.putIfAbsent(this.getUUID(), this);
      }
      super.onClientRemoval();
   }

   public static EntityNukeTorex getClientSimulation(EntityNukeTorex trackedEntity) {
      return CLIENT_CLOUDS.getOrDefault(trackedEntity.getUUID(), trackedEntity);
   }

   public static void tickDetachedClientClouds() {
      CLIENT_CLOUDS.entrySet().removeIf(entry -> {
         EntityNukeTorex cloud = entry.getValue();
         if (cloud.getAge() >= cloud.getMaxAge() || cloud.level().isClientSide == false) {
            return true;
         }
         cloud.tick();
         return false;
      });
   }

   public EntityNukeTorex setScale(float scale) {
      if (!this.level().isClientSide) {
         this.entityData.set(SCALE, scale);
      }

      this.coreHeight = this.coreHeight / 1.5 * (double)scale;
      this.convectionHeight = this.convectionHeight / 1.5 * (double)scale;
      this.torusWidth = this.torusWidth / 1.5 * (double)scale;
      this.rollerSize = this.rollerSize / 1.5 * (double)scale;
      return this;
   }

   public EntityNukeTorex setType(int type) {
      this.entityData.set(TYPE, type);
      return this;
   }

   public EntityNukeTorex setRenderRadius(float radius) {
      this.entityData.set(RENDER_RADIUS, radius);
      return this;
   }

   public float getRenderRadius() {
      return this.entityData.get(RENDER_RADIUS);
   }

   public EntityNukeTorex setRadiationRadius(float radius) {
      this.entityData.set(RADIATION_RADIUS, radius);
      return this;
   }

   public float getRadiationRadius() {
      return this.entityData.get(RADIATION_RADIUS);
   }

   public EntityNukeTorex setRadiationDuration(int duration) {
      this.entityData.set(RADIATION_DURATION, duration);
      return this;
   }

   public int getRadiationDuration() {
      return this.entityData.get(RADIATION_DURATION);
   }

   public double getSimulationSpeed() {
      int lifetime = this.getMaxAge();
      int simSlow = lifetime / 4;
      int simStop = lifetime / 2;
      int life = this.age;
      if (life > simStop) {
         return 0.0;
      } else {
         return life > simSlow ? 1.0 - (double)(life - simSlow) / (double)(simStop - simSlow) : 1.0;
      }
   }

   public int getAge() {
      long startTime = this.entityData.get(START_TIME);
      return startTime >= 0L ? Math.max(0, (int)(this.level().getGameTime() - startTime)) : this.age;
   }

   public boolean isSimulationReady() {
      return this.entityData.get(START_TIME) >= 0L;
   }

   public double getScale() {
      return (double)((Float)this.entityData.get(SCALE)).floatValue();
   }

   public double getSaturation() {
      double d = (double)this.age / (double)this.getMaxAge();
      return 1.0 - d * d * d * d;
   }

   public double getGreying() {
      int lifetime = this.getMaxAge();
      int greying = lifetime * 3 / 4;
      return this.age > greying ? 1.0 + (double)(this.age - greying) / (double)(lifetime - greying) : 1.0;
   }

   public float getAlpha() {
      int lifetime = this.getMaxAge();
      int fadeOut = lifetime * 3 / 4;
      int life = this.age;
      if (life > fadeOut) {
         float fac = (float)(life - fadeOut) / (float)(lifetime - fadeOut);
         return 1.0F - fac;
      } else {
         return 1.0F;
      }
   }

   public int getMaxAge() {
      double s = this.getScale();
      return (int)(900.0 * s);
   }

   public static void statFac(Level world, double x, double y, double z, float scale) {
      EntityNukeTorex torex = new EntityNukeTorex(world).setScale(Math.clamp((float)Util.squirt((double)scale * 0.01) * 1.5F, 0.5F, 5.0F));
      torex.setPos(x, y, z);
      world.addFreshEntity(torex);
   }

   public class Cloudlet {
      public double posX;
      public double posY;
      public double posZ;
      public double prevPosX;
      public double prevPosY;
      public double prevPosZ;
      public double motionX;
      public double motionY;
      public double motionZ;
      public int age;
      public int cloudletLife;
      public float angle;
      public boolean isDead = false;
      float rangeMod = 1.0F;
      public float colorMod = 1.0F;
      public Vec3Old color;
      public Vec3Old prevColor;
      public EntityNukeTorex.TorexType type;
      private float startingScale = 1.0F;
      private float growingScale = 5.0F;
      private double motionMult = 1.0;

      public Cloudlet(double posX, double posY, double posZ, float angle, int age, int maxAge) {
         this(posX, posY, posZ, angle, age, maxAge, EntityNukeTorex.TorexType.STANDARD);
      }

      public Cloudlet(double posX, double posY, double posZ, float angle, int age, int maxAge, EntityNukeTorex.TorexType type) {
         this.posX = posX;
         this.posY = posY;
         this.posZ = posZ;
         this.age = age;
         this.cloudletLife = maxAge;
         this.angle = angle;
         this.rangeMod = 0.3F + EntityNukeTorex.this.random.nextFloat() * 0.7F;
         this.colorMod = 0.8F + EntityNukeTorex.this.random.nextFloat() * 0.2F;
         this.type = type;
         this.updateColor();
      }


      private void update() {
         this.age++;
         if (this.age > this.cloudletLife) {
            this.isDead = true;
         }

         this.prevPosX = this.posX;
         this.prevPosY = this.posY;
         this.prevPosZ = this.posZ;
         Vec3Old simPos = Vec3Old.createVectorHelper(EntityNukeTorex.this.getX() - this.posX, 0.0, EntityNukeTorex.this.getZ() - this.posZ);
         double simPosX = EntityNukeTorex.this.getX() + simPos.lengthVector();
         double simPosZ = EntityNukeTorex.this.getZ() + 0.0;
         if (this.type == EntityNukeTorex.TorexType.STANDARD) {
            Vec3Old convection = this.getConvectionMotion(simPosX, simPosZ);
            Vec3Old lift = this.getLiftMotion(simPosX, simPosZ);
            double factor = Math.clamp((this.posY - EntityNukeTorex.this.getY()) / EntityNukeTorex.this.coreHeight, 0.0, 1.0);
            this.motionX = convection.xCoord * factor + lift.xCoord * (1.0 - factor);
            this.motionY = convection.yCoord * factor + lift.yCoord * (1.0 - factor);
            this.motionZ = convection.zCoord * factor + lift.zCoord * (1.0 - factor);
         } else if (this.type == EntityNukeTorex.TorexType.SHOCK) {
            double factor = Math.clamp((this.posY - EntityNukeTorex.this.getY()) / EntityNukeTorex.this.coreHeight, 0.0, 1.0);
            Vec3Old motion = Vec3Old.createVectorHelper(1.0, 0.0, 0.0);
            motion.rotateAroundY(this.angle);
            this.motionX = motion.xCoord * factor;
            this.motionY = motion.yCoord * factor;
            this.motionZ = motion.zCoord * factor;
         } else if (this.type == EntityNukeTorex.TorexType.RING) {
            Vec3Old motion = this.getRingMotion(simPosX, simPosZ);
            this.motionX = motion.xCoord;
            this.motionY = motion.yCoord;
            this.motionZ = motion.zCoord;
         } else if (this.type == EntityNukeTorex.TorexType.CONDENSATION) {
            Vec3Old motion = this.getCondensationMotion();
            this.motionX = motion.xCoord;
            this.motionY = motion.yCoord;
            this.motionZ = motion.zCoord;
         }

         double mult = this.motionMult * EntityNukeTorex.this.getSimulationSpeed();
         this.posX = this.posX + this.motionX * mult;
         this.posY = this.posY + this.motionY * mult;
         this.posZ = this.posZ + this.motionZ * mult;
         this.updateColor();
      }

      private Vec3Old getCondensationMotion() {
         Vec3Old delta = Vec3Old.createVectorHelper(this.posX - EntityNukeTorex.this.getX(), 0.0, this.posZ - EntityNukeTorex.this.getZ());
         double speed = 2.0E-5 * (double)EntityNukeTorex.this.age;
         delta.xCoord *= speed;
         delta.zCoord *= speed;
         return delta;
      }

      private Vec3Old getRingMotion(double simPosX, double simPosZ) {
         if (simPosX > EntityNukeTorex.this.getX() + EntityNukeTorex.this.torusWidth * 2.0) {
            return Vec3Old.createVectorHelper(0.0, 0.0, 0.0);
         } else {
            Vec3Old torusPos = Vec3Old.createVectorHelper(
               EntityNukeTorex.this.getX() + EntityNukeTorex.this.torusWidth,
               EntityNukeTorex.this.getY() + EntityNukeTorex.this.coreHeight * 0.5,
               EntityNukeTorex.this.getZ()
            );
            Vec3Old delta = Vec3Old.createVectorHelper(torusPos.xCoord - simPosX, torusPos.yCoord - this.posY, torusPos.zCoord - simPosZ);
            double roller = EntityNukeTorex.this.rollerSize * (double)this.rangeMod * 0.25;
            double dist = delta.lengthVector() / roller - 1.0;
            double func = 1.0 - Math.pow(Math.E, -dist);
            float angle = (float)(func * Math.PI * 0.5);
            Vec3Old rot = Vec3Old.createVectorHelper(-delta.xCoord / dist, -delta.yCoord / dist, -delta.zCoord / dist);
            rot.rotateAroundZ(angle);
            Vec3Old motion = Vec3Old.createVectorHelper(
               torusPos.xCoord + rot.xCoord - simPosX, torusPos.yCoord + rot.yCoord - this.posY, torusPos.zCoord + rot.zCoord - simPosZ
            );
            double speed = 0.001;
            motion.xCoord *= speed;
            motion.yCoord *= speed;
            motion.zCoord *= speed;
            motion = motion.normalize();
            motion.rotateAroundY(this.angle);
            return motion;
         }
      }

      private Vec3Old getConvectionMotion(double simPosX, double simPosZ) {
         Vec3Old torusPos = Vec3Old.createVectorHelper(
            EntityNukeTorex.this.getX() + EntityNukeTorex.this.torusWidth,
            EntityNukeTorex.this.getY() + EntityNukeTorex.this.coreHeight,
            EntityNukeTorex.this.getZ()
         );
         Vec3Old delta = Vec3Old.createVectorHelper(torusPos.xCoord - simPosX, torusPos.yCoord - this.posY, torusPos.zCoord - simPosZ);
         double roller = EntityNukeTorex.this.rollerSize * (double)this.rangeMod;
         double dist = delta.lengthVector() / roller - 1.0;
         double func = 1.0 - Math.pow(Math.E, -dist);
         float angle = (float)(func * Math.PI * 0.5);
         Vec3Old rot = Vec3Old.createVectorHelper(-delta.xCoord / dist, -delta.yCoord / dist, -delta.zCoord / dist);
         rot.rotateAroundZ(angle);
         Vec3Old motion = Vec3Old.createVectorHelper(
            torusPos.xCoord + rot.xCoord - simPosX, torusPos.yCoord + rot.yCoord - this.posY, torusPos.zCoord + rot.zCoord - simPosZ
         );
         motion = motion.normalize();
         motion.rotateAroundY(this.angle);
         return motion;
      }

      private Vec3Old getLiftMotion(double simPosX, double simPosZ) {
         double scale = Math.clamp(1.0 - (simPosX - (EntityNukeTorex.this.getX() + EntityNukeTorex.this.torusWidth)), 0.0, 1.0);
         Vec3Old motion = Vec3Old.createVectorHelper(
            EntityNukeTorex.this.getX() - this.posX,
            EntityNukeTorex.this.getY() + EntityNukeTorex.this.convectionHeight - this.posY,
            EntityNukeTorex.this.getZ() - this.posZ
         );
         motion = motion.normalize();
         motion.xCoord *= scale;
         motion.yCoord *= scale;
         motion.zCoord *= scale;
         return motion;
      }

      private void updateColor() {
         this.prevColor = this.color;
         double exX = EntityNukeTorex.this.getX();
         double exY = EntityNukeTorex.this.getY() + EntityNukeTorex.this.coreHeight;
         double exZ = EntityNukeTorex.this.getZ();
         double distX = exX - this.posX;
         double distY = exY - this.posY;
         double distZ = exZ - this.posZ;
         double distSq = distX * distX + distY * distY + distZ * distZ;
         distSq /= EntityNukeTorex.this.heat;
         double dist = Math.sqrt(distSq);
         dist = Math.max(dist, 1.0);
         double col = 2.0 / dist;
         int type = (Integer)EntityNukeTorex.this.entityData.get(EntityNukeTorex.TYPE);
         if (type == 1) {
            this.color = Vec3Old.createVectorHelper(Math.max(col * 1.0, 0.25), Math.max(col * 2.0, 0.25), Math.max(col * 0.5, 0.25));
         } else if (type == 2) {
            Color color = Color.getHSBColor(this.angle / 2.0F / (float) Math.PI, 1.0F, 1.0F);
            if (this.type == EntityNukeTorex.TorexType.RING) {
               this.color = Vec3Old.createVectorHelper(Math.max(col * 1.0, 0.25), Math.max(col * 1.0, 0.25), Math.max(col * 1.0, 0.25));
            } else {
               this.color = Vec3Old.createVectorHelper((double)color.getRed() / 255.0, (double)color.getGreen() / 255.0, (double)color.getBlue() / 255.0);
            }
         } else {
            this.color = Vec3Old.createVectorHelper(Math.max(col * 2.0, 0.25), Math.max(col * 1.5, 0.25), Math.max(col * 0.5, 0.25));
         }

         this.applyRadiationTint();
      }

      private void applyRadiationTint() {
         if (this.type == EntityNukeTorex.TorexType.SHOCK || this.type == EntityNukeTorex.TorexType.CONDENSATION) {
            return;
         }

         double horizontalDistance = Math.hypot(this.posX - EntityNukeTorex.this.getX(), this.posZ - EntityNukeTorex.this.getZ());
         double shellRadius = Math.max(2.0, EntityNukeTorex.this.torusWidth * 2.2 + EntityNukeTorex.this.rollerSize);
         double shellFactor = 1.0 - Math.clamp(horizontalDistance / shellRadius, 0.0, 1.0);
         double shellHeight = EntityNukeTorex.this.getY() + EntityNukeTorex.this.coreHeight * 0.55;
         double heightFactor = 1.0
            - Math.clamp(Math.abs(this.posY - shellHeight) / Math.max(2.0, EntityNukeTorex.this.coreHeight + EntityNukeTorex.this.rollerSize + 4.0), 0.0, 1.0);
         double ageFactor = 1.0 - Math.clamp((double)EntityNukeTorex.this.age / ((double)EntityNukeTorex.this.getMaxAge() * 0.55), 0.0, 1.0);
         double tint = shellFactor * heightFactor * ageFactor * 0.85;

         this.color = Vec3Old.createVectorHelper(
            this.color.xCoord + (0.55 - this.color.xCoord) * tint,
            this.color.yCoord + (1.0 - this.color.yCoord) * tint,
            this.color.zCoord + (0.05 - this.color.zCoord) * tint
         );
      }

      public Vec3Old getInterpPos(float interp) {
         float scale = (float)EntityNukeTorex.this.getScale();
         Vec3Old base = Vec3Old.createVectorHelper(
            this.prevPosX + (this.posX - this.prevPosX) * (double)interp,
            this.prevPosY + (this.posY - this.prevPosY) * (double)interp,
            this.prevPosZ + (this.posZ - this.prevPosZ) * (double)interp
         );
         if (this.type != EntityNukeTorex.TorexType.SHOCK) {
            base.xCoord = (base.xCoord - EntityNukeTorex.this.getX()) * (double)scale + EntityNukeTorex.this.getX();
            base.yCoord = (base.yCoord - EntityNukeTorex.this.getY()) * (double)scale + EntityNukeTorex.this.getY();
            base.zCoord = (base.zCoord - EntityNukeTorex.this.getZ()) * (double)scale + EntityNukeTorex.this.getZ();
         }

         return base;
      }

      public Vec3Old getInterpColor(float interp) {
         if (this.type == EntityNukeTorex.TorexType.CONDENSATION) {
            return Vec3Old.createVectorHelper(1.0, 1.0, 1.0);
         } else {
            double greying = EntityNukeTorex.this.getGreying();
            if (this.type == EntityNukeTorex.TorexType.RING) {
               greying++;
            }

            return Vec3Old.createVectorHelper(
               (this.prevColor.xCoord + (this.color.xCoord - this.prevColor.xCoord) * (double)interp) * greying,
               (this.prevColor.yCoord + (this.color.yCoord - this.prevColor.yCoord) * (double)interp) * greying,
               (this.prevColor.zCoord + (this.color.zCoord - this.prevColor.zCoord) * (double)interp) * greying
            );
         }
      }

      public float getAlpha() {
         float fadeStart = this.type == EntityNukeTorex.TorexType.SHOCK ? 0.30F : 0.75F;
         float fadeProgress = Math.clamp(((float)this.age / (float)this.cloudletLife - fadeStart) / (1.0F - fadeStart), 0.0F, 1.0F);
         float fade = this.type == EntityNukeTorex.TorexType.SHOCK
            ? 1.0F - fadeProgress
            : 1.0F - fadeProgress * fadeProgress * (3.0F - 2.0F * fadeProgress);
         float alpha = fade * EntityNukeTorex.this.getAlpha();
         if (this.type == EntityNukeTorex.TorexType.CONDENSATION) {
            alpha *= 0.25F;
         }

         return alpha;
      }

      public float getScale() {
         float base = this.startingScale + (float)this.age / (float)this.cloudletLife * this.growingScale;
         if (this.type != EntityNukeTorex.TorexType.SHOCK) {
            base *= (float)EntityNukeTorex.this.getScale();
         } else {
            base *= 0.5F;
         }

         return base;
      }

      public EntityNukeTorex.Cloudlet setScale(float start, float grow) {
         this.startingScale = start;
         this.growingScale = grow;
         return this;
      }

      public EntityNukeTorex.Cloudlet setMotion(double mult) {
         this.motionMult = mult;
         return this;
      }
   }

   public static enum TorexType {
      STANDARD,
      SHOCK,
      RING,
      CONDENSATION;
   }

}
