package net.aichichikuwa.appliedhistory.sort;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.AEKey;
import net.aichichikuwa.appliedhistory.block.MELoggerBlockEntity;
import net.aichichikuwa.appliedhistory.config.ServerConfig;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public final class ServerRecentAccessTracker {
    private ServerRecentAccessTracker() {
    }

    // history is keyed by the logger block's stable uuid, not by the volatile grid topology
    private static final HashMap<String, HashMap<AEKey, Long>> historyByKey = new HashMap<>();
    private static final HashMap<String, HashMap<AEKey, RecentInteractionInfo>> detailsByKey = new HashMap<>();
    private static final HashMap<String, Long> sequenceByKey = new HashMap<>();
    private static final HashMap<String, Boolean> historyPinEnabledByKey = new HashMap<>();
    private static final HashSet<String> loadedKeys = new HashSet<>();

    private record Target(ServerLevel level, String key) {
    }

    public static void markInteraction(IGridNode gridNode, AEKey key, String playerName, RecentInteractionAction action) {
        var target = target(gridNode);
        if (target == null) {
            return;
        }
        ensureLoaded(target);
        long nextSequence = sequenceByKey.getOrDefault(target.key, 0L) + 1L;
        sequenceByKey.put(target.key, nextSequence);
        historyByKey.computeIfAbsent(target.key, ignored -> new HashMap<>()).put(key, nextSequence);
        detailsByKey.computeIfAbsent(target.key, ignored -> new HashMap<>()).put(
                key,
                new RecentInteractionInfo(nextSequence, System.currentTimeMillis(), playerName, action)
        );
        pruneOldest(target.key);
        persist(target);
    }

    public static HashMap<AEKey, Long> snapshotHistory(IGridNode gridNode) {
        var target = target(gridNode);
        if (target == null) {
            return new HashMap<>();
        }
        ensureLoaded(target);
        if (pruneOldest(target.key)) {
            persist(target);
        }
        var history = historyByKey.get(target.key);
        return history == null ? new HashMap<>() : new HashMap<>(history);
    }

    public static HashMap<AEKey, RecentInteractionInfo> snapshotDetails(IGridNode gridNode) {
        var target = target(gridNode);
        if (target == null) {
            return new HashMap<>();
        }
        ensureLoaded(target);
        if (pruneOldest(target.key)) {
            persist(target);
        }
        var details = detailsByKey.get(target.key);
        return details == null ? new HashMap<>() : new HashMap<>(details);
    }

    public static boolean isHistoryPinEnabled(IGridNode gridNode) {
        var target = target(gridNode);
        if (target == null) {
            return false;
        }
        ensureLoaded(target);
        if (pruneOldest(target.key)) {
            persist(target);
        }
        return historyPinEnabledByKey.getOrDefault(target.key, false);
    }

    public static void setHistoryPinEnabled(IGridNode gridNode, boolean enabled) {
        var target = target(gridNode);
        if (target == null) {
            return;
        }
        ensureLoaded(target);
        historyPinEnabledByKey.put(target.key, enabled);
        persist(target);
    }

    // true only when a single, active me logger owns the terminal's network
    public static boolean hasLogger(IGridNode gridNode) {
        return resolveLogger(gridNode) != null;
    }

    // true when the terminal's network holds more than one me logger
    public static boolean hasConflict(IGridNode gridNode) {
        if (gridNode == null) {
            return false;
        }
        var grid = gridNode.getGrid();
        if (grid == null) {
            return false;
        }
        return countLoggers(grid) > 1;
    }

    // number of remembered entries stored for a specific logger, used by the logger gui
    public static int getEntryCount(MELoggerBlockEntity logger) {
        if (logger == null || logger.getHistoryId() == null) {
            return 0;
        }
        if (!(logger.getLevel() instanceof ServerLevel level)) {
            return 0;
        }
        var target = new Target(level, logger.getHistoryId().toString());
        ensureLoaded(target);
        var details = detailsByKey.get(target.key);
        return details == null ? 0 : details.size();
    }

    public static int maxRememberedItems() {
        return maxHistoryRows() * 9 + 27;
    }

    // erases everything stored for this logger's uuid, both in memory and on disk
    public static void purge(MELoggerBlockEntity logger) {
        if (logger == null || logger.getHistoryId() == null) {
            return;
        }
        if (!(logger.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var key = logger.getHistoryId().toString();
        historyByKey.remove(key);
        detailsByKey.remove(key);
        sequenceByKey.remove(key);
        historyPinEnabledByKey.remove(key);
        loadedKeys.remove(key);
        RecentAccessSavedData.get(level).remove(key);
    }

    private static void ensureLoaded(Target target) {
        if (loadedKeys.contains(target.key)) {
            return;
        }
        var data = RecentAccessSavedData.get(target.level);
        var loadedDetails = data.getDetails(target.key);
        var loadedHistory = new HashMap<AEKey, Long>();
        for (var entry : loadedDetails.entrySet()) {
            loadedHistory.put(entry.getKey(), entry.getValue().sequence());
        }

        historyByKey.put(target.key, loadedHistory);
        detailsByKey.put(target.key, loadedDetails);
        sequenceByKey.put(target.key, Math.max(data.getSequence(target.key), maxSequence(loadedDetails)));
        historyPinEnabledByKey.put(target.key, data.isHistoryPinEnabled(target.key));
        loadedKeys.add(target.key);
    }

    private static void persist(Target target) {
        var data = RecentAccessSavedData.get(target.level);
        data.put(
                target.key,
                sequenceByKey.getOrDefault(target.key, 0L),
                detailsByKey.getOrDefault(target.key, new HashMap<>())
        );
        data.putHistoryPinEnabled(target.key, historyPinEnabledByKey.getOrDefault(target.key, false));
    }

    private static long maxSequence(Map<AEKey, RecentInteractionInfo> details) {
        long max = 0L;
        for (var info : details.values()) {
            max = Math.max(max, info.sequence());
        }
        return max;
    }

    private static int maxHistoryRows() {
        return Math.max(1, ServerConfig.historyRows.get());
    }

    private static boolean pruneOldest(String key) {
        var details = detailsByKey.get(key);
        var history = historyByKey.get(key);
        if (details == null || history == null) {
            return false;
        }
        boolean changed = false;
        while (details.size() > maxRememberedItems()) {
            AEKey oldestKey = null;
            long oldestSequence = Long.MAX_VALUE;
            for (var entry : details.entrySet()) {
                long seq = entry.getValue().sequence();
                if (seq < oldestSequence) {
                    oldestSequence = seq;
                    oldestKey = entry.getKey();
                }
            }
            if (oldestKey == null) {
                break;
            }
            details.remove(oldestKey);
            history.remove(oldestKey);
            changed = true;
        }
        return changed;
    }

    private static Target target(IGridNode gridNode) {
        var logger = resolveLogger(gridNode);
        if (logger == null || logger.getHistoryId() == null) {
            return null;
        }
        if (!(logger.getLevel() instanceof ServerLevel level)) {
            return null;
        }
        return new Target(level, logger.getHistoryId().toString());
    }

    // true when this logger shares its network with at least one other logger
    public static boolean isLoggerConflicted(MELoggerBlockEntity logger) {
        if (logger == null) {
            return false;
        }
        var node = logger.getGridNode();
        if (node == null) {
            return false;
        }
        var grid = node.getGrid();
        if (grid == null) {
            return false;
        }
        return countLoggers(grid) > 1;
    }

    // find the single, active logger that owns history for this network.
    // more than one logger is treated as a conflict: history behaves as if none exists.
    private static MELoggerBlockEntity resolveLogger(IGridNode gridNode) {
        if (gridNode == null) {
            return null;
        }
        var grid = gridNode.getGrid();
        if (grid == null) {
            return null;
        }
        MELoggerBlockEntity only = null;
        int count = 0;
        for (var logger : grid.getMachines(MELoggerBlockEntity.class)) {
            if (logger.getHistoryId() == null) {
                continue;
            }
            if (!(logger.getLevel() instanceof ServerLevel)) {
                continue;
            }
            only = logger;
            count++;
            if (count > 1) {
                return null;
            }
        }
        if (count != 1) {
            return null;
        }
        // the logger needs power and a channel to actually record history
        if (!only.getMainNode().isActive()) {
            return null;
        }
        return only;
    }

    private static int countLoggers(IGrid grid) {
        int count = 0;
        for (var logger : grid.getMachines(MELoggerBlockEntity.class)) {
            if (logger.getHistoryId() != null && logger.getLevel() instanceof ServerLevel) {
                count++;
            }
        }
        return count;
    }

    public static int countLoadedKeys() {
        return loadedKeys.size();
    }
}
