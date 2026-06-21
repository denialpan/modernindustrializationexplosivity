package com.ddd.modernindustrializationexplosivity.nuke.mixin;

import net.minecraft.world.entity.player.Player;
import com.ddd.modernindustrializationexplosivity.nuke.items.RangeFinderItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Player.class})
public class PlayerMixin {
   @Inject(
      method = {"isScoping"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void isScoping(CallbackInfoReturnable<Boolean> cir) {
      Player player = (Player)(Object)this;
      if (RangeFinderItem.isUsing(player)) {
         cir.setReturnValue(true);
      }
   }
}
