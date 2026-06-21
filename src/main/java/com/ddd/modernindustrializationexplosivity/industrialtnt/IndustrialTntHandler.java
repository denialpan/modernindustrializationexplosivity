package com.ddd.modernindustrializationexplosivity.industrialtnt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/** Adds TNT behaviour to Modern Industrialization's otherwise generic industrial TNT block. */
public final class IndustrialTntHandler {
   public static final float EXPLOSION_POWER = 24.0F;
   private static final ResourceLocation INDUSTRIAL_TNT = ResourceLocation.fromNamespaceAndPath("modern_industrialization", "industrial_tnt");

   private IndustrialTntHandler() {
   }

   @SubscribeEvent
   public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
      Level level = event.getLevel();
      BlockPos pos = event.getPos();
      ItemStack heldItem = event.getItemStack();
      if (!isIndustrialTnt(level.getBlockState(pos)) || !isIgniter(heldItem)) {
         return;
      }

      if (!level.isClientSide) {
         Player player = event.getEntity();
         if (detonate(level, pos)) {
            consumeIgniter(player, event.getHand(), heldItem);
         }
      }

      event.setCancellationResult(InteractionResult.SUCCESS);
      event.setCanceled(true);
   }

   @SubscribeEvent
   public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
      if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
         return;
      }

      for (Direction direction : event.getNotifiedSides()) {
         BlockPos target = event.getPos().relative(direction);
         if (isIndustrialTnt(level.getBlockState(target)) && level.hasNeighborSignal(target)) {
            detonate(level, target);
         }
      }
   }

   @SubscribeEvent
   public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
      Level level = event.getLevel();
      if (level.isClientSide) {
         return;
      }

      event.getAffectedBlocks().removeIf(pos -> {
         if (!isIndustrialTnt(level.getBlockState(pos))) {
            return false;
         }

         detonate(level, pos);
         return true;
      });
   }

   private static boolean detonate(Level level, BlockPos pos) {
      if (!isIndustrialTnt(level.getBlockState(pos))) {
         return false;
      }

      level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
      level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, EXPLOSION_POWER, Level.ExplosionInteraction.TNT);
      return true;
   }

   private static boolean isIndustrialTnt(BlockState state) {
      return BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(INDUSTRIAL_TNT);
   }

   private static boolean isIgniter(ItemStack stack) {
      return stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE);
   }

   private static void consumeIgniter(Player player, net.minecraft.world.InteractionHand hand, ItemStack stack) {
      if (player.getAbilities().instabuild) {
         return;
      }

      if (stack.is(Items.FLINT_AND_STEEL)) {
         stack.hurtAndBreak(1, player, hand == net.minecraft.world.InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
      } else {
         stack.shrink(1);
      }
   }
}
