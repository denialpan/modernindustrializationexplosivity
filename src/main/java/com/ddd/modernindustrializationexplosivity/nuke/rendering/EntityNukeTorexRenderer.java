package com.ddd.modernindustrializationexplosivity.nuke.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.awt.Color;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import com.ddd.modernindustrializationexplosivity.ModernIndustrializationExplosivity;
import com.ddd.modernindustrializationexplosivity.nuke.NukeSounds;
import com.ddd.modernindustrializationexplosivity.nuke.entity.EntityNukeTorex;

public class EntityNukeTorexRenderer extends EntityRenderer<EntityNukeTorex> {
   public static final ResourceLocation CLOUDLET_TEXTURE = ResourceLocation.fromNamespaceAndPath("modern_industrialization_explosivity", "textures/particle_base.png");
   public static final ResourceLocation FLASH_TEXTURE = ResourceLocation.fromNamespaceAndPath("modern_industrialization_explosivity", "textures/flare.png");
   private static final RenderType CLOUDLET_TYPE = RenderType.entityTranslucent(CLOUDLET_TEXTURE);
   private static final RenderType FLASH_TYPE = RenderType.entityTranslucent(FLASH_TEXTURE);

   public EntityNukeTorexRenderer(Context context) {
      super(context);
   }

   public boolean shouldRender(EntityNukeTorex entity, Frustum frustum, double x, double y, double z) {
      double dx = entity.getX() - x;
      double dy = entity.getY() - y;
      double dz = entity.getZ() - z;
      return entity.shouldRenderAtSqrDistance(dx * dx + dy * dy + dz * dz);
   }

   public void render(EntityNukeTorex entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
      if (!entity.isSimulationReady()) {
         return;
      }

      EntityNukeTorex simulation = EntityNukeTorex.getClientSimulation(entity);
      for (EntityNukeTorex.Cloudlet cloudlet : simulation.cloudlets) {
         float scale = cloudlet.getScale();
         Vec3 pos = cloudlet.getInterpPos(partialTicks).toVec3d();
         Vec3 colf = cloudlet.getInterpColor(partialTicks).toVec3d();
         Color col = new Color(
            (float)Math.clamp(colf.x, 0.0, 1.0),
            (float)Math.clamp(colf.y, 0.0, 1.0),
            (float)Math.clamp(colf.z, 0.0, 1.0),
            Math.clamp(cloudlet.getAlpha() * 0.9F, 0.0F, 1.0F)
         );
         this.renderBillBoard(scale, poseStack, buffer, CLOUDLET_TYPE, pos.subtract(simulation.position()), col);
      }

      Player player = Minecraft.getInstance().player;
      if (!entity.didIrradiate) {
         ModernIndustrializationExplosivity.radiationStartTime = entity.level().getDayTime() - (long)entity.getAge();
         ModernIndustrializationExplosivity.radiationX = entity.getX();
         ModernIndustrializationExplosivity.radiationY = entity.getY();
         ModernIndustrializationExplosivity.radiationZ = entity.getZ();
         ModernIndustrializationExplosivity.radiationRadius = entity.getRadiationRadius();
         entity.didIrradiate = true;
      }

      if (player != null && entity.getAge() < 20 && (double)player.distanceTo(entity) < ((double)entity.getAge() * 1.5 + 1.0) * 1.5 && !entity.didPlaySound) {
         entity.level()
            .playLocalSound(
               new BlockPos(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ()),
               (SoundEvent)NukeSounds.NUCLEAR_EXPLOSION.get(),
               SoundSource.BLOCKS,
               1.0F,
               1.0F,
               false
            );
         entity.didPlaySound = true;
      }

      if (entity.getAge() < 101) {
         this.renderFlashes(entity, partialTicks, poseStack, buffer);
      }

      if (entity.getAge() < 10 && System.currentTimeMillis() - ModernIndustrializationExplosivity.flashTimestamp > 1000L) {
         ModernIndustrializationExplosivity.flashTimestamp = System.currentTimeMillis();
      }

      if (entity.didPlaySound && !entity.didShake && System.currentTimeMillis() - ModernIndustrializationExplosivity.shakeTimestamp > 1000L) {
         ModernIndustrializationExplosivity.shakeTimestamp = System.currentTimeMillis();
         entity.didShake = true;
         player.hurtTime = 15;
      }
   }

   private void renderFlashes(EntityNukeTorex cloud, float interp, PoseStack poseStack, MultiBufferSource buffer) {
      double age = (double)Math.min((float)cloud.getAge() + interp, 100.0F);
      float alpha = (float)((100.0 - age) / 100.0);
      Random rand = new Random((long)cloud.getId());

      for (int i = 0; i < 3; i++) {
         float x = (float)(rand.nextGaussian() * 0.5 * cloud.rollerSize);
         float y = (float)(rand.nextGaussian() * 0.5 * cloud.rollerSize);
         float z = (float)(rand.nextGaussian() * 0.5 * cloud.rollerSize);
         Color color = new Color(1.0F, 1.0F, 1.0F, alpha);
         this.renderBillBoard(
            (float)(25.0 * cloud.rollerSize * 2.0), poseStack, buffer, FLASH_TYPE, new Vec3((double)x, (double)y + cloud.coreHeight, (double)z), color
         );
      }
   }

   private void renderBillBoard(float scale, PoseStack poseStack, MultiBufferSource buffer, RenderType type, Vec3 offset, Color color) {
      poseStack.pushPose();
      poseStack.translate(offset.x, offset.y, offset.z);
      poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
      poseStack.scale(scale, scale, scale);
      VertexConsumer vertexconsumer = buffer.getBuffer(type);
      Pose pose = poseStack.last();
      vertex(vertexconsumer, pose, -1.0F, -1.0F, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), 0.0F, 0.0F, 15728880);
      vertex(vertexconsumer, pose, 1.0F, -1.0F, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), 1.0F, 0.0F, 15728880);
      vertex(vertexconsumer, pose, 1.0F, 1.0F, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), 1.0F, 1.0F, 15728880);
      vertex(vertexconsumer, pose, -1.0F, 1.0F, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), 0.0F, 1.0F, 15728880);
      poseStack.popPose();
   }

   private static void vertex(VertexConsumer consumer, Pose pose, float x, float y, int red, int green, int blue, int alpha, float u, float v, int packedLight) {
      consumer.addVertex(pose, x, y, 0.0F)
         .setColor(red, green, blue, alpha)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(packedLight)
         .setNormal(pose, 0.0F, 1.0F, 0.0F);
   }

   public ResourceLocation getTextureLocation(EntityNukeTorex entityNukeTorex) {
      return null;
   }
}
