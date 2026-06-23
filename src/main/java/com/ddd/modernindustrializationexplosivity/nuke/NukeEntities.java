package com.ddd.modernindustrializationexplosivity.nuke;

import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.ddd.modernindustrializationexplosivity.nuke.entity.EntityNukeExplosion;
import com.ddd.modernindustrializationexplosivity.nuke.entity.EntityNukeTorex;
import com.ddd.modernindustrializationexplosivity.nuke.entity.EntityNukeCountdown;
import com.ddd.modernindustrializationexplosivity.nuke.entity.EntityRadiationZone;

public class NukeEntities {
   public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, "modern_industrialization_explosivity");
   public static final Supplier<EntityType<EntityNukeExplosion>> NUKE = ENTITY_TYPES.register(
      "nuke", () -> Builder.<EntityNukeExplosion>of(EntityNukeExplosion::new, MobCategory.MISC).sized(1.0F, 1.0F).build("nuke")
   );
   public static final Supplier<EntityType<EntityNukeTorex>> TOREX = ENTITY_TYPES.register(
      "torex", () -> Builder.<EntityNukeTorex>of(EntityNukeTorex::new, MobCategory.MISC)
            .sized(1.0F, 1.0F)
            .clientTrackingRange(128)
            .updateInterval(1)
            .build("torex")
   );
   public static final Supplier<EntityType<EntityNukeCountdown>> COUNTDOWN = ENTITY_TYPES.register(
      "nuke_countdown", () -> Builder.<EntityNukeCountdown>of(EntityNukeCountdown::new, MobCategory.MISC)
            .sized(0.1F, 0.1F)
            .clientTrackingRange(20)
            .updateInterval(1)
            .build("nuke_countdown")
   );
   public static final Supplier<EntityType<EntityRadiationZone>> RADIATION_ZONE = ENTITY_TYPES.register(
      "radiation_zone", () -> Builder.<EntityRadiationZone>of(EntityRadiationZone::new, MobCategory.MISC)
            .sized(0.1F, 0.1F)
            .clientTrackingRange(30)
            .updateInterval(20)
            .build("radiation_zone")
   );

   public static void register(IEventBus modEventBus) {
      ENTITY_TYPES.register(modEventBus);
   }
}
