package com.ddd.modernindustrializationexplosivity.nuke;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class NukeSounds {
   public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, "modern_industrialization_explosivity");
   public static final DeferredHolder<SoundEvent, SoundEvent> NUCLEAR_EXPLOSION = SOUND_EVENTS.register(
      "nuclear_explosion", () -> SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath("modern_industrialization_explosivity", "nuclear_explosion"), 16.0F)
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> RAILGUN = SOUND_EVENTS.register(
      "railgun", () -> SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath("modern_industrialization_explosivity", "railgun"), 16.0F)
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> COIN = SOUND_EVENTS.register(
      "coin", () -> SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath("modern_industrialization_explosivity", "coin"), 16.0F)
   );

   public static void register(IEventBus modBus) {
      SOUND_EVENTS.register(modBus);
   }
}
