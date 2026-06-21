package com.ddd.modernindustrializationexplosivity.nuke;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.DataComponents;
import com.ddd.modernindustrializationexplosivity.nuke.items.SelectedNuke;

public class NukeComponents {
   public static final DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, "modern_industrialization_explosivity");
   public static final Supplier<DataComponentType<SelectedNuke>> SELECTED_NUKE = COMPONENTS.registerComponentType(
      "selected_nuke", builder -> builder.persistent(SelectedNuke.CODEC)
   );

   public static void register(IEventBus modEventBus) {
      COMPONENTS.register(modEventBus);
   }
}
