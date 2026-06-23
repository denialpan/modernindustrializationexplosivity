package com.ddd.modernindustrializationexplosivity.nuke.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import com.ddd.modernindustrializationexplosivity.ExplosivityConfig;
import com.ddd.modernindustrializationexplosivity.nuke.NukeEntities;

public class EntityRadiationZone extends Entity {
    private static final EntityDataAccessor<Long> START_TIME = SynchedEntityData.defineId(EntityRadiationZone.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(EntityRadiationZone.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DURATION_TICKS = SynchedEntityData.defineId(EntityRadiationZone.class, EntityDataSerializers.INT);

    public EntityRadiationZone(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public static EntityRadiationZone create(Level level, double x, double y, double z, double radius, int durationTicks) {
        EntityRadiationZone zone = new EntityRadiationZone(NukeEntities.RADIATION_ZONE.get(), level);
        zone.entityData.set(START_TIME, level.getDayTime());
        zone.entityData.set(RADIUS, (float) radius);
        zone.entityData.set(DURATION_TICKS, durationTicks);
        zone.setPos(x, y, z);
        return zone;
    }

    @Override
    protected void defineSynchedData(Builder builder) {
        builder.define(START_TIME, -1L);
        builder.define(RADIUS, 200.0F);
        builder.define(DURATION_TICKS, 48000);
    }

    public long getRadiationStartTime() {
        return this.entityData.get(START_TIME);
    }

    public float getRadiationRadius() {
        return this.entityData.get(RADIUS);
    }

    public int getRadiationDurationTicks() {
        return this.entityData.get(DURATION_TICKS);
    }

    @Override
    public boolean isAlwaysTicking() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        if (this.getRadiationStartTime() < 0L) this.entityData.set(START_TIME, this.level().getDayTime());
        if (this.level().getDayTime() - this.getRadiationStartTime() >= this.getRadiationDurationTicks()) {
            this.discard();
            return;
        }
        if (this.tickCount % 20 != 0) return;

        AABB bounds = this.getBoundingBox().inflate(this.getRadiationRadius());
        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, bounds)) {
            double distance = entity.position().distanceTo(this.position());
            if (distance > this.getRadiationRadius()) continue;
            int amplifier = Math.min(4, (int) Math.floor((1.0 - distance / this.getRadiationRadius()) * 5.0));
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 200, amplifier, false, true, true));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 0, false, true, true));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0, false, true, true));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(START_TIME, tag.getLong("StartTime"));
        this.entityData.set(RADIUS, tag.contains("Radius") ? (float)tag.getDouble("Radius") : 200.0F);
        this.entityData.set(DURATION_TICKS, tag.contains("DurationTicks") ? tag.getInt("DurationTicks") : ExplosivityConfig.RADIATION_DURATION_TICKS.get());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("StartTime", this.getRadiationStartTime());
        tag.putDouble("Radius", this.getRadiationRadius());
        tag.putInt("DurationTicks", this.getRadiationDurationTicks());
    }
}
