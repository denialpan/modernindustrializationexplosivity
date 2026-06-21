package com.ddd.modernindustrializationexplosivity.nuke.items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record SelectedNuke(int x, int y, int z) {
   public static final Codec<SelectedNuke> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               Codec.INT.fieldOf("x").forGetter(SelectedNuke::x),
               Codec.INT.fieldOf("y").forGetter(SelectedNuke::y),
               Codec.INT.fieldOf("z").forGetter(SelectedNuke::z)
            )
            .apply(instance, SelectedNuke::new)
   );

   public BlockPos getBlockPos() {
      return new BlockPos(this.x, this.y, this.z);
   }
}
