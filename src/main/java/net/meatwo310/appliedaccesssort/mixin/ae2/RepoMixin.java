package net.meatwo310.appliedaccesssort.mixin.ae2;

import appeng.api.stacks.AEKey;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.common.Repo;
import appeng.client.gui.widgets.IScrollSource;
import appeng.client.gui.widgets.ISortSource;
import appeng.menu.me.common.GridInventoryEntry;
import net.meatwo310.appliedaccesssort.client.AppliedHistoryScrollbar;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;

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
    private int recentPrioritySlotCount;
    @Unique
    private int recentPriorityPaddingSlots;

    @Shadow
    public abstract boolean isPaused();

    @Inject(method = "updateView", at = @At("TAIL"))
    private void applyRecentHistoryOrdering(CallbackInfo ci) {
        if (!(sortSrc instanceof MEStorageScreen<?> screen)) {
            return;
        }
        try {
            var containerId = screen.getMenu().containerId;
            ClientRecentAccessState.setAutoCraftPinnedRowPresent(containerId, !pinnedRow.isEmpty());

            if (ClientRecentAccessState.isHistoryReorderFrozen(containerId)) {
                return;
            }

            if (!ClientRecentAccessState.isRecentPinEnabled(containerId) || isPaused()) {
                ClientRecentAccessState.setRecentPinnedKeys(containerId, new HashSet<>());
                ClientRecentAccessState.setPinnedRowCount(containerId, 0);
                ClientRecentAccessState.setRecentPrioritySlotCount(containerId, 0);
                recentPrioritySlotCount = 0;
                recentPriorityPaddingSlots = 0;
                return;
            }

            int maxPrioritySlots = rowSize * ClientRecentAccessState.getMaxHistoryRows(containerId);
            if (maxPrioritySlots <= 0 || view.isEmpty()) {
                ClientRecentAccessState.setRecentPinnedKeys(containerId, new HashSet<>());
                ClientRecentAccessState.setPinnedRowCount(containerId, 0);
                ClientRecentAccessState.setRecentPrioritySlotCount(containerId, 0);
                recentPrioritySlotCount = 0;
                recentPriorityPaddingSlots = 0;
                return;
            }

            var snapshot = new ArrayList<>(view);
            var withHistory = new ArrayList<GridInventoryEntry>();
            for (var entry : snapshot) {
                if (ClientRecentAccessState.getSequence(containerId, entry.getWhat()) > 0L) {
                    withHistory.add(entry);
                }
            }
            if (withHistory.isEmpty()) {
                ClientRecentAccessState.setRecentPinnedKeys(containerId, new HashSet<>());
                ClientRecentAccessState.setPinnedRowCount(containerId, 0);
                ClientRecentAccessState.setRecentPrioritySlotCount(containerId, 0);
                recentPrioritySlotCount = 0;
                recentPriorityPaddingSlots = 0;
                return;
            }
            withHistory.sort((left, right) -> Long.compare(
                    ClientRecentAccessState.getSequence(containerId, right.getWhat()),
                    ClientRecentAccessState.getSequence(containerId, left.getWhat())));

            int headCount = Math.min(maxPrioritySlots, withHistory.size());
            var head = new ArrayList<>(withHistory.subList(0, headCount));
            var headIdentity = Collections.newSetFromMap(new IdentityHashMap<GridInventoryEntry, Boolean>());
            headIdentity.addAll(head);

            var tail = new ArrayList<GridInventoryEntry>();
            for (var entry : snapshot) {
                if (!headIdentity.contains(entry)) {
                    tail.add(entry);
                }
            }
            view.clear();
            view.addAll(head);
            view.addAll(tail);

            var keys = new HashSet<AEKey>();
            for (var entry : head) {
                keys.add(entry.getWhat());
            }
            ClientRecentAccessState.setRecentPinnedKeys(containerId, keys);
            ClientRecentAccessState.setPinnedRowCount(containerId, (headCount + rowSize - 1) / rowSize);
            ClientRecentAccessState.setRecentPrioritySlotCount(containerId, headCount);
            recentPrioritySlotCount = headCount;
            recentPriorityPaddingSlots = ((headCount + rowSize - 1) / rowSize) * rowSize - headCount;
        } finally {
            AppliedHistoryScrollbar.afterRepoViewUpdate(screen);
        }
    }

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void getWithRecentPriorityGap(int idx, CallbackInfoReturnable<GridInventoryEntry> cir) {
        if (!(sortSrc instanceof MEStorageScreen<?> screen)) {
            return;
        }
        int containerId = screen.getMenu().containerId;
        if (!ClientRecentAccessState.isRecentPinEnabled(containerId) || recentPriorityPaddingSlots <= 0) {
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

        localIndex += src.getCurrentScroll() * rowSize;
        int roundedPrioritySlots = recentPrioritySlotCount + recentPriorityPaddingSlots;
        if (localIndex < roundedPrioritySlots) {
            if (localIndex < recentPrioritySlotCount) {
                cir.setReturnValue(view.get(localIndex));
            } else {
                cir.setReturnValue(null);
            }
            return;
        }

        int mapped = localIndex - recentPriorityPaddingSlots;
        if (mapped >= view.size()) {
            cir.setReturnValue(null);
            return;
        }
        cir.setReturnValue(view.get(mapped));
    }

    @Inject(method = "size", at = @At("HEAD"), cancellable = true)
    private void sizeWithRecentPriorityGap(CallbackInfoReturnable<Integer> cir) {
        if (!(sortSrc instanceof MEStorageScreen<?> screen)) {
            return;
        }
        int containerId = screen.getMenu().containerId;
        if (!ClientRecentAccessState.isRecentPinEnabled(containerId) || recentPriorityPaddingSlots <= 0) {
            return;
        }
        cir.setReturnValue(view.size() + pinnedRow.size() + recentPriorityPaddingSlots);
    }
}
