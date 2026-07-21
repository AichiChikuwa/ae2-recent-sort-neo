package net.aichichikuwa.appliedhistory.sort;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class RecentTooltipFormatter {
    private RecentTooltipFormatter() {
    }

    public static MutableComponent buildSentence(RecentInteractionInfo info, long nowMillis) {
        long ageSeconds = Math.max(1L, (nowMillis - info.timestampMillis()) / 1000L);
        return Component.translatable(
                "tooltip.appliedhistory.history.detail",
                actionLabel(info.action()),
                info.playerName(),
                formatAge(ageSeconds)
        );
    }

    private static MutableComponent actionLabel(RecentInteractionAction action) {
        return switch (action) {
            case inserted -> Component.translatable("tooltip.appliedhistory.action.imported");
            case extracted -> Component.translatable("tooltip.appliedhistory.action.exported");
            case requested -> Component.translatable("tooltip.appliedhistory.action.requested");
        };
    }

    private static MutableComponent formatAge(long ageSeconds) {
        if (ageSeconds < 60) {
            return Component.translatable(
                    ageSeconds == 1 ? "tooltip.appliedhistory.age.second" : "tooltip.appliedhistory.age.seconds",
                    ageSeconds
            );
        }
        long minutes = ageSeconds / 60;
        if (minutes < 60) {
            return Component.translatable(
                    minutes == 1 ? "tooltip.appliedhistory.age.minute" : "tooltip.appliedhistory.age.minutes",
                    minutes
            );
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return Component.translatable(
                    hours == 1 ? "tooltip.appliedhistory.age.hour" : "tooltip.appliedhistory.age.hours",
                    hours
            );
        }
        long days = hours / 24;
        return Component.translatable(
                days == 1 ? "tooltip.appliedhistory.age.day" : "tooltip.appliedhistory.age.days",
                days
        );
    }
}
