package net.aichichikuwa.appliedhistory.item;

import net.aichichikuwa.appliedhistory.AHRegistry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class MELoggerItems {
    private MELoggerItems() {
    }

    public static boolean hasHistoryId(ItemStack stack) {
        return stack.getItem() == AHRegistry.ME_LOGGER_ITEM.get()
                && stack.has(AHRegistry.HISTORY_ID.get());
    }

    @Nullable
    public static UUID getHistoryId(ItemStack stack) {
        return stack.get(AHRegistry.HISTORY_ID.get());
    }

    public static boolean isLunaticOrigin(ItemStack stack) {
        return stack.getOrDefault(AHRegistry.LUNATIC_ORIGIN.get(), false);
    }

    public static void setLunaticOrigin(ItemStack stack, boolean lunatic) {
        if (lunatic) {
            stack.set(AHRegistry.LUNATIC_ORIGIN.get(), true);
        } else {
            stack.remove(AHRegistry.LUNATIC_ORIGIN.get());
        }
    }

    public static void copyLunaticOrigin(ItemStack from, ItemStack to) {
        setLunaticOrigin(to, isLunaticOrigin(from));
    }

    public static ItemStack createReadyLogger(UUID historyId, boolean lunaticOrigin) {
        var stack = new ItemStack(AHRegistry.ME_LOGGER_ITEM.get());
        stack.set(AHRegistry.HISTORY_ID.get(), historyId);
        setLunaticOrigin(stack, lunaticOrigin);
        return stack;
    }

    public static ItemStack createReadyLogger(UUID historyId) {
        return createReadyLogger(historyId, false);
    }

    public static ItemStack createReadyLogger() {
        return createReadyLogger(UUID.randomUUID(), false);
    }

    public static ItemStack createDormantLogger(boolean lunaticOrigin) {
        var stack = new ItemStack(AHRegistry.DORMANT_ME_LOGGER.get());
        setLunaticOrigin(stack, lunaticOrigin);
        return stack;
    }

    public static DataComponentType<Integer> transformProgress() {
        return AHRegistry.TRANSFORM_PROGRESS.get();
    }

    public static DataComponentType<Long> transformLastActionTick() {
        return AHRegistry.TRANSFORM_LAST_ACTION_TICK.get();
    }

    public static DataComponentType<Long> transformCompleteTick() {
        return AHRegistry.TRANSFORM_COMPLETE_TICK.get();
    }

    public static DataComponentType<Boolean> transformConduitPlayed() {
        return AHRegistry.TRANSFORM_CONDUIT_PLAYED.get();
    }
}
