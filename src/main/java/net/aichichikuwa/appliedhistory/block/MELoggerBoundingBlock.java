package net.aichichikuwa.appliedhistory.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

// invisible segment of the logger multiblock; forwards interaction and collision to the main block below
public class MELoggerBoundingBlock extends Block implements EntityBlock {
    private final Supplier<BlockEntityType<MELoggerBoundingBlockEntity>> blockEntityType;

    public MELoggerBoundingBlock(Supplier<BlockEntityType<MELoggerBoundingBlockEntity>> blockEntityType, Properties properties) {
        super(properties);
        this.blockEntityType = blockEntityType;
    }

    @Nullable
    public static BlockPos getMainPos(BlockGetter level, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        if (be instanceof MELoggerBoundingBlockEntity bounding && bounding.canRedirectFrom(pos)) {
            return bounding.getMainPos();
        }
        // after the bounding be is cleared (e.g. affectNeighborsAfterRemoval), fall back to fixed geometry
        return findMainPosByGeometry(level, pos);
    }

    // 1x1x3: bounding cells sit directly above the main logger
    @Nullable
    private static BlockPos findMainPosByGeometry(BlockGetter level, BlockPos pos) {
        for (int below = 1; below < MELoggerMultiblock.HEIGHT; below++) {
            var candidate = pos.below(below);
            if (level.getBlockState(candidate).getBlock() instanceof MELoggerBlock) {
                return candidate;
            }
        }
        return null;
    }

    public static BlockHitResult redirectHit(BlockHitResult hit, BlockPos clickedPos, BlockPos mainPos) {
        var offset = clickedPos.subtract(mainPos);
        if (hit.getType() == HitResult.Type.MISS) {
            return BlockHitResult.miss(
                    hit.getLocation().subtract(Vec3.atLowerCornerOf(offset)),
                    hit.getDirection(),
                    mainPos);
        }
        return new BlockHitResult(
                hit.getLocation().subtract(Vec3.atLowerCornerOf(offset)),
                hit.getDirection(),
                mainPos,
                hit.isInside());
    }

    @Nullable
    private static MainProxy getMainProxy(BlockGetter level, BlockPos pos) {
        var mainPos = getMainPos(level, pos);
        if (mainPos == null) {
            return null;
        }
        var mainState = level.getBlockState(mainPos);
        if (!(mainState.getBlock() instanceof MELoggerBlock)) {
            return null;
        }
        return new MainProxy(mainPos, mainState, level);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return proxyShape(level, pos, BlockState::getShape);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return proxyShape(level, pos, BlockState::getCollisionShape);
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return proxyShape(level, pos, BlockState::getVisualShape);
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return proxyShape(level, pos, (mainState, mainLevel, mainPos, ctx) -> mainState.getInteractionShape(mainLevel, mainPos));
    }

    private VoxelShape proxyShape(BlockGetter level, BlockPos pos, ShapeProxy proxy) {
        var main = getMainProxy(level, pos);
        if (main == null) {
            return Shapes.empty();
        }
        var offset = pos.subtract(main.pos);
        return proxy.getShape(main.state, level, main.pos, null)
                .move(-offset.getX(), -offset.getY(), -offset.getZ());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        var main = getMainProxy(level, pos);
        if (main == null) {
            return InteractionResult.PASS;
        }
        return main.state.useWithoutItem(level, player, redirectHit(hit, pos, main.pos));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        var main = getMainProxy(level, pos);
        if (main == null) {
            return InteractionResult.PASS;
        }
        return main.state.useItemOn(stack, level, player, hand, redirectHit(hit, pos, main.pos));
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        // be is already gone here; geometry fallback finds the main logger
        var mainPos = findMainPosByGeometry(level, pos);
        if (mainPos != null) {
            var be = level.getBlockEntity(mainPos);
            if (be instanceof MELoggerBlockEntity logger && !logger.isTearingDown()) {
                // automation (annihilation plane, etc.) removes bounding without player harvest;
                // tear down the whole logger and drop its item with identity preserved
                logger.dropAndRemoveStructure();
            } else if (!(be instanceof MELoggerBlockEntity)) {
                var mainState = level.getBlockState(mainPos);
                if (!mainState.isAir()) {
                    level.removeBlock(mainPos, false);
                }
            }
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        var main = getMainProxy(level, pos);
        if (main == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(main.state.getBlock().asItem());
    }

    // neo 26: creative (preventsBlockDrops) never calls playerDestroy, and willHarvest deferral is unreliable,
    // so tear down + drop here instead of waiting for playerDestroy
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, ItemStack tool,
            boolean willHarvest, FluidState fluidState) {
        if (level.isClientSide()) {
            // client prediction: clear the whole structure visually
            var main = getMainProxy(level, pos);
            if (main != null && !main.state.isAir()) {
                level.removeBlock(main.pos, false);
            }
            return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluidState);
        }

        var mainPos = getMainPos(level, pos);
        if (mainPos != null) {
            var be = level.getBlockEntity(mainPos);
            if (be instanceof MELoggerBlockEntity logger && !logger.isTearingDown()) {
                if (player.preventsBlockDrops()) {
                    logger.dropAndRemoveStructureWithoutWorldDrop();
                } else {
                    // world-drop the identity-preserving item, then remove the whole 1x1x3
                    logger.dropAndRemoveStructure();
                }
                return true;
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluidState);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        var main = getMainProxy(level, pos);
        if (main != null && !main.state.isAir()) {
            main.state.getBlock().playerWillDestroy(level, main.pos, main.state, player);
            return state;
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        var main = getMainProxy(level, pos);
        if (main == null) {
            return super.getDestroyProgress(state, player, level, pos);
        }
        return main.state.getDestroyProgress(player, level, main.pos);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
            ItemStack tool) {
        // drop/teardown already ran in onDestroyedByPlayer; avoid empty bounding loot and a second removal
        if (!level.getBlockState(pos).isAir()) {
            level.removeBlock(pos, false);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MELoggerBoundingBlockEntity(blockEntityType.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide() || type != blockEntityType.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ((MELoggerBoundingBlockEntity) be).serverTick();
    }

    private record MainProxy(BlockPos pos, BlockState state, BlockGetter level) {
    }

    @FunctionalInterface
    private interface ShapeProxy {
        VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, @Nullable CollisionContext context);
    }
}
