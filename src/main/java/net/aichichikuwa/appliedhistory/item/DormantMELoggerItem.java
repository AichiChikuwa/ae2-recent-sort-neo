package net.aichichikuwa.appliedhistory.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class DormantMELoggerItem extends Item {
    public static final int MAX_PROGRESS = 5;
    public static final int ROLLBACK_DELAY_TICKS = 60;
    public static final int ROLLBACK_STEP_TICKS = 10;
    public static final int COMPLETION_CONDUIT_DELAY_TICKS = 10;
    public static final int COMPLETION_TRANSFORM_DELAY_TICKS = 60;

    public DormantMELoggerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getProgress(stack) / (float) MAX_PROGRESS);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float ratio = getProgress(stack) / (float) MAX_PROGRESS;
        return Mth.hsvToRgb(ratio / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        tooltipComponents.add(Component.translatable("tooltip.appliedhistory.dormant_me_logger.instructions")
                .withStyle(ChatFormatting.GRAY));
        if (MELoggerItems.isLunaticOrigin(stack)) {
            tooltipComponents.add(Component.translatable("tooltip.appliedhistory.lunatic_origin")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    public static int getProgress(ItemStack stack) {
        return stack.getOrDefault(MELoggerItems.transformProgress(), 0);
    }

    public static void setProgress(ItemStack stack, int progress) {
        stack.set(MELoggerItems.transformProgress(), Math.max(0, Math.min(MAX_PROGRESS, progress)));
    }

    public static long getLastActionTick(ItemStack stack) {
        return stack.getOrDefault(MELoggerItems.transformLastActionTick(), 0L);
    }

    public static void setLastActionTick(ItemStack stack, long tick) {
        stack.set(MELoggerItems.transformLastActionTick(), tick);
    }

    public static long getCompleteTick(ItemStack stack) {
        return stack.getOrDefault(MELoggerItems.transformCompleteTick(), -1L);
    }

    public static void setCompleteTick(ItemStack stack, long tick) {
        stack.set(MELoggerItems.transformCompleteTick(), tick);
    }

    public static boolean isConduitPlayed(ItemStack stack) {
        return stack.getOrDefault(MELoggerItems.transformConduitPlayed(), false);
    }

    public static void setConduitPlayed(ItemStack stack, boolean played) {
        stack.set(MELoggerItems.transformConduitPlayed(), played);
    }

    public static void clearTransformationState(ItemStack stack) {
        stack.remove(MELoggerItems.transformProgress());
        stack.remove(MELoggerItems.transformLastActionTick());
        stack.remove(MELoggerItems.transformCompleteTick());
        stack.remove(MELoggerItems.transformConduitPlayed());
    }
}
