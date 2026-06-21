package com.ddd.modernindustrializationexplosivity;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-authoritative balance settings for explosive blocks. */
public final class ExplosivityConfig {
   public static final ModConfigSpec SPEC;
   public static final ModConfigSpec.IntValue INDUSTRIAL_TNT_STRENGTH;
   public static final ModConfigSpec.IntValue NUKE_STRENGTH;
   public static final ModConfigSpec.IntValue NUKE_COUNTDOWN_SECONDS;

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
         .defineInRange("strength", 240, 140, 320);
      NUKE_COUNTDOWN_SECONDS = builder.comment("Delay between using a linked detonator and detonation, in seconds.")
         .translation("modern_industrialization_explosivity.configuration.nuke.countdownSeconds")
         .defineInRange("countdownSeconds", 10, 5, 30);
      builder.pop();
      SPEC = builder.build();
   }

   private ExplosivityConfig() {
   }
}
