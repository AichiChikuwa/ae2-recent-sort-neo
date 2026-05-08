package net.meatwo310.appliedaccesssort.mixin.ae2;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.common.Repo;
import appeng.client.gui.widgets.IScrollSource;
import appeng.client.gui.widgets.ISortSource;
import appeng.menu.me.common.GridInventoryEntry;
import net.meatwo310.appliedaccesssort.sort.ClientRecentAccessState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@Mixin(value = Repo.class, remap = false)
public abstract class RepoMixin {
    @Shadow
    @Final
    private ISortSource sortSrc;
    @Shadow
    @Final
    private IScrollSource src;
    @Shadow
    private int rowSize;
    @Shadow
    @Final
    private ArrayList<GridInventoryEntry> view;
    @Shadow
    @Final
    private ArrayList<GridInventoryEntry> pinnedRow;

    @Unique
    private final List<GridInventoryEntry> recentPinnedEntries = new ArrayList<>();

    @Inject(method = "updateView", at = @At("TAIL"))
    private void applyRecentPinRows(CallbackInfo ci) {
        if (!(sortSrc instanceof MEStorageScreen<?> screen)) {
            return;
        }
        var containerId = screen.getMenu().containerId;
        ClientRecentAccessState.setAutoCraftPinnedRowPresent(containerId, !pinnedRow.isEmpty());

        // keep ordering frozen, but refresh entry data from the latest view
        if (ClientRecentAccessState.isHistoryReorderFrozen(containerId)) {
            if (recentPinnedEntries.isEmpty()) {
                return;
            }
            var latestEntriesByKey = new HashMap<appeng.api.stacks.AEKey, GridInventoryEntry>();
            for (var entry : view) {
                latestEntriesByKey.put(entry.getWhat(), entry);
            }
            var refreshedPinnedEntries = new ArrayList<GridInventoryEntry>(recentPinnedEntries.size());
            for (var entry : recentPinnedEntries) {
                var refreshed = latestEntriesByKey.get(entry.getWhat());
                if (refreshed != null) {
                    refreshedPinnedEntries.add(refreshed);
                }
            }
            recentPinnedEntries.clear();
            recentPinnedEntries.addAll(refreshedPinnedEntries);
            view.removeAll(recentPinnedEntries);
            ClientRecentAccessState.setPinnedRowCount(containerId, (recentPinnedEntries.size() + rowSize - 1) / rowSize);
            var pinnedKeys = new HashSet<appeng.api.stacks.AEKey>();
            for (var entry : recentPinnedEntries) {
                pinnedKeys.add(entry.getWhat());
            }
            ClientRecentAccessState.setRecentPinnedKeys(containerId, pinnedKeys);
            return;
        }

        recentPinnedEntries.clear();
        if (!ClientRecentAccessState.isRecentPinEnabled(containerId)) {
            ClientRecentAccessState.setPinnedRowCount(containerId, 0);
            ClientRecentAccessState.setRecentPinnedKeys(containerId, new HashSet<>());
            return;
        }

        int maxPinnedEntries = rowSize * ClientRecentAccessState.getMaxHistoryRows(containerId);
        if (maxPinnedEntries <= 0 || view.isEmpty()) {
            ClientRecentAccessState.setPinnedRowCount(containerId, 0);
            ClientRecentAccessState.setRecentPinnedKeys(containerId, new HashSet<>());
            return;
        }

        var candidates = new ArrayList<GridInventoryEntry>();
        for (var entry : view) {
            if (ClientRecentAccessState.getSequence(containerId, entry.getWhat()) > 0L) {
                candidates.add(entry);
            }
        }
        if (candidates.isEmpty()) {
            ClientRecentAccessState.setPinnedRowCount(containerId, 0);
            ClientRecentAccessState.setRecentPinnedKeys(containerId, new HashSet<>());
            return;
        }

        candidates.sort((left, right) -> Long.compare(
                ClientRecentAccessState.getSequence(containerId, right.getWhat()),
                ClientRecentAccessState.getSequence(containerId, left.getWhat())));

        int pinnedCount = Math.min(maxPinnedEntries, candidates.size());
        for (int i = 0; i < pinnedCount; i++) {
            recentPinnedEntries.add(candidates.get(i));
        }
        view.removeAll(recentPinnedEntries);
        ClientRecentAccessState.setPinnedRowCount(containerId, (recentPinnedEntries.size() + rowSize - 1) / rowSize);
        var pinnedKeys = new HashSet<appeng.api.stacks.AEKey>();
        for (var entry : recentPinnedEntries) {
            pinnedKeys.add(entry.getWhat());
        }
        ClientRecentAccessState.setRecentPinnedKeys(containerId, pinnedKeys);
    }

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void getWithRecentPinnedRows(int idx, CallbackInfoReturnable<GridInventoryEntry> cir) {
        if (!(sortSrc instanceof MEStorageScreen<?> screen)) {
            return;
        }
        int containerId = screen.getMenu().containerId;
        if (!ClientRecentAccessState.isRecentPinEnabled(containerId) || recentPinnedEntries.isEmpty()) {
            return;
        }

        int localIndex = idx;
        if (!pinnedRow.isEmpty()) {
            if (localIndex < rowSize) {
                if (localIndex < pinnedRow.size()) {
                    cir.setReturnValue(pinnedRow.get(localIndex));
                } else {
                    cir.setReturnValue(null);
                }
                return;
            }
            localIndex -= rowSize;
        }

        int recentSlotCount = ((recentPinnedEntries.size() + rowSize - 1) / rowSize) * rowSize;
        if (localIndex < recentSlotCount) {
            if (localIndex < recentPinnedEntries.size()) {
                cir.setReturnValue(recentPinnedEntries.get(localIndex));
            } else {
                cir.setReturnValue(null);
            }
            return;
        }
        localIndex -= recentSlotCount;
        localIndex += src.getCurrentScroll() * rowSize;

        if (localIndex >= view.size()) {
            cir.setReturnValue(null);
            return;
        }
        cir.setReturnValue(view.get(localIndex));
    }

    @Inject(method = "size", at = @At("HEAD"), cancellable = true)
    private void sizeWithRecentPinnedRows(CallbackInfoReturnable<Integer> cir) {
        if (!(sortSrc instanceof MEStorageScreen<?> screen)) {
            return;
        }
        int containerId = screen.getMenu().containerId;
        if (!ClientRecentAccessState.isRecentPinEnabled(containerId) || recentPinnedEntries.isEmpty()) {
            return;
        }
        cir.setReturnValue(view.size() + pinnedRow.size() + recentPinnedEntries.size());
    }

    @Inject(method = "hasPinnedRow", at = @At("HEAD"), cancellable = true)
    private void hasPinnedRowsWithRecent(CallbackInfoReturnable<Boolean> cir) {
        if (!(sortSrc instanceof MEStorageScreen<?> screen)) {
            return;
        }
        int containerId = screen.getMenu().containerId;
        if (!ClientRecentAccessState.isRecentPinEnabled(containerId)) {
            return;
        }
        cir.setReturnValue(!pinnedRow.isEmpty() || !recentPinnedEntries.isEmpty());
    }
}

