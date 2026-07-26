package net.aichichikuwa.appliedhistory.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class MELoggerBoundingBlockEntity extends BlockEntity {
    private static final int ORPHAN_TICK_INTERVAL = 100;

    @Nullable
    private BlockPos mainPos;
    private int orphanTick;

    public MELoggerBoundingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setMainPos(BlockPos mainPos) {
        this.mainPos = mainPos.immutable();
        setChanged();
    }

    public void clearMainPos() {
        this.mainPos = null;
        setChanged();
    }

    @Nullable
    public BlockPos getMainPos() {
        return mainPos;
    }

    public boolean canRedirectFrom(BlockPos queryPos) {
        return mainPos != null && !mainPos.equals(queryPos);
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (++orphanTick < ORPHAN_TICK_INTERVAL) {
            return;
        }
        orphanTick = 0;
        if (hasLoggerBelow()) {
            return;
        }
        // unlink first so onRemove does not try to tear down a missing/unrelated main
        clearMainPos();
        level.removeBlock(getBlockPos(), false);
    }

    // cheap: blockstate only, no block-entity lookup
    private boolean hasLoggerBelow() {
        var pos = getBlockPos();
        return level.getBlockState(pos.below()).getBlock() instanceof MELoggerBlock
                || level.getBlockState(pos.below(2)).getBlock() instanceof MELoggerBlock;
    }

    @Override
    protected void saveAdditional(ValueOutput data) {
        super.saveAdditional(data);
        if (mainPos != null) {
            data.store("mainPos", BlockPos.CODEC, mainPos);
        }
    }

    @Override
    protected void loadAdditional(ValueInput data) {
        super.loadAdditional(data);
        mainPos = data.read("mainPos", BlockPos.CODEC).orElse(null);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var data = super.getUpdateTag(registries);
        if (mainPos != null) {
            data.store("mainPos", BlockPos.CODEC, mainPos);
        }
        return data;
    }
}
