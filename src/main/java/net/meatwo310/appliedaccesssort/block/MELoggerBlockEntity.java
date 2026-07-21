package net.meatwo310.appliedaccesssort.block;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNodeListener;
import appeng.blockentity.ServerTickingBlockEntity;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.util.SettingsFrom;
import net.meatwo310.appliedaccesssort.AHRegistry;
import net.meatwo310.appliedaccesssort.config.ServerConfig;
import net.meatwo310.appliedaccesssort.sort.ServerRecentAccessTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// grid-connected block entity that owns the per-network history identity.
// history is stored server-side keyed by this uuid rather than by the volatile grid topology.
public class MELoggerBlockEntity extends AENetworkedBlockEntity implements ServerTickingBlockEntity {
    // fallback used before the server config is read in onReady
    private static final double DEFAULT_ENERGY_PER_TICK = 10.0;
    // how often the block re-checks its own status texture, in ticks
    private static final int STATUS_TICK_INTERVAL = 10;

    @Nullable
    private UUID historyId;
    private int statusTick;

    public MELoggerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // the logger always occupies a channel and draws power to function
        getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(DEFAULT_ENERGY_PER_TICK);
    }

    @Nullable
    public UUID getHistoryId() {
        return historyId;
    }

    public void setHistoryId(@Nullable UUID id) {
        if (id != null && !id.equals(this.historyId)) {
            this.historyId = id;
            setChanged();
        }
    }

    // assign a fresh identity if this logger does not already carry one
    public void ensureHistoryId() {
        if (this.historyId == null) {
            this.historyId = UUID.randomUUID();
            setChanged();
        }
    }

    @Override
    public void onReady() {
        super.onReady();
        if (!isClientSide()) {
            getMainNode().setIdlePowerUsage(ServerConfig.meLoggerEnergyPerTick.get());
            ensureHistoryId();
            markForUpdate();
        }
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        // power / channel / boot changes flip the status texture
        markForUpdate();
    }

    @Override
    public void serverTick() {
        // conflict state (another logger joining/leaving) is not signalled through the node,
        // so re-check the status texture periodically as a safety net
        if (++statusTick < STATUS_TICK_INTERVAL) {
            return;
        }
        statusTick = 0;
        if (getBlockState().getValue(MELoggerBlock.STATUS) != getStatus()) {
            markForUpdate();
        }
    }

    // resolves the logger's current visual status; server computes it, client reads the synced blockstate
    public MELoggerStatus getStatus() {
        if (level == null || level.isClientSide()) {
            return getBlockState().getValue(MELoggerBlock.STATUS);
        }
        if (ServerRecentAccessTracker.isLoggerConflicted(this)) {
            return MELoggerStatus.ERROR;
        }
        return getMainNode().isActive() ? MELoggerStatus.ON : MELoggerStatus.OFF;
    }

    public int getEntryCount() {
        return ServerRecentAccessTracker.getEntryCount(this);
    }

    public int getMaxEntries() {
        return ServerRecentAccessTracker.maxRememberedItems();
    }

    // wipes this logger's stored history and removes the block, dropping a fresh item with no identity
    public void purgeHistory() {
        if (level == null || level.isClientSide()) {
            return;
        }
        ServerRecentAccessTracker.purge(this);
        // dropping the item after clearing the id yields a plain logger with no stored history
        this.historyId = null;
        level.destroyBlock(getBlockPos(), true);
    }

    @Override
    public void exportSettings(SettingsFrom mode, DataComponentMap.Builder builder, @Nullable Player player) {
        super.exportSettings(mode, builder, player);
        if (mode == SettingsFrom.DISMANTLE_ITEM && this.historyId != null) {
            builder.set(AHRegistry.HISTORY_ID.get(), this.historyId);
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, DataComponentMap input, @Nullable Player player) {
        super.importSettings(mode, input, player);
        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            var id = input.get(AHRegistry.HISTORY_ID.get());
            if (id != null) {
                setHistoryId(id);
            }
        }
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        if (data.hasUUID("historyId")) {
            this.historyId = data.getUUID("historyId");
        }
    }

    @Override
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        if (this.historyId != null) {
            data.putUUID("historyId", this.historyId);
        }
    }
}
