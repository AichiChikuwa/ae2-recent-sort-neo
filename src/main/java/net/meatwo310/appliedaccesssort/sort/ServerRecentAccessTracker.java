package net.meatwo310.appliedaccesssort.sort;

import appeng.api.networking.IGridNode;
import appeng.api.stacks.AEKey;
import appeng.parts.AEBasePart;
import net.meatwo310.appliedaccesssort.config.ServerConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

public final class ServerRecentAccessTracker {
    private ServerRecentAccessTracker() {
    }
    private static final HashMap<String, HashMap<AEKey, Long>> historyByNodeKey = new HashMap<>();
    private static final HashMap<String, HashMap<AEKey, RecentInteractionInfo>> detailsByNodeKey = new HashMap<>();
    private static final HashMap<String, Long> sequenceByNodeKey = new HashMap<>();
    private static final HashMap<String, Boolean> historyPinEnabledByNodeKey = new HashMap<>();
    private static final HashMap<String, Boolean> loadedByNodeKey = new HashMap<>();

    public static void markInteraction(IGridNode gridNode, AEKey key, String playerName, RecentInteractionAction action) {
        var nodeKey = getNodeKey(gridNode);
        if (nodeKey == null) {
            return;
        }
        ensureLoaded(gridNode, nodeKey);
        long nextSequence = sequenceByNodeKey.getOrDefault(nodeKey, 0L) + 1L;
        sequenceByNodeKey.put(nodeKey, nextSequence);
        historyByNodeKey.computeIfAbsent(nodeKey, ignored -> new HashMap<>()).put(key, nextSequence);
        detailsByNodeKey.computeIfAbsent(nodeKey, ignored -> new HashMap<>()).put(
                key,
                new RecentInteractionInfo(nextSequence, System.currentTimeMillis(), playerName, action)
        );
        pruneOldest(nodeKey);
        persist(gridNode, nodeKey);
    }

    public static HashMap<AEKey, Long> snapshotHistory(IGridNode gridNode) {
        var nodeKey = getNodeKey(gridNode);
        if (nodeKey == null) {
            return new HashMap<>();
        }
        ensureLoaded(gridNode, nodeKey);
        if (pruneOldest(nodeKey)) {
            persist(gridNode, nodeKey);
        }
        var history = historyByNodeKey.get(nodeKey);
        return history == null ? new HashMap<>() : new HashMap<>(history);
    }

    public static HashMap<AEKey, RecentInteractionInfo> snapshotDetails(IGridNode gridNode) {
        var nodeKey = getNodeKey(gridNode);
        if (nodeKey == null) {
            return new HashMap<>();
        }
        ensureLoaded(gridNode, nodeKey);
        if (pruneOldest(nodeKey)) {
            persist(gridNode, nodeKey);
        }
        var details = detailsByNodeKey.get(nodeKey);
        return details == null ? new HashMap<>() : new HashMap<>(details);
    }

    public static boolean isHistoryPinEnabled(IGridNode gridNode) {
        var nodeKey = getNodeKey(gridNode);
        if (nodeKey == null) {
            return false;
        }
        ensureLoaded(gridNode, nodeKey);
        if (pruneOldest(nodeKey)) {
            persist(gridNode, nodeKey);
        }
        return historyPinEnabledByNodeKey.getOrDefault(nodeKey, false);
    }

    public static void setHistoryPinEnabled(IGridNode gridNode, boolean enabled) {
        var nodeKey = getNodeKey(gridNode);
        if (nodeKey == null) {
            return;
        }
        ensureLoaded(gridNode, nodeKey);
        historyPinEnabledByNodeKey.put(nodeKey, enabled);
        persist(gridNode, nodeKey);
    }

    private static void ensureLoaded(IGridNode gridNode, String nodeKey) {
        if (loadedByNodeKey.getOrDefault(nodeKey, false)) {
            return;
        }
        var serverLevel = getServerLevel(gridNode);
        if (serverLevel == null) {
            historyByNodeKey.put(nodeKey, new HashMap<>());
            detailsByNodeKey.put(nodeKey, new HashMap<>());
            sequenceByNodeKey.put(nodeKey, 0L);
            historyPinEnabledByNodeKey.put(nodeKey, false);
            loadedByNodeKey.put(nodeKey, true);
            return;
        }

        var data = RecentAccessSavedData.get(serverLevel);
        var loadedDetails = data.getDetails(nodeKey);
        var loadedHistory = new HashMap<AEKey, Long>();
        for (var entry : loadedDetails.entrySet()) {
            loadedHistory.put(entry.getKey(), entry.getValue().sequence());
        }

        historyByNodeKey.put(nodeKey, loadedHistory);
        detailsByNodeKey.put(nodeKey, loadedDetails);
        sequenceByNodeKey.put(nodeKey, Math.max(data.getSequence(nodeKey), maxSequence(loadedDetails)));
        historyPinEnabledByNodeKey.put(nodeKey, data.isHistoryPinEnabled(nodeKey));
        loadedByNodeKey.put(nodeKey, true);
    }

    private static void persist(IGridNode gridNode, String nodeKey) {
        var serverLevel = getServerLevel(gridNode);
        if (serverLevel == null) {
            return;
        }
        var data = RecentAccessSavedData.get(serverLevel);
        data.put(
                nodeKey,
                sequenceByNodeKey.getOrDefault(nodeKey, 0L),
                detailsByNodeKey.getOrDefault(nodeKey, new HashMap<>())
        );
        data.putHistoryPinEnabled(nodeKey, historyPinEnabledByNodeKey.getOrDefault(nodeKey, false));
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

    private static int maxRememberedItems() {
        return maxHistoryRows() * 9 + 27;
    }

    private static boolean pruneOldest(String nodeKey) {
        var details = detailsByNodeKey.get(nodeKey);
        var history = historyByNodeKey.get(nodeKey);
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

    private static ServerLevel getServerLevel(IGridNode gridNode) {
        return gridNode.getLevel();
    }

    private static String getNodeKey(IGridNode node) {
        var level = node.getLevel();
        var owner = node.getOwner();
        var dim = level.dimension().location().toString();
        if (owner instanceof BlockEntity blockEntity) {
            return dim + "|be|" + blockEntity.getBlockPos().asLong();
        }
        if (owner instanceof AEBasePart part && part.getBlockEntity() != null) {
            return dim + "|part|" + part.getBlockEntity().getBlockPos().asLong() + "|" + part.getSide().getName();
        }
        return dim + "|owner|" + owner.getClass().getName() + "|" + System.identityHashCode(owner);
    }
}

