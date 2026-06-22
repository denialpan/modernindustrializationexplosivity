package com.ddd.modernindustrializationexplosivity.nuke.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import com.ddd.modernindustrializationexplosivity.ModernIndustrializationExplosivity;
import com.ddd.modernindustrializationexplosivity.nuke.NukeEntities;

public class EntityRadiationZone extends Entity {
    public static final int DURATION_TICKS = 24000;
    private long startTime = -1L;
    private double radius = 200.0;

    public EntityRadiationZone(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public static EntityRadiationZone create(Level level, double x, double y, double z, double radius) {
        EntityRadiationZone zone = new EntityRadiationZone(NukeEntities.RADIATION_ZONE.get(), level);
        zone.startTime = level.getDayTime();
        zone.radius = radius;
        zone.setPos(x, y, z);
        return zone;
    }

    @Override
    protected void defineSynchedData(Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        if (this.startTime < 0L) this.startTime = this.level().getDayTime();
        if (this.level().getDayTime() - this.startTime >= DURATION_TICKS) {
            this.discard();
            return;
        }
        if (this.tickCount % 20 != 0) return;

        AABB bounds = this.getBoundingBox().inflate(this.radius);
        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, bounds)) {
            double distance = entity.position().distanceTo(this.position());
            if (distance > this.radius) continue;
            int amplifier = Math.min(4, (int) Math.floor((1.0 - distance / this.radius) * 5.0));
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 200, amplifier, false, true, true));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 0, false, true, true));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0, false, true, true));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.startTime = tag.getLong("StartTime");
        this.radius = tag.contains("Radius") ? tag.getDouble("Radius") : 200.0;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("StartTime", this.startTime);
        tag.putDouble("Radius", this.radius);
    }
}
