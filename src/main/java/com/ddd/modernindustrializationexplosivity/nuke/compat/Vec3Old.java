package com.ddd.modernindustrializationexplosivity.nuke.compat;

import net.minecraft.world.phys.Vec3;

public class Vec3Old {
   public double xCoord;
   public double yCoord;
   public double zCoord;
   private static final String __OBFID = "CL_00000612";

   public static Vec3Old createVectorHelper(double x, double y, double z) {
      return new Vec3Old(x, y, z);
   }

   protected Vec3Old(double x, double y, double z) {
      if (x == -0.0) {
         x = 0.0;
      }

      if (y == -0.0) {
         y = 0.0;
      }

      if (z == -0.0) {
         z = 0.0;
      }

      this.xCoord = x;
      this.yCoord = y;
      this.zCoord = z;
   }

   protected Vec3Old setComponents(double x, double y, double z) {
      this.xCoord = x;
      this.yCoord = y;
      this.zCoord = z;
      return this;
   }

   public Vec3Old subtract(Vec3Old vec) {
      return createVectorHelper(vec.xCoord - this.xCoord, vec.yCoord - this.yCoord, vec.zCoord - this.zCoord);
   }

   public Vec3Old normalize() {
      double d0 = Math.sqrt((double)((float)(this.xCoord * this.xCoord + this.yCoord * this.yCoord + this.zCoord * this.zCoord)));
      return d0 < 1.0E-4 ? createVectorHelper(0.0, 0.0, 0.0) : createVectorHelper(this.xCoord / d0, this.yCoord / d0, this.zCoord / d0);
   }

   public double dotProduct(Vec3Old vec) {
      return this.xCoord * vec.xCoord + this.yCoord * vec.yCoord + this.zCoord * vec.zCoord;
   }

   public Vec3Old crossProduct(Vec3Old vec) {
      return createVectorHelper(
         this.yCoord * vec.zCoord - this.zCoord * vec.yCoord,
         this.zCoord * vec.xCoord - this.xCoord * vec.zCoord,
         this.xCoord * vec.yCoord - this.yCoord * vec.xCoord
      );
   }

   public Vec3Old addVector(double x, double y, double z) {
      return createVectorHelper(this.xCoord + x, this.yCoord + y, this.zCoord + z);
   }

   public double distanceTo(Vec3Old vec) {
      double d0 = vec.xCoord - this.xCoord;
      double d1 = vec.yCoord - this.yCoord;
      double d2 = vec.zCoord - this.zCoord;
      return Math.sqrt((double)((float)(d0 * d0 + d1 * d1 + d2 * d2)));
   }

   public double squareDistanceTo(Vec3Old vec) {
      double d0 = vec.xCoord - this.xCoord;
      double d1 = vec.yCoord - this.yCoord;
      double d2 = vec.zCoord - this.zCoord;
      return d0 * d0 + d1 * d1 + d2 * d2;
   }

   public double squareDistanceTo(double x, double y, double z) {
      double d3 = x - this.xCoord;
      double d4 = y - this.yCoord;
      double d5 = z - this.zCoord;
      return d3 * d3 + d4 * d4 + d5 * d5;
   }

   public double lengthVector() {
      return Math.sqrt((double)((float)(this.xCoord * this.xCoord + this.yCoord * this.yCoord + this.zCoord * this.zCoord)));
   }

   public Vec3Old getIntermediateWithXValue(Vec3Old vec, double x) {
      double d1 = vec.xCoord - this.xCoord;
      double d2 = vec.yCoord - this.yCoord;
      double d3 = vec.zCoord - this.zCoord;
      if (d1 * d1 < 1.0E-7F) {
         return null;
      } else {
         double d4 = (x - this.xCoord) / d1;
         return d4 >= 0.0 && d4 <= 1.0 ? createVectorHelper(this.xCoord + d1 * d4, this.yCoord + d2 * d4, this.zCoord + d3 * d4) : null;
      }
   }

   public Vec3Old getIntermediateWithYValue(Vec3Old vec, double y) {
      double d1 = vec.xCoord - this.xCoord;
      double d2 = vec.yCoord - this.yCoord;
      double d3 = vec.zCoord - this.zCoord;
      if (d2 * d2 < 1.0E-7F) {
         return null;
      } else {
         double d4 = (y - this.yCoord) / d2;
         return d4 >= 0.0 && d4 <= 1.0 ? createVectorHelper(this.xCoord + d1 * d4, this.yCoord + d2 * d4, this.zCoord + d3 * d4) : null;
      }
   }

   public Vec3Old getIntermediateWithZValue(Vec3Old vec, double z) {
      double d1 = vec.xCoord - this.xCoord;
      double d2 = vec.yCoord - this.yCoord;
      double d3 = vec.zCoord - this.zCoord;
      if (d3 * d3 < 1.0E-7F) {
         return null;
      } else {
         double d4 = (z - this.zCoord) / d3;
         return d4 >= 0.0 && d4 <= 1.0 ? createVectorHelper(this.xCoord + d1 * d4, this.yCoord + d2 * d4, this.zCoord + d3 * d4) : null;
      }
   }

   @Override
   public String toString() {
      return "(" + this.xCoord + ", " + this.yCoord + ", " + this.zCoord + ")";
   }

   public void rotateAroundX(float angle) {
      float f1 = (float)Math.cos((double)angle);
      float f2 = (float)Math.sin((double)angle);
      double d0 = this.xCoord;
      double d1 = this.yCoord * (double)f1 + this.zCoord * (double)f2;
      double d2 = this.zCoord * (double)f1 - this.yCoord * (double)f2;
      this.setComponents(d0, d1, d2);
   }

   public void rotateAroundY(float angle) {
      float f1 = (float)Math.cos((double)angle);
      float f2 = (float)Math.sin((double)angle);
      double d0 = this.xCoord * (double)f1 + this.zCoord * (double)f2;
      double d1 = this.yCoord;
      double d2 = this.zCoord * (double)f1 - this.xCoord * (double)f2;
      this.setComponents(d0, d1, d2);
   }

   public void rotateAroundZ(float angle) {
      float f1 = (float)Math.cos((double)angle);
      float f2 = (float)Math.sin((double)angle);
      double d0 = this.xCoord * (double)f1 + this.yCoord * (double)f2;
      double d1 = this.yCoord * (double)f1 - this.xCoord * (double)f2;
      double d2 = this.zCoord;
      this.setComponents(d0, d1, d2);
   }

   public Vec3 toVec3d() {
      return new Vec3(this.xCoord, this.yCoord, this.zCoord);
   }
}
