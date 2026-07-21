package net.aichichikuwa.appliedhistory.block;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNodeListener;
import appeng.api.orientation.BlockOrientation;
import appeng.api.orientation.RelativeSide;
import appeng.blockentity.ServerTickingBlockEntity;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.util.SettingsFrom;
import net.aichichikuwa.appliedhistory.AHRegistry;
import net.aichichikuwa.appliedhistory.config.ServerConfig;
import net.aichichikuwa.appliedhistory.item.MELoggerItems;
import net.aichichikuwa.appliedhistory.sort.ServerRecentAccessTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

// grid-connected block entity that owns the per-network history identity.
// history is stored server-side keyed by this uuid rather than by the volatile grid topology.
public class MELoggerBlockEntity extends AENetworkedBlockEntity implements ServerTickingBlockEntity {
    // fallback used before the server config is read in onReady
    private static final double DEFAULT_ENERGY_PER_TICK = 10.0;
    // how often the block re-checks its own status texture, in ticks
    private static final int STATUS_TICK_INTERVAL = 10;
    // integrity check interval: 5 seconds
    private static final int INTEGRITY_TICK_INTERVAL = 100;

    @Nullable
    private UUID historyId;
    private boolean lunaticOrigin;
    // client-synced; only refreshed when the ae node state changes
    private boolean underCable;
    private boolean tearingDown;
    private int statusTick;
    private int integrityTick;

    public MELoggerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // the logger always occupies a channel and draws power to function
        getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(DEFAULT_ENERGY_PER_TICK);
        onGridConnectableSidesChanged();
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        // bottom segment accepts cables from every side, including underneath
        return orientation.getSides(EnumSet.allOf(RelativeSide.class));
    }

    public boolean isBlank() {
        return historyId == null;
    }

    public boolean isLunaticOrigin() {
        return lunaticOrigin;
    }

    public void setLunaticOrigin(boolean lunaticOrigin) {
        if (this.lunaticOrigin != lunaticOrigin) {
            this.lunaticOrigin = lunaticOrigin;
            setChanged();
        }
    }

    public boolean hasUnderCable() {
        return underCable;
    }

    @Nullable
    public UUID getHistoryId() {
        return historyId;
    }

    public void setHistoryId(@Nullable UUID id) {
        if (id != null && !id.equals(this.historyId)) {
            this.historyId = id;
            setChanged();
            markForUpdate();
        } else if (id == null && this.historyId != null) {
            this.historyId = null;
            setChanged();
            markForUpdate();
        }
    }

    @Override
    public void onReady() {
        super.onReady();
        if (!isClientSide()) {
            getMainNode().setIdlePowerUsage(ServerConfig.meLoggerEnergyPerTick.get());
            refreshUnderCable();
            markForUpdate();
        }
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        // power / channel / boot / neighbor cable changes flip textures and under-cable visual
        if (!isClientSide()) {
            refreshUnderCable();
        }
        markForUpdate();
    }

    public void refreshUnderCable() {
        if (level == null || isClientSide()) {
            return;
        }
        boolean found = level.getBlockEntity(getBlockPos().below()) instanceof CableBusBlockEntity;
        if (found != this.underCable) {
            this.underCable = found;
            setChanged();
            markForUpdate();
        }
    }

    @Override
    public void serverTick() {
        if (!isClientSide()) {
            if (++integrityTick >= INTEGRITY_TICK_INTERVAL) {
                integrityTick = 0;
                if (!MELoggerMultiblock.isStructureIntact(level, getBlockPos())) {
                    purgeHistoryWithLightning();
                    return;
                }
            }
        }

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
        if (isBlank()) {
            return MELoggerStatus.ERROR;
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

    public ItemStack createDismantleDrop() {
        if (historyId != null) {
            return MELoggerItems.createReadyLogger(historyId, lunaticOrigin);
        }
        // blank placed logger recovers as a dormant item
        return MELoggerItems.createDormantLogger(lunaticOrigin);
    }

    public boolean isTearingDown() {
        return tearingDown;
    }

    // used when bounding segments are destroyed by automation (annihilation plane, etc.)
    public void dropAndRemoveStructure() {
        dropAndRemoveStructure(true);
    }

    // player harvest already put the item in inventory; only remove the blocks
    public void dropAndRemoveStructureWithoutWorldDrop() {
        dropAndRemoveStructure(false);
    }

    private void dropAndRemoveStructure(boolean spawnWorldDrop) {
        if (tearingDown || level == null || level.isClientSide()) {
            return;
        }
        tearingDown = true;
        try {
            var pos = getBlockPos().immutable();
            // keep saved history for this uuid; only the block is removed
            var drop = createDismantleDrop();
            this.historyId = null;
            MELoggerMultiblock.removeBoundingBlocks(level, pos);
            if (level.getBlockState(pos).getBlock() instanceof MELoggerBlock) {
                level.removeBlock(pos, false);
            }
            if (spawnWorldDrop) {
                spawnDrop(level, pos, drop);
            }
        } finally {
            tearingDown = false;
        }
    }

    public void purgeHistory() {
        purgeHistory(false);
    }

    private void purgeHistoryWithLightning() {
        purgeHistory(true);
    }

    // wipes this logger's stored history and removes the block, dropping a dormant logger item
    private void purgeHistory(boolean withLightning) {
        if (level == null || level.isClientSide()) {
            return;
        }
        var pos = getBlockPos().immutable();
        if (withLightning) {
            // visual-only so the dormant drop is not destroyed by the strike
            summonLightning((ServerLevel) level, pos);
        }
        ServerRecentAccessTracker.purge(this);
        var dormant = MELoggerItems.createDormantLogger(lunaticOrigin);
        this.historyId = null;
        MELoggerMultiblock.removeBoundingBlocks(level, pos);
        level.removeBlock(pos, false);
        spawnDrop(level, pos, dormant);
    }

    private static void summonLightning(ServerLevel level, BlockPos pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    private static void spawnDrop(Level level, BlockPos pos, ItemStack stack) {
        var entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    @Override
    public void exportSettings(SettingsFrom mode, DataComponentMap.Builder builder, @Nullable Player player) {
        super.exportSettings(mode, builder, player);
        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            if (this.historyId != null) {
                builder.set(AHRegistry.HISTORY_ID.get(), this.historyId);
            }
            if (this.lunaticOrigin) {
                builder.set(AHRegistry.LUNATIC_ORIGIN.get(), true);
            }
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
            setLunaticOrigin(Boolean.TRUE.equals(input.get(AHRegistry.LUNATIC_ORIGIN.get())));
        }
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        if (data.hasUUID("historyId")) {
            this.historyId = data.getUUID("historyId");
        }
        if (data.contains("lunaticOrigin")) {
            this.lunaticOrigin = data.getBoolean("lunaticOrigin");
        }
        if (data.contains("underCable")) {
            this.underCable = data.getBoolean("underCable");
        }
    }

    @Override
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        if (this.historyId != null) {
            data.putUUID("historyId", this.historyId);
        }
        if (this.lunaticOrigin) {
            data.putBoolean("lunaticOrigin", true);
        }
        if (this.underCable) {
            data.putBoolean("underCable", true);
        }
    }

    // ae syncs client visuals through #upd stream; extra nbt keys break the size==1 update path
    @Override
    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(this.underCable);
    }

    @Override
    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean redraw = super.readFromStream(data);
        boolean newUnderCable = data.readBoolean();
        if (newUnderCable != this.underCable) {
            this.underCable = newUnderCable;
            redraw = true;
        }
        return redraw;
    }

    @Override
    protected void saveVisualState(CompoundTag data) {
        super.saveVisualState(data);
        if (this.underCable) {
            data.putBoolean("underCable", true);
        }
    }

    @Override
    protected void loadVisualState(CompoundTag data) {
        super.loadVisualState(data);
        this.underCable = data.getBoolean("underCable");
    }
}
