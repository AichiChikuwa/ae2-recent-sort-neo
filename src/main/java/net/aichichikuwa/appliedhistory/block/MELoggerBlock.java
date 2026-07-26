package net.aichichikuwa.appliedhistory.block;

import appeng.api.orientation.IOrientationStrategy;
import appeng.api.orientation.OrientationStrategies;
import appeng.block.AEBaseEntityBlock;
import appeng.core.AEConfig;
import appeng.core.particles.ParticleTypes;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.aichichikuwa.appliedhistory.AHRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class MELoggerBlock extends AEBaseEntityBlock<MELoggerBlockEntity> {
    public static final EnumProperty<MELoggerStatus> STATUS = EnumProperty.create("status", MELoggerStatus.class);

    private final Supplier<MELoggerBoundingBlock> boundingBlock;

    public MELoggerBlock(Properties properties, Supplier<MELoggerBoundingBlock> boundingBlock) {
        super(properties);
        this.boundingBlock = boundingBlock;
        this.registerDefaultState(this.defaultBlockState().setValue(STATUS, MELoggerStatus.OFF));
    }

    public MELoggerBoundingBlock getBoundingBlock() {
        return boundingBlock.get();
    }

    @Override
    public IOrientationStrategy getOrientationStrategy() {
        // facing only; spin is not supported on the multiblock model
        return OrientationStrategies.horizontalFacing();
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
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return MELoggerShapes.structureShape();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return MELoggerShapes.structureShape();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return MELoggerShapes.structureShape();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return openMenu(level, pos, player);
    }

    static InteractionResult openMenu(Level level, BlockPos pos, Player player) {
        var logger = MELoggerMultiblock.getMainBlockEntity(level, pos);
        if (logger != null) {
            if (!level.isClientSide()) {
                MenuOpener.open(AHRegistry.ME_LOGGER_MENU_TYPE, player, MenuLocators.forBlockEntity(logger));
            }
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            MELoggerMultiblock.placeBoundingBlocks(level, pos, getBoundingBlock());
            var be = getBlockEntity(level, pos);
            if (be != null) {
                be.refreshUnderCable();
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
            @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (!level.isClientSide()) {
            var be = getBlockEntity(level, pos);
            if (be != null) {
                be.refreshUnderCable();
            }
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        MELoggerMultiblock.removeBoundingBlocks(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    // plates in model pixels (north-facing authoring space); ber elevates by +16 so they sit in the bottom cell
    private static final float[] PLATE_A = {6.5f, -10f, 1f, 7.5f, -3f, 6f};
    private static final float[] PLATE_B = {8.5f, -10f, 1f, 9.5f, -3f, 6f};

    // same vibrant motes as ae2 quartz vibrant glass; only while the logger is actively working
    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(STATUS) != MELoggerStatus.ON) {
            return;
        }
        if (!AEConfig.instance().isEnableEffects()) {
            return;
        }
        if (!random.nextBoolean()) {
            return;
        }
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        spawnPlateParticle(level, pos, facing, random, PLATE_A);
        spawnPlateParticle(level, pos, facing, random, PLATE_B);
    }

    private static void spawnPlateParticle(Level level, BlockPos pos, Direction facing, RandomSource random,
            float[] plate) {
        float mx = Mth.lerp(random.nextFloat(), plate[0], plate[3]);
        float my = Mth.lerp(random.nextFloat(), plate[1], plate[4]);
        float mz = Mth.lerp(random.nextFloat(), plate[2], plate[5]);
        var world = modelPointToWorld(pos, facing, mx, my, mz);
        level.addParticle(ParticleTypes.VIBRANT, world.x, world.y, world.z, 0.0D, 0.0D, 0.0D);
    }

    // model coords are authored for facing=north; blockstate y-rotates the mesh the same way
    private static Vec3 modelPointToWorld(BlockPos pos, Direction facing, float modelX, float modelY, float modelZ) {
        double lx = modelX / 16.0;
        double ly = (modelY + 16.0) / 16.0;
        double lz = modelZ / 16.0;
        double rx = lx - 0.5;
        double rz = lz - 0.5;
        double nx;
        double nz;
        switch (facing) {
            case EAST -> {
                nx = -rz;
                nz = rx;
            }
            case SOUTH -> {
                nx = -rx;
                nz = -rz;
            }
            case WEST -> {
                nx = rz;
                nz = -rx;
            }
            default -> {
                nx = rx;
                nz = rz;
            }
        }
        return new Vec3(pos.getX() + nx + 0.5, pos.getY() + ly, pos.getZ() + nz + 0.5);
    }
}
