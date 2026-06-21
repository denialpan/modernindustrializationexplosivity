package com.ddd.modernindustrializationexplosivity.nuke.items;

import java.util.Stack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import com.ddd.modernindustrializationexplosivity.ModernIndustrializationExplosivity;

public class DummyEnt {
   private Level level;
   private Player player;
   private Stack<DamageContainer> damageContainers;
   public float lastDamage = 0.0F;

   public static double calculateDamage(double damage, Player player, Level level) {
      DummyEnt ent = new DummyEnt(level, player);
      DamageSource damageSource = level.damageSources().source(ModernIndustrializationExplosivity.NUCLEAR_BLAST);
      ent.hurt(damageSource, (float)damage);
      return (double)ent.lastDamage;
   }

   public DummyEnt(Level level, Player player) {
      this.level = level;
      this.player = player;
      this.damageContainers = new Stack<>();
   }

   public boolean isInvulnerableTo(DamageSource source) {
      return false;
   }

   public boolean isDeadOrDying() {
      return false;
   }

   public boolean hurt(DamageSource source, float amount) {
      if (this.isInvulnerableTo(source)) {
         return false;
      } else if (this.isDeadOrDying()) {
         return false;
      } else {
         amount = Math.max(0.0F, source.type().scaling().getScalingFunction().scaleDamage(source, this.player, amount, this.level.getDifficulty()));
         return amount == 0.0F ? false : this.LEhurt(source, amount);
      }
   }

   public boolean isDamageSourceBlocked(DamageSource damageSource) {
      Entity entity = damageSource.getDirectEntity();
      boolean flag = false;
      if (entity instanceof AbstractArrow abstractarrow && abstractarrow.getPierceLevel() > 0) {
         flag = true;
      }

      if (!damageSource.is(DamageTypeTags.BYPASSES_SHIELD) && this.player.isBlocking() && !flag) {
         Vec3 vec32 = damageSource.getSourcePosition();
         if (vec32 != null) {
            Vec3 vec3 = this.player.calculateViewVector(0.0F, this.player.getYHeadRot());
            Vec3 vec31 = vec32.vectorTo(this.player.position());
            vec31 = new Vec3(vec31.x, 0.0, vec31.z).normalize();
            return vec31.dot(vec3) < 0.0;
         }
      }

      return false;
   }

   public boolean LEhurt(DamageSource source, float amount) {
      if (this.isInvulnerableTo(source)) {
         return false;
      } else {
         this.damageContainers.push(new DamageContainer(source, amount));
         if (CommonHooks.onEntityIncomingDamage(this.player, this.damageContainers.peek())) {
            return false;
         } else {
            amount = this.damageContainers.peek().getNewDamage();
            boolean flag = false;
            float f1 = 0.0F;
            LivingShieldBlockEvent ev;
            if (amount > 0.0F && (ev = CommonHooks.onDamageBlock(this.player, this.damageContainers.peek(), this.isDamageSourceBlocked(source))).getBlocked()) {
               this.damageContainers.peek().setBlockedDamage(ev);
               f1 = ev.getBlockedDamage();
               amount = ev.getDamageContainer().getNewDamage();
               flag = amount <= 0.0F;
            }

            if (source.is(DamageTypeTags.IS_FREEZING) && this.player.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
               amount *= 5.0F;
            }

            if (source.is(DamageTypeTags.DAMAGES_HELMET) && !this.player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
               amount *= 0.75F;
            }

            this.damageContainers.peek().setNewDamage(amount);
            boolean flag1 = true;
            this.actuallyHurt(source, amount);
            amount = this.damageContainers.peek().getNewDamage();
            boolean flag2 = !flag || amount > 0.0F;
            this.damageContainers.pop();
            return flag2;
         }
      }
   }

   private boolean checkTotemDeathProtection(DamageSource damageSource) {
      if (!damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         for (InteractionHand interactionhand : InteractionHand.values()) {
            ItemStack itemstack1 = this.player.getItemInHand(interactionhand);
            if (itemstack1.is(Items.TOTEM_OF_UNDYING)) {
               return true;
            }
         }
      }

      return false;
   }

   protected float getDamageAfterMagicAbsorb(DamageSource damageSource, float damageAmount) {
      if (damageSource.is(DamageTypeTags.BYPASSES_EFFECTS)) {
         return damageAmount;
      } else {
         if (this.player.hasEffect(MobEffects.DAMAGE_RESISTANCE) && !damageSource.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            int i = (this.player.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
            int j = 25 - i;
            float f = damageAmount * (float)j;
            float f1 = damageAmount;
            damageAmount = Math.max(f / 25.0F, 0.0F);
            float f2 = f1 - damageAmount;
            if (f2 > 0.0F && f2 < 3.4028235E37F) {
               this.damageContainers.peek().setReduction(Reduction.MOB_EFFECTS, f2);
            }
         }

         if (damageAmount <= 0.0F) {
            return 0.0F;
         } else if (damageSource.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
            return damageAmount;
         } else {
            float f3;
            if (this.level instanceof ServerLevel serverlevel) {
               f3 = EnchantmentHelper.getDamageProtection(serverlevel, this.player, damageSource);
            } else {
               f3 = 0.0F;
            }

            if (f3 > 0.0F) {
               damageAmount = CombatRules.getDamageAfterMagicAbsorb(damageAmount, f3);
               this.damageContainers.peek().setReduction(Reduction.ENCHANTMENTS, this.damageContainers.peek().getNewDamage() - damageAmount);
            }

            return damageAmount;
         }
      }
   }

   protected float getDamageAfterArmorAbsorb(DamageSource damageSource, float damageAmount) {
      if (!damageSource.is(DamageTypeTags.BYPASSES_ARMOR)) {
         damageAmount = CombatRules.getDamageAfterAbsorb(
            this.player, damageAmount, damageSource, (float)this.player.getArmorValue(), (float)this.player.getAttributeValue(Attributes.ARMOR_TOUGHNESS)
         );
      }

      return damageAmount;
   }

   protected void actuallyHurt(DamageSource damageSource, float damageAmount) {
      if (!this.isInvulnerableTo(damageSource)) {
         this.damageContainers
            .peek()
            .setReduction(
               Reduction.ARMOR,
               this.damageContainers.peek().getNewDamage() - this.getDamageAfterArmorAbsorb(damageSource, this.damageContainers.peek().getNewDamage())
            );
         this.getDamageAfterMagicAbsorb(damageSource, this.damageContainers.peek().getNewDamage());
         float damage = CommonHooks.onLivingDamagePre(this.player, this.damageContainers.peek());
         this.damageContainers.peek().setReduction(Reduction.ABSORPTION, Math.min(this.player.getAbsorptionAmount(), damage));
         float absorbed = Math.min(damage, this.damageContainers.peek().getReduction(Reduction.ABSORPTION));
         float f1 = this.damageContainers.peek().getNewDamage();
         if (f1 != 0.0F) {
            this.lastDamage = f1;
         }
      }
   }
}
