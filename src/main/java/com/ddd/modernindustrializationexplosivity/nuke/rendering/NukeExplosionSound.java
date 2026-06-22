package com.ddd.modernindustrializationexplosivity.nuke.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import com.ddd.modernindustrializationexplosivity.nuke.NukeSounds;
import com.ddd.modernindustrializationexplosivity.nuke.entity.EntityNukeTorex;

/** One-shot blast audio that may begin when a client first enters an active nuke's range. */
public final class NukeExplosionSound extends AbstractTickableSoundInstance {
   private final EntityNukeTorex nuke;

   public NukeExplosionSound(EntityNukeTorex nuke) {
      super(NukeSounds.NUCLEAR_EXPLOSION.get(), SoundSource.BLOCKS, RandomSource.create());
      this.nuke = nuke;
      this.attenuation = Attenuation.NONE;
      this.volume = 1.0F;
      this.x = nuke.getX();
      this.y = nuke.getY();
      this.z = nuke.getZ();
   }

   @Override
   public void tick() {
      this.x = this.nuke.getX();
      this.y = this.nuke.getY();
      this.z = this.nuke.getZ();
      if (Minecraft.getInstance().player != null) {
         double distance = Minecraft.getInstance().player.position().distanceTo(this.nuke.position());
         this.volume = Math.clamp((float)(1.0 - distance / (double)this.nuke.getRadiationRadius()), 0.0F, 1.0F);
      }
   }
}
