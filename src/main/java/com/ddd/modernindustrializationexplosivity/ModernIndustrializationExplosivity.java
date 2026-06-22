package com.ddd.modernindustrializationexplosivity;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import com.ddd.modernindustrializationexplosivity.nuke.NukeArmorMaterials;
import com.ddd.modernindustrializationexplosivity.nuke.NukeComponents;
import com.ddd.modernindustrializationexplosivity.nuke.NukeEntities;
import com.ddd.modernindustrializationexplosivity.nuke.NukeItems;
import com.ddd.modernindustrializationexplosivity.nuke.NukeSounds;
import com.ddd.modernindustrializationexplosivity.industrialtnt.IndustrialTntHandler;
import com.ddd.modernindustrializationexplosivity.nuke.entity.EntityNukeExplosion;
import com.ddd.modernindustrializationexplosivity.nuke.entity.EntityNukeTorex;
import com.ddd.modernindustrializationexplosivity.nuke.entity.EntityNukeCountdown;
import com.ddd.modernindustrializationexplosivity.nuke.entity.EntityRadiationZone;
import com.ddd.modernindustrializationexplosivity.nuke.explosion.ExplosionNuke;
import com.ddd.modernindustrializationexplosivity.nuke.rendering.EntityNukeTorexRenderer;
import com.ddd.modernindustrializationexplosivity.nuke.rendering.NukeCountdownSirenSound;
import org.slf4j.Logger;

@Mod("modern_industrialization_explosivity")
public class ModernIndustrializationExplosivity {
   public static final String MODID = "modern_industrialization_explosivity";
   private static final Logger LOGGER = LogUtils.getLogger();
   public static long flashTimestamp = 0L;
   public static long radiationStartTime = -1L;
   public static double radiationX;
   public static double radiationY;
   public static double radiationZ;
   public static double radiationRadius = 200.0;
   public static long radiationDuration = 48000L;
   public static long shakeTimestamp = 0L;
   public static final int FLASH_TIME = 3000;
   public static final ResourceKey<DamageType> NUCLEAR_BLAST = ResourceKey.create(
      Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("modern_industrialization_explosivity", "nuclear_blast")
   );

   public ModernIndustrializationExplosivity(IEventBus modEventBus, ModContainer modContainer) {
      modContainer.registerConfig(ModConfig.Type.SERVER, ExplosivityConfig.SPEC);
      if (FMLEnvironment.dist == Dist.CLIENT) {
         modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
      }
      modEventBus.addListener(this::commonSetup);
      NukeComponents.register(modEventBus);
      NukeArmorMaterials.register(modEventBus);
      NukeEntities.register(modEventBus);
      NukeItems.register(modEventBus);
      NukeSounds.register(modEventBus);
      NeoForge.EVENT_BUS.register(this);
      NeoForge.EVENT_BUS.register(IndustrialTntHandler.class);
   }

   private void commonSetup(FMLCommonSetupEvent event) {
   }

   @SubscribeEvent
   public void onServerStarting(ServerStartingEvent event) {
   }

   public static void nuke(int strength, Vec3 pos, Level world, Entity cause) {
      double extendedEffectRadius = ExplosionNuke.getExtendedEffectRadius(strength);
      int radiationDuration = ExplosivityConfig.RADIATION_DURATION_TICKS.get();
      EntityNukeExplosion explosion = EntityNukeExplosion.statFac(world, strength, pos.x, pos.y, pos.z, cause);
      world.addFreshEntity(explosion);
      EntityNukeTorex torex = new EntityNukeTorex(world);
      torex.setPos(pos.x, pos.y + 0.5, pos.z);
      torex.getEntityData().set(EntityNukeTorex.SCALE, 1.2F);
      torex.setRenderRadius(strength + 64.0F + EntityNukeTorex.MAX_SHOCK_RING_DISTANCE);
      torex.setRadiationRadius((float)extendedEffectRadius);
      torex.setRadiationDuration(radiationDuration);
      world.addFreshEntity(torex);
      world.addFreshEntity(EntityRadiationZone.create(world, pos.x, pos.y, pos.z, extendedEffectRadius, radiationDuration));
   }

   @EventBusSubscriber(
      modid = "modern_industrialization_explosivity",
      value = {Dist.CLIENT}
   )
   public static class ClientForgeEvents {
      private static final Map<java.util.UUID, NukeCountdownSirenSound> COUNTDOWN_SIRENS = new HashMap<>();
      private static final Set<java.util.UUID> STARTED_COUNTDOWN_SIRENS = new HashSet<>();

      @SubscribeEvent
      public static void onRenderWorld(RenderLevelStageEvent event) {
         if (event.getStage() == Stage.AFTER_LEVEL) {
            ;
         }
      }

      @SubscribeEvent
      public static void onClientTick(ClientTickEvent.Post event) {
         EntityNukeTorex.tickDetachedClientClouds();
         syncCountdownSirens();
      }

      private static void syncCountdownSirens() {
         net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
         if (client.level == null) {
            COUNTDOWN_SIRENS.values().forEach(NukeCountdownSirenSound::stopSiren);
            COUNTDOWN_SIRENS.clear();
            STARTED_COUNTDOWN_SIRENS.clear();
            EntityNukeTorexRenderer.clearPlayedSoundCache();
            return;
         }

         Set<java.util.UUID> activeCountdowns = new HashSet<>();
         for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof EntityNukeCountdown countdown && !countdown.isRemoved()) {
               activeCountdowns.add(countdown.getUUID());
               if (STARTED_COUNTDOWN_SIRENS.add(countdown.getUUID())) {
                  NukeCountdownSirenSound siren = new NukeCountdownSirenSound(countdown);
                  client.getSoundManager().play(siren);
                  COUNTDOWN_SIRENS.put(countdown.getUUID(), siren);
               }
            }
         }
         COUNTDOWN_SIRENS.entrySet().removeIf(entry -> {
            if (activeCountdowns.contains(entry.getKey())) {
               return false;
            }
            entry.getValue().stopSiren();
            return true;
         });
      }
   }

   @EventBusSubscriber(
      modid = "modern_industrialization_explosivity",
      bus = Bus.MOD,
      value = {Dist.CLIENT}
   )
   public static class ClientModEvents {
      @SubscribeEvent
      public static void onClientSetup(FMLClientSetupEvent event) {
         EntityRenderers.register(NukeEntities.TOREX.get(), EntityNukeTorexRenderer::new);
         EntityRenderers.register(NukeEntities.NUKE.get(), NoopRenderer::new);
         EntityRenderers.register(NukeEntities.COUNTDOWN.get(), NoopRenderer::new);
         EntityRenderers.register(NukeEntities.RADIATION_ZONE.get(), NoopRenderer::new);
      }

   }
}
