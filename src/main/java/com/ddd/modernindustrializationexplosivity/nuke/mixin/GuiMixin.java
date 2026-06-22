package com.ddd.modernindustrializationexplosivity.nuke.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import com.ddd.modernindustrializationexplosivity.ModernIndustrializationExplosivity;
import com.ddd.modernindustrializationexplosivity.nuke.NukeItems;
import com.ddd.modernindustrializationexplosivity.nuke.entity.EntityNukeExplosion;
import com.ddd.modernindustrializationexplosivity.nuke.items.DummyEnt;
import com.ddd.modernindustrializationexplosivity.nuke.items.RangeFinderItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Gui.class})
public abstract class GuiMixin {
   @Unique
   private static final ResourceLocation GOGGLES = ResourceLocation.fromNamespaceAndPath("modern_industrialization_explosivity", "textures/overlay_goggles.png");
   @Unique
   private static int widest = 0;
   @Unique
   private static int lines = 0;
   @Unique
   private static float radiationAlpha = 0.0F;

   @Shadow
   protected abstract void renderTextureOverlay(GuiGraphics var1, ResourceLocation var2, float var3);

   @Inject(
      method = {"renderCameraOverlays"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/DeltaTracker;getGameTimeDeltaTicks()F"
      )}
   )
   public void renderCameraOverlays(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      Minecraft client = Minecraft.getInstance();
      if (client.options.getCameraType().isFirstPerson()) {
         ItemStack itemStack = client.player.getInventory().getArmor(3);
         if (!client.player.isScoping() && itemStack.is(NukeItems.GOGGLES.asItem())) {
            this.renderTextureOverlay(guiGraphics, GOGGLES, 0.8F);
         } else {
            renderFlash(guiGraphics);
         }
         renderRadiation(guiGraphics, client);
      }
   }

   @Inject(
      method = {"renderCameraOverlays"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;getTicksFrozen()I"
      )}
   )
   public void renderCameraOverlaysRangeFinder(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      Minecraft client = Minecraft.getInstance();
      if (client.options.getCameraType().isFirstPerson() && RangeFinderItem.isUsing(client.player)) {
         renderRangeFinder(guiGraphics, client);
      }
   }

   @Inject(
      method = {"renderSpyglassOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void renderCameraOverlaysRangeFinder(GuiGraphics guiGraphics, float scopeScale, CallbackInfo ci) {
      Minecraft client = Minecraft.getInstance();
      if (RangeFinderItem.isUsing(client.player)) {
         ci.cancel();
      }
   }

   @Unique
   private static void renderFlash(GuiGraphics context) {
      float t = Math.clamp((float)(System.currentTimeMillis() - ModernIndustrializationExplosivity.flashTimestamp) / 3000.0F, 0.0F, 1.0F);
      float alpha = 1.0F - t * t;
      if (alpha > 0.0F) {
         Color color = new Color(1.0F, 1.0F, 1.0F, alpha);
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         context.fill(0, 0, context.guiWidth(), context.guiHeight(), color.hashCode());
         RenderSystem.depthMask(true);
         RenderSystem.enableDepthTest();
      }
   }

   @Unique
   private static void renderRadiation(GuiGraphics context, Minecraft client) {
      long duration = ModernIndustrializationExplosivity.radiationDuration;
      float progress = client.level == null || ModernIndustrializationExplosivity.radiationStartTime < 0L
         ? 1.0F
         : Math.clamp((float)(client.level.getDayTime() - ModernIndustrializationExplosivity.radiationStartTime) / (float)duration, 0.0F, 1.0F);
      float targetAlpha = 0.0F;
      if (ModernIndustrializationExplosivity.radiationStartTime >= 0L && progress < 1.0F && client.player != null) {
         double distance = client.player.position().distanceTo(new Vec3(ModernIndustrializationExplosivity.radiationX, ModernIndustrializationExplosivity.radiationY, ModernIndustrializationExplosivity.radiationZ));
         float rangeFade = 1.0F - Math.clamp((float)(distance - (ModernIndustrializationExplosivity.radiationRadius - 24.0)) / 24.0F, 0.0F, 1.0F);
         targetAlpha = rangeFade * (1.0F - progress) * 0.28F;
      }

      radiationAlpha += (targetAlpha - radiationAlpha) * 0.08F;
      if (radiationAlpha > 0.001F) {
         Color color = new Color(0.72F, 1.0F, 0.18F, radiationAlpha);
         context.fill(0, 0, context.guiWidth(), context.guiHeight(), color.hashCode());
      }
   }

   @Unique
   private static void renderRangeFinder(GuiGraphics context, Minecraft client) {
      widest = 0;
      lines = 0;
      double distance = RangeFinderItem.getDistance(client.player, client.level);
      double rawDamage = (double)EntityNukeExplosion.getDamage(distance, 200.0, 400.0);
      double damage = DummyEnt.calculateDamage(rawDamage, client.player, client.level);
      boolean survivable = (double)client.player.getHealth() - damage > 0.0;
      if (distance > 499.0) {
         drawLine(context, client, Component.literal("Too far"), 0, ChatFormatting.RED);
      } else {
         drawLine(context, client, Component.literal("Distance:"), 0);
         drawLine(context, client, Component.literal(String.valueOf(Math.round(distance))), 1);
         drawLine(context, client, Component.literal("Raw Damage:"), 2);
         drawLine(context, client, Component.literal(String.valueOf(Math.round(rawDamage))), 3);
         drawLine(context, client, Component.literal("Damage:"), 4);
         drawLine(context, client, Component.literal(String.valueOf(Math.round(damage))), 5);
         drawLine(context, client, Component.literal("Survivable:"), 6);
         drawLine(context, client, Component.literal(survivable ? "Yes" : "No"), 7, survivable ? ChatFormatting.GREEN : ChatFormatting.RED);
      }

      context.fill(0, 0, widest + 4, (9 + 2) * lines + 1, new Color(1, 1, 1, 100).hashCode());
      context.renderOutline(0, 0, widest + 4, (9 + 2) * lines + 1, new Color(255, 255, 255, 100).hashCode());
   }

   @Unique
   private static void drawLine(GuiGraphics context, Minecraft client, Component text, int i) {
      drawLine(context, client, text, i, ChatFormatting.WHITE);
   }

   @Unique
   private static void drawLine(GuiGraphics context, Minecraft client, Component text, int i, ChatFormatting color) {
      int spacing = 2 + 9;
      widest = Math.max(widest, client.font.width(text.getString()));
      lines = Math.max(lines, i + 1);
      context.drawString(client.font, text, 2, spacing * i + 2, color.getColor());
   }
}
