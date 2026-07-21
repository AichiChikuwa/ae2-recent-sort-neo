package net.aichichikuwa.appliedhistory.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        var main = getMainProxy(level, pos);
        if (main == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return main.state.useItemOn(stack, level, player, hand, redirectHit(hit, pos, main.pos));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            var mainPos = getMainPos(level, pos);
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
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        var main = getMainProxy(level, pos);
        if (main == null) {
            return ItemStack.EMPTY;
        }
        var redirectedTarget = target instanceof BlockHitResult blockHit
                ? redirectHit(blockHit, pos, main.pos)
                : target;
        return main.state.getBlock().getCloneItemStack(main.state, redirectedTarget, level, main.pos, player);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest,
            FluidState fluidState) {
        if (willHarvest) {
            return true;
        }
        var main = getMainProxy(level, pos);
        if (main != null && !main.state.isAir()) {
            main.state.onDestroyedByPlayer(level, main.pos, player, false, main.state.getFluidState());
        }
        return super.onDestroyedByPlayer(state, level, pos, player, false, fluidState);
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
        var mainPos = getMainPos(level, pos);
        if (mainPos != null) {
            var be = level.getBlockEntity(mainPos);
            if (be instanceof MELoggerBlockEntity logger && !logger.isTearingDown()) {
                // one drop into the player's inventory, then tear the structure down without a second world drop
                player.getInventory().placeItemBackInInventory(logger.createDismantleDrop());
                logger.dropAndRemoveStructureWithoutWorldDrop();
                return;
            }
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        level.removeBlock(pos, false);
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
