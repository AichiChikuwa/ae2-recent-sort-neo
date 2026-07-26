package net.aichichikuwa.appliedhistory.sort;

import appeng.api.stacks.AEKey;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ClientRecentAccessState {
    private ClientRecentAccessState() {
    }

    private static final Map<Integer, Map<AEKey, Long>> historyByContainer = new HashMap<>();
    private static final Map<Integer, Map<AEKey, RecentInteractionInfo>> detailByContainer = new HashMap<>();
    private static final Map<Integer, Set<AEKey>> recentPinnedKeysByContainer = new HashMap<>();
    private static final Map<Integer, Boolean> recentPinEnabledByContainer = new HashMap<>();
    private static final Map<Integer, Integer> pinnedRowCountByContainer = new HashMap<>();
    private static final Map<Integer, Integer> recentPrioritySlotCountByContainer = new HashMap<>();
    private static final Map<Integer, Integer> maxHistoryRowsByContainer = new HashMap<>();
    private static final Map<Integer, Boolean> autoCraftPinnedRowByContainer = new HashMap<>();
    private static final Map<Integer, Boolean> historyReorderFrozenByContainer = new HashMap<>();
    private static final Map<Integer, Boolean> hasLoggerByContainer = new HashMap<>();
    private static final Map<Integer, Boolean> loggerConflictByContainer = new HashMap<>();
    private static final Set<Integer> dirtyContainers = new HashSet<>();

    public static void replaceHistory(int containerId, Map<AEKey, Long> history) {
        historyByContainer.put(containerId, new HashMap<>(history));
        dirtyContainers.add(containerId);
    }

    public static void replaceDetails(int containerId, Map<AEKey, RecentInteractionInfo> details) {
        detailByContainer.put(containerId, new HashMap<>(details));
        dirtyContainers.add(containerId);
    }

    public static long getSequence(int containerId, AEKey key) {
        var history = historyByContainer.get(containerId);
        if (history == null) {
            return 0L;
        }
        return history.getOrDefault(key, 0L);
    }

    public static void setRecentPinEnabled(int containerId, boolean enabled) {
        recentPinEnabledByContainer.put(containerId, enabled);
        dirtyContainers.add(containerId);
    }

    public static boolean isRecentPinEnabled(int containerId) {
        return recentPinEnabledByContainer.getOrDefault(containerId, false);
    }

    public static void setHasLogger(int containerId, boolean hasLogger) {
        hasLoggerByContainer.put(containerId, hasLogger);
        dirtyContainers.add(containerId);
    }

    public static boolean hasLogger(int containerId) {
        return hasLoggerByContainer.getOrDefault(containerId, false);
    }

    public static void setLoggerConflict(int containerId, boolean conflict) {
        loggerConflictByContainer.put(containerId, conflict);
        dirtyContainers.add(containerId);
    }

    public static boolean hasLoggerConflict(int containerId) {
        return loggerConflictByContainer.getOrDefault(containerId, false);
    }

    public static void setPinnedRowCount(int containerId, int rowCount) {
        pinnedRowCountByContainer.put(containerId, rowCount);
    }

    public static void setMaxHistoryRows(int containerId, int rowCount) {
        maxHistoryRowsByContainer.put(containerId, Math.max(1, rowCount));
        dirtyContainers.add(containerId);
    }

    public static int getMaxHistoryRows(int containerId) {
        return maxHistoryRowsByContainer.getOrDefault(containerId, 5);
    }

    public static void setHistoryReorderFrozen(int containerId, boolean frozen) {
        historyReorderFrozenByContainer.put(containerId, frozen);
        dirtyContainers.add(containerId);
    }

    public static void triggerHistoryReorderFreeze(int containerId) {
        setHistoryReorderFrozen(containerId, true);
    }

    public static boolean isHistoryReorderFrozen(int containerId) {
        return historyReorderFrozenByContainer.getOrDefault(containerId, false);
    }

    public static int getPinnedRowCount(int containerId) {
        return pinnedRowCountByContainer.getOrDefault(containerId, 0);
    }

    public static void setRecentPrioritySlotCount(int containerId, int slotCount) {
        recentPrioritySlotCountByContainer.put(containerId, Math.max(0, slotCount));
    }

    public static int getRecentPrioritySlotCount(int containerId) {
        return recentPrioritySlotCountByContainer.getOrDefault(containerId, 0);
    }

    public static int getRecentPriorityPaddingSlots(int containerId, int rowSize) {
        int slotCount = getRecentPrioritySlotCount(containerId);
        if (rowSize <= 0 || slotCount <= 0) {
            return 0;
        }
        int rounded = ((slotCount + rowSize - 1) / rowSize) * rowSize;
        return rounded - slotCount;
    }

    public static void setAutoCraftPinnedRowPresent(int containerId, boolean present) {
        autoCraftPinnedRowByContainer.put(containerId, present);
    }

    public static boolean hasAutoCraftPinnedRow(int containerId) {
        return autoCraftPinnedRowByContainer.getOrDefault(containerId, false);
    }

    public static void setRecentPinnedKeys(int containerId, Set<AEKey> keys) {
        recentPinnedKeysByContainer.put(containerId, new HashSet<>(keys));
    }

    public static boolean isRecentPinnedKey(int containerId, AEKey key) {
        var pinnedKeys = recentPinnedKeysByContainer.get(containerId);
        return pinnedKeys != null && pinnedKeys.contains(key);
    }

    public static RecentInteractionInfo getDetails(int containerId, AEKey key) {
        var details = detailByContainer.get(containerId);
        if (details == null) {
            return null;
        }
        return details.get(key);
    }

    public static boolean consumeDirty(int containerId) {
        return dirtyContainers.remove(containerId);
    }
}

