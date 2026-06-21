package com.ddd.modernindustrializationexplosivity.nuke.mixin;

import java.util.Set;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import com.ddd.modernindustrializationexplosivity.nuke.NukeEntities;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(
   targets = {"net.minecraft.server.level.ChunkMap$TrackedEntity"}
)
public class TrackedEntityMixin {
   @Final
   @Shadow
   Entity entity;

   @Redirect(
      method = {"updatePlayer"},
      at = @At(
         value = "INVOKE",
         target = "Ljava/lang/Math;min(II)I"
      )
   )
   public int min(int a, int b) {
      return this.entity.getType() == NukeEntities.TOREX.get() ? 999 : Math.min(a, b);
   }

   @Redirect(
      method = {"updatePlayer"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;broadcastToPlayer(Lnet/minecraft/server/level/ServerPlayer;)Z"
      )
   )
   public boolean broadcast(Entity instance, ServerPlayer player) {
      return this.entity.getType() == NukeEntities.TOREX.get() ? true : instance.broadcastToPlayer(player);
   }

   @Redirect(
      method = {"updatePlayer"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/server/level/ChunkMap;isChunkTracked(Lnet/minecraft/server/level/ServerPlayer;II)Z"
      )
   )
   public boolean isTracked(ChunkMap instance, ServerPlayer player, int x, int z) {
      return this.entity.getType() == NukeEntities.TOREX.get() ? true : this.isChunkTracked(player, x, z);
   }

   @Unique
   boolean isChunkTracked(ServerPlayer player, int x, int z) {
      return player.getChunkTrackingView().contains(x, z) && !player.connection.chunkSender.isPending(ChunkPos.asLong(x, z));
   }

   @Redirect(
      method = {"updatePlayer"},
      at = @At(
         value = "INVOKE",
         target = "Ljava/util/Set;remove(Ljava/lang/Object;)Z"
      )
   )
   public boolean remove(Set instance, Object o) {
      return this.entity.getType() == NukeEntities.TOREX.get() ? false : instance.remove(o);
   }
}
