package net.meatwo310.appliedaccesssort.block;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.block.AEBaseEntityBlock;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.meatwo310.appliedaccesssort.AHRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class MELoggerBlock extends AEBaseEntityBlock<MELoggerBlockEntity> {
    public static final EnumProperty<MELoggerStatus> STATUS = EnumProperty.create("status", MELoggerStatus.class);

    public MELoggerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(STATUS, MELoggerStatus.OFF));
    }

    @Override
    public IOrientationStrategy getOrientationStrategy() {
        // full() adds both facing and spin and keeps the block upright on normal placement,
        // matching how other fully-orientable ae2 blocks behave
        return OrientationStrategies.full();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STATUS);
    }

    @Override
    protected BlockState updateBlockStateFromBlockEntity(BlockState currentState, MELoggerBlockEntity be) {
        return currentState.setValue(STATUS, be.getStatus());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        var be = getBlockEntity(level, pos);
        if (be != null) {
            if (!level.isClientSide()) {
                MenuOpener.open(AHRegistry.ME_LOGGER_MENU_TYPE, player, MenuLocators.forBlockEntity(be));
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }
}
