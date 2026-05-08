package net.meatwo310.appliedaccesssort.sort;

public record RecentInteractionInfo(
        long sequence,
        long timestampMillis,
        String playerName,
        RecentInteractionAction action
) {
}
