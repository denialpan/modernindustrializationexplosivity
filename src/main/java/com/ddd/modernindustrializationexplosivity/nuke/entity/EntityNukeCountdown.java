package com.ddd.modernindustrializationexplosivity.nuke.entity;

import aztech.modern_industrialization.MIBlock;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import com.ddd.modernindustrializationexplosivity.ModernIndustrializationExplosivity;
import com.ddd.modernindustrializationexplosivity.nuke.NukeEntities;
import com.ddd.modernindustrializationexplosivity.nuke.NukeComponents;
import com.ddd.modernindustrializationexplosivity.nuke.NukeItems;
import com.ddd.modernindustrializationexplosivity.nuke.items.SelectedNuke;

public class EntityNukeCountdown extends Entity {
    public static final int COUNTDOWN_TICKS = 200;
    private BlockPos target = BlockPos.ZERO;
    private UUID causeId;
    private int strength;

    public EntityNukeCountdown(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public static EntityNukeCountdown create(Level level, BlockPos target, int strength, Entity cause) {
        EntityNukeCountdown countdown = new EntityNukeCountdown(NukeEntities.COUNTDOWN.get(), level);
        countdown.target = target.immutable();
        countdown.strength = strength;
        countdown.causeId = cause == null ? null : cause.getUUID();
        countdown.setPos(target.getCenter());
        return countdown;
    }

    @Override
    protected void defineSynchedData(Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        if (this.level().getBlockState(this.target).getBlock() != MIBlock.NUKE.get()) {
            this.cancelCountdown();
            this.discard();
            return;
        }
        if (this.tickCount > 0 && this.tickCount % 20 == 0) {
            ServerPlayer player = this.getCausePlayer();
            if (player != null) {
                int seconds = (int) Math.ceil((double) (COUNTDOWN_TICKS - this.tickCount) / 20.0);
                player.displayClientMessage(Component.translatable("detonator.countdown", Math.max(seconds, 0)), true);
            }
        }
        if (this.tickCount < COUNTDOWN_TICKS) return;

        this.level().setBlock(this.target, Blocks.AIR.defaultBlockState(), 11);
        Entity cause = this.getCausePlayer();
        ModernIndustrializationExplosivity.nuke(this.strength, this.target.getCenter(), this.level(), cause);
        this.discard();
    }

    private ServerPlayer getCausePlayer() {
        if (this.causeId == null) return null;
        Player player = ((ServerLevel) this.level()).getPlayerByUUID(this.causeId);
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    private void cancelCountdown() {
        ServerPlayer player = this.getCausePlayer();
        if (player == null) return;

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(NukeItems.DETONATOR.get())) continue;
            SelectedNuke selected = stack.get(NukeComponents.SELECTED_NUKE);
            if (selected != null && selected.getBlockPos().equals(this.target)) {
                stack.set(NukeComponents.SELECTED_NUKE, null);
            }
        }
        player.displayClientMessage(Component.translatable("detonator.cancelled"), true);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.target = BlockPos.of(tag.getLong("Target"));
        this.strength = tag.getInt("Strength");
        this.causeId = tag.hasUUID("Cause") ? tag.getUUID("Cause") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("Target", this.target.asLong());
        tag.putInt("Strength", this.strength);
        if (this.causeId != null) tag.putUUID("Cause", this.causeId);
    }
}
