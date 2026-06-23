package com.ddd.modernindustrializationexplosivity;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-authoritative balance settings for explosive blocks. */
public final class ExplosivityConfig {
   public static final ModConfigSpec SPEC;
   public static final ModConfigSpec.IntValue INDUSTRIAL_TNT_STRENGTH;
   public static final ModConfigSpec.IntValue NUKE_STRENGTH;
   public static final ModConfigSpec.IntValue NUKE_COUNTDOWN_SECONDS;
   public static final ModConfigSpec.IntValue RADIATION_DURATION_TICKS;
   public static final ModConfigSpec.IntValue MAX_NUKE_RAY_COUNT;
   public static final ModConfigSpec.BooleanValue UPDATE_NEIGHBORS_DURING_DESTRUCTION;

   static {
      ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
      builder.translation("modern_industrialization_explosivity.configuration.industrialTnt").push("industrialTnt");
      INDUSTRIAL_TNT_STRENGTH = builder.comment("Explosion strength. Vanilla TNT has a strength of 4.")
         .translation("modern_industrialization_explosivity.configuration.industrialTnt.strength")
         .defineInRange("strength", 24, 16, 128);
      builder.pop();

      builder.translation("modern_industrialization_explosivity.configuration.nuke").push("nuke");
      NUKE_STRENGTH = builder.comment("Destruction radius and strength of a nuclear detonation.")
         .translation("modern_industrialization_explosivity.configuration.nuke.strength")
         .defineInRange("strength", 140, 140, 320);
      NUKE_COUNTDOWN_SECONDS = builder.comment("Delay between using a linked detonator and detonation, in seconds.")
         .translation("modern_industrialization_explosivity.configuration.nuke.countdownSeconds")
         .defineInRange("countdownSeconds", 10, 5, 30);
      RADIATION_DURATION_TICKS = builder.comment("How long the radiation region remains active, in ticks.")
         .translation("modern_industrialization_explosivity.configuration.nuke.radiationDurationTicks")
         .defineInRange("radiationDurationTicks", 48000, 20, Integer.MAX_VALUE);
      MAX_NUKE_RAY_COUNT = builder.comment("Maximum number of rays used for one nuke. Higher values improve outer blast detail but increase calculation time.")
         .translation("modern_industrialization_explosivity.configuration.nuke.maxRayCount")
         .defineInRange("maxRayCount", 500_000, 100_000, 5_000_000);
      UPDATE_NEIGHBORS_DURING_DESTRUCTION = builder.comment("Whether mass block removal notifies neighboring blocks. Disabling this reduces update cascades during destruction.")
         .translation("modern_industrialization_explosivity.configuration.nuke.updateNeighborsDuringDestruction")
         .define("updateNeighborsDuringDestruction", false);
      builder.pop();
      SPEC = builder.build();
   }

   private ExplosivityConfig() {
   }
}
