package com.ddd.modernindustrializationexplosivity.nuke.items;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;
import com.ddd.modernindustrializationexplosivity.nuke.NukeArmorMaterials;

public class GogglesItem extends ArmorItem {
   public GogglesItem(Properties settings) {
      super(NukeArmorMaterials.GOGGLES_ARMOR_MATERIAL, Type.HELMET, new Properties().durability(-1));
   }
}
