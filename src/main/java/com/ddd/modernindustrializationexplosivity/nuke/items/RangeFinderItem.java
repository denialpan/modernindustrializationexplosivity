package com.ddd.modernindustrializationexplosivity.nuke.items;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.ddd.modernindustrializationexplosivity.nuke.NukeItems;

public class RangeFinderItem extends Item {
   public static final float RANGE = 500.0F;

   public RangeFinderItem(Properties properties) {
      super(properties);
   }

   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 1200;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
      return ItemUtils.startUsingInstantly(level, player, usedHand);
   }

   public static double getDistance(Player player, Level level) {
      Vec3 start = player.getEyePosition();
      Vec3 end = player.getEyePosition().add(player.getLookAngle().normalize().scale(500.0));
      HitResult result = level.clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, player));
      return player.position().distanceTo(result.getLocation());
   }

   public static boolean isUsing(Player player) {
      return player.isUsingItem() && player.getUseItem().is(NukeItems.RANGEFINDER.asItem());
   }

   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("rangefinder.tooltip").withColor(ChatFormatting.GRAY.getColor()));
      tooltip.add(Component.translatable("rangefinder.tooltip2").withColor(ChatFormatting.GOLD.getColor()));
   }
}
