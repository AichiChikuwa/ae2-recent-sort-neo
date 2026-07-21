package net.aichichikuwa.appliedhistory.client;

import appeng.client.gui.me.common.MEStorageScreen;
import net.aichichikuwa.appliedhistory.mixin.ae2.MEStorageScreenAccessor;
import net.aichichikuwa.appliedhistory.mixin.ae2.RepoViewAccessor;
import net.aichichikuwa.appliedhistory.sort.ClientRecentAccessState;

public final class AppliedHistoryScrollbar {
    private AppliedHistoryScrollbar() {
    }

    public static void afterRepoViewUpdate(MEStorageScreen<?> screen) {
        var acc = (MEStorageScreenAccessor) screen;
        int containerId = screen.getMenu().containerId;
        int fixedRows = ClientRecentAccessState.hasAutoCraftPinnedRow(containerId) ? 1 : 0;
        var style = acc.getTerminalStyle();
        int rowSize = style.getSlotsPerRow();
        var repo = acc.getRepo();
        int viewSize = ((RepoViewAccessor) (Object) repo).getScrollableView().size();
        int paddingSlots = ClientRecentAccessState.getRecentPriorityPaddingSlots(containerId, rowSize);
        int scrollableRows = (viewSize + paddingSlots + rowSize - 1) / rowSize;
        int totalContentRows = fixedRows + scrollableRows;
        int terminalRows = acc.getTerminalGridRows();
        int maxScroll = Math.max(0, totalContentRows - terminalRows);
        acc.getScrollbar().setRange(0, maxScroll, Math.max(1, terminalRows / 6));
    }
}
