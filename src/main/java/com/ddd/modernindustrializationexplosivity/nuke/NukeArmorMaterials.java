package com.ddd.modernindustrializationexplosivity.nuke;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.ArmorMaterial.Layer;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class NukeArmorMaterials {
   public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(BuiltInRegistries.ARMOR_MATERIAL, "modern_industrialization_explosivity");
   public static final Holder<ArmorMaterial> GOGGLES_ARMOR_MATERIAL = ARMOR_MATERIALS.register(
      "goggles",
      () -> new ArmorMaterial(
            (Map)net.minecraft.Util.make(new EnumMap(Type.class), map -> {
               map.put(Type.BOOTS, 0);
               map.put(Type.LEGGINGS, 0);
               map.put(Type.CHESTPLATE, 0);
               map.put(Type.HELMET, 0);
               map.put(Type.BODY, 0);
            }),
            0,
            SoundEvents.ARMOR_EQUIP_GENERIC,
            () -> Ingredient.of(),
            List.of(new Layer(ResourceLocation.fromNamespaceAndPath("modern_industrialization_explosivity", "goggles"))),
            0.0F,
            0.0F
         )
   );

   public static void register(IEventBus modEventBus) {
      ARMOR_MATERIALS.register(modEventBus);
   }
}
