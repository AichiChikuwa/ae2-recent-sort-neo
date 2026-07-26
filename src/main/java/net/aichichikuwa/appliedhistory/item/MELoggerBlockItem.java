package net.aichichikuwa.appliedhistory.item;

import net.aichichikuwa.appliedhistory.block.MELoggerMultiblock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class MELoggerBlockItem extends BlockItem {
    public MELoggerBlockItem(net.minecraft.world.level.block.Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    @Nullable
    protected BlockState getPlacementState(BlockPlaceContext context) {
        var state = super.getPlacementState(context);
        if (state == null) {
            return null;
        }
        return MELoggerMultiblock.canPlaceStructure(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltipComponents, TooltipFlag flag) {
        if (MELoggerItems.hasHistoryId(stack)) {
            tooltipComponents.accept(Component.translatable("tooltip.appliedhistory.me_logger.ready")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltipComponents.accept(Component.translatable("tooltip.appliedhistory.me_logger.invalid")
                    .withStyle(ChatFormatting.RED));
        }
        tooltipComponents.accept(Component.translatable("tooltip.appliedhistory.me_logger.obtain_dormant")
                .withStyle(ChatFormatting.GRAY));
        if (MELoggerItems.isLunaticOrigin(stack)) {
            tooltipComponents.accept(Component.translatable("tooltip.appliedhistory.lunatic_origin")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}

