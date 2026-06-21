package com.ddd.modernindustrializationexplosivity.nuke;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class Util {
   public static double squirt(double x) {
      return Math.sqrt(x + 1.0 / ((x + 2.0) * (x + 2.0))) - 1.0 / (x + 2.0);
   }

   public static boolean isObstructed(Level world, Vec3 start, Vec3 end, Entity ent) {
      HitResult result = world.clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, ent));
      return result.getType() == Type.BLOCK;
   }
}
