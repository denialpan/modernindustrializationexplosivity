package com.ddd.modernindustrializationexplosivity.nuke.rendering;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import com.ddd.modernindustrializationexplosivity.nuke.NukeSounds;
import com.ddd.modernindustrializationexplosivity.nuke.entity.EntityNukeCountdown;

/** A siren is bound to one countdown entity, allowing overlapping detonations to stop independently. */
public final class NukeCountdownSirenSound extends AbstractTickableSoundInstance {
   private final EntityNukeCountdown countdown;

   public NukeCountdownSirenSound(EntityNukeCountdown countdown) {
      super(NukeSounds.AIR_SIREN.get(), SoundSource.BLOCKS, RandomSource.create());
      this.countdown = countdown;
      this.looping = true;
      this.delay = 0;
      this.attenuation = Attenuation.NONE;
      this.volume = 1.0F;
      this.x = countdown.getX();
      this.y = countdown.getY();
      this.z = countdown.getZ();
   }

   @Override
   public void tick() {
      if (this.countdown.isRemoved()) {
         this.stop();
         return;
      }
      this.x = this.countdown.getX();
      this.y = this.countdown.getY();
      this.z = this.countdown.getZ();
      if (Minecraft.getInstance().player != null) {
         double distance = Minecraft.getInstance().player.position().distanceTo(this.countdown.position());
         this.volume = Math.clamp((float)(1.0 - distance / (double)this.countdown.getSirenRange()), 0.0F, 1.0F);
      }
   }

   public void stopSiren() {
      this.stop();
   }
}
