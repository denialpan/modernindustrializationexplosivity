package com.ddd.modernindustrializationexplosivity.nuke.items;

import aztech.modern_industrialization.MIBlock;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import com.ddd.modernindustrializationexplosivity.ModernIndustrializationExplosivity;
import com.ddd.modernindustrializationexplosivity.ExplosivityConfig;
import com.ddd.modernindustrializationexplosivity.nuke.NukeComponents;
import com.ddd.modernindustrializationexplosivity.nuke.entity.EntityNukeCountdown;

public class DetonatorItem extends Item {
   public DetonatorItem(Properties properties) {
      super(properties);
   }

   public InteractionResult useOn(UseOnContext context) {
      Level world = context.getLevel();
      BlockPos pos = context.getClickedPos();
      ItemStack stack = context.getItemInHand();
      Block block = world.getBlockState(pos).getBlock();
      if (!world.isClientSide && block == MIBlock.NUKE.get()) {
         stack.set(NukeComponents.SELECTED_NUKE, new SelectedNuke(pos.getX(), pos.getY(), pos.getZ()));
         if (!world.isClientSide) {
            context.getPlayer().displayClientMessage(Component.translatable("detonator.message"), true);
         }
      }

      return InteractionResult.SUCCESS;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
      if (player.getItemInHand(usedHand).get(NukeComponents.SELECTED_NUKE) == null) {
         return InteractionResultHolder.pass(player.getItemInHand(usedHand));
      } else {
         BlockPos selected = ((SelectedNuke)player.getItemInHand(usedHand).get(NukeComponents.SELECTED_NUKE)).getBlockPos();
         if (!level.isClientSide && level.getBlockState(selected).getBlock() == MIBlock.NUKE.get()) {
            level.addFreshEntity(EntityNukeCountdown.create(level, selected, ExplosivityConfig.NUKE_STRENGTH.get(), player));
            Component countdownMessage = Component.translatable("detonator.countdown", ExplosivityConfig.NUKE_COUNTDOWN_SECONDS.get());
            if (ExplosivityConfig.NUKE_COUNTDOWN_SECONDS.get() <= 5) {
               countdownMessage = countdownMessage.copy().withStyle(ChatFormatting.RED);
            }
            player.displayClientMessage(countdownMessage, true);
            player.getItemInHand(usedHand).set(NukeComponents.SELECTED_NUKE, null);
         }

         return InteractionResultHolder.success(player.getItemInHand(usedHand));
      }
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      if (stack.get(NukeComponents.SELECTED_NUKE) != null) {
         tooltip.add(Component.translatable("detonator.tooltip").withColor(ChatFormatting.GRAY.getColor()));
         BlockPos selected = ((SelectedNuke)stack.get(NukeComponents.SELECTED_NUKE)).getBlockPos();
         tooltip.add(Component.literal(selected.toShortString()).withColor(ChatFormatting.BLUE.getColor()));
      }
   }
}
