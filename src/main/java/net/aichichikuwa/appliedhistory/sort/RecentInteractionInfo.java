package net.aichichikuwa.appliedhistory.sort;

public record RecentInteractionInfo(
        long sequence,
        long timestampMillis,
        String playerName,
        RecentInteractionAction action
) {
}
