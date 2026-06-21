package com.ddd.modernindustrializationexplosivity.nuke.rendering;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class TorexRenderer {
   public static List<TorexRenderer.Beam> clouds = new ArrayList<>();

   public static void renderClouds(PoseStack poseStack, Matrix4f modelViewMatrix, Camera camera) {
   }

   public static void renderBeam(PoseStack poseStack, Vec3 start, Vec3 end, float width, Color color, Camera camera, Matrix4f modelViewMatrix) {
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.disableCull();
      RenderSystem.enableBlend();
      BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      poseStack.pushPose();
      poseStack.mulPose(modelViewMatrix);
      poseStack.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
      Matrix4f matrix = poseStack.last().pose();
      Vec3 beamDir = end.subtract(start).normalize();
      Vec3 toCamera = camera.getPosition().subtract(start).normalize();
      Vec3 perpendicular = beamDir.cross(toCamera).normalize().scale((double)width / 2.0);
      if (perpendicular.lengthSqr() < 1.0E-6) {
         perpendicular = beamDir.cross(new Vec3(0.0, 1.0, 0.0)).normalize().scale((double)width / 2.0);
      }

      Vec3 v1 = start.add(perpendicular);
      Vec3 v2 = start.subtract(perpendicular);
      Vec3 v3 = end.subtract(perpendicular);
      Vec3 v4 = end.add(perpendicular);
      float r = (float)color.getRed() / 255.0F;
      float g = (float)color.getGreen() / 255.0F;
      float b = (float)color.getBlue() / 255.0F;
      float a = (float)color.getAlpha() / 255.0F;
      buffer.addVertex(matrix, (float)v1.x, (float)v1.y, (float)v1.z).setColor(r, g, b, a);
      buffer.addVertex(matrix, (float)v2.x, (float)v2.y, (float)v2.z).setColor(r, g, b, a);
      buffer.addVertex(matrix, (float)v3.x, (float)v3.y, (float)v3.z).setColor(r, g, b, a);
      buffer.addVertex(matrix, (float)v4.x, (float)v4.y, (float)v4.z).setColor(r, g, b, a);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      poseStack.popPose();
   }

   public static void renderQuad(PoseStack poseStack, Vec3 pos, float width, Color color, Camera camera, Matrix4f modelViewMatrix) {
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.disableCull();
      RenderSystem.enableDepthTest();
      RenderSystem.enableBlend();
      BufferBuilder buffer = Tesselator.getInstance().begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      poseStack.pushPose();
      poseStack.mulPose(modelViewMatrix);
      poseStack.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
      poseStack.mulPose(camera.rotation());
      Matrix4f matrix = poseStack.last().pose();
      Vec3 v1 = new Vec3(-0.5, 0.5, 0.0);
      Vec3 v2 = new Vec3(0.5, 0.5, 0.0);
      Vec3 v3 = new Vec3(0.5, -0.5, 0.0);
      Vec3 v4 = new Vec3(-0.5, -0.5, 0.0);
      float r = (float)color.getRed() / 255.0F;
      float g = (float)color.getGreen() / 255.0F;
      float b = (float)color.getBlue() / 255.0F;
      float a = (float)color.getAlpha() / 255.0F;
      buffer.addVertex(matrix, (float)v1.x, (float)v1.y, (float)v1.z).setColor(r, g, b, a);
      buffer.addVertex(matrix, (float)v2.x, (float)v2.y, (float)v2.z).setColor(r, g, b, a);
      buffer.addVertex(matrix, (float)v3.x, (float)v3.y, (float)v3.z).setColor(r, g, b, a);
      buffer.addVertex(matrix, (float)v4.x, (float)v4.y, (float)v4.z).setColor(r, g, b, a);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      poseStack.popPose();
   }

   public static class Beam {
      public Vec3 start;
      public Vec3 end;
      public long created;
      public static final int DURATION = 1500;
      public int depth;

      public Beam(Vec3 start, Vec3 end, int depth) {
         this.start = start;
         this.end = end;
         this.created = System.currentTimeMillis();
         this.depth = depth;
      }

      public Beam(Entity start, Entity end, int depth) {
         this.start = start.position();
         this.end = end.position();
         this.created = System.currentTimeMillis();
         this.depth = depth;
      }
   }
}
