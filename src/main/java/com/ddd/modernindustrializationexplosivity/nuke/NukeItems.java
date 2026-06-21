package com.ddd.modernindustrializationexplosivity.nuke;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Items;
import com.ddd.modernindustrializationexplosivity.nuke.items.DetonatorItem;
import com.ddd.modernindustrializationexplosivity.nuke.items.GogglesItem;
import com.ddd.modernindustrializationexplosivity.nuke.items.RangeFinderItem;

public class NukeItems {
   public static final Items ITEMS = DeferredRegister.createItems("modern_industrialization_explosivity");
   public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "modern_industrialization_explosivity");
   public static final DeferredItem<DetonatorItem> DETONATOR = ITEMS.registerItem("detonator", DetonatorItem::new, new Properties().stacksTo(1));
   public static final DeferredItem<GogglesItem> GOGGLES = ITEMS.registerItem("goggles", GogglesItem::new, new Properties().stacksTo(1));
   public static final DeferredItem<RangeFinderItem> RANGEFINDER = ITEMS.registerItem("rangefinder", RangeFinderItem::new, new Properties().stacksTo(1));
   public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_MODE_TABS.register(
      "tab",
      () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.modern_industrialization_explosivity"))
            .withTabsBefore(new ResourceKey[]{CreativeModeTabs.COMBAT})
            .icon(() -> ((DetonatorItem)DETONATOR.get()).getDefaultInstance())
            .displayItems((parameters, output) -> {
               output.accept((ItemLike)DETONATOR.get());
               output.accept((ItemLike)GOGGLES.get());
               output.accept((ItemLike)RANGEFINDER.get());
            })
            .build()
   );

   public static void register(IEventBus modEventBus) {
      ITEMS.register(modEventBus);
      CREATIVE_MODE_TABS.register(modEventBus);
   }
}
