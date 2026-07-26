package net.aichichikuwa.appliedhistory.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.Nullable;

// helpers for the logger's fixed 1x1x3 vertical multiblock
public final class MELoggerMultiblock {
    public static final int HEIGHT = 3;

    private MELoggerMultiblock() {
    }

    public static BlockPos offset(int segment) {
        return new BlockPos(0, segment, 0);
    }

    @Nullable
    public static BlockPos getMainPos(LevelReader level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof MELoggerBlock) {
            return pos;
        }
        if (state.getBlock() instanceof MELoggerBoundingBlock) {
            var be = level.getBlockEntity(pos);
            if (be instanceof MELoggerBoundingBlockEntity bounding) {
                return bounding.getMainPos();
            }
        }
        return null;
    }

    @Nullable
    public static MELoggerBlockEntity getMainBlockEntity(LevelReader level, BlockPos pos) {
        var mainPos = getMainPos(level, pos);
        if (mainPos == null) {
            return null;
        }
        var be = level.getBlockEntity(mainPos);
        return be instanceof MELoggerBlockEntity logger ? logger : null;
    }

    public static boolean canPlaceStructure(Level level, BlockPos mainPos) {
        for (int segment = 0; segment < HEIGHT; segment++) {
            var partPos = mainPos.offset(offset(segment));
            if (!level.getWorldBorder().isWithinBounds(partPos)) {
                return false;
            }
            if (segment == 0) {
                continue;
            }
            if (!level.getBlockState(partPos).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isStructureIntact(LevelReader level, BlockPos mainPos) {
        if (!(level.getBlockState(mainPos).getBlock() instanceof MELoggerBlock)) {
            return false;
        }
        for (int segment = 1; segment < HEIGHT; segment++) {
            var partPos = mainPos.offset(offset(segment));
            var state = level.getBlockState(partPos);
            if (!(state.getBlock() instanceof MELoggerBoundingBlock)) {
                return false;
            }
            var be = level.getBlockEntity(partPos);
            if (!(be instanceof MELoggerBoundingBlockEntity bounding)) {
                return false;
            }
            if (!mainPos.equals(bounding.getMainPos())) {
                return false;
            }
        }
        return true;
    }

    public static void placeBoundingBlocks(Level level, BlockPos mainPos, MELoggerBoundingBlock boundingBlock) {
        for (int segment = 1; segment < HEIGHT; segment++) {
            var partPos = mainPos.offset(offset(segment));
            level.setBlock(partPos, boundingBlock.defaultBlockState(), 3);
            var be = level.getBlockEntity(partPos);
            if (be instanceof MELoggerBoundingBlockEntity bounding) {
                bounding.setMainPos(mainPos);
            }
        }
    }

    public static void removeBoundingBlocks(Level level, BlockPos mainPos) {
        for (int segment = 1; segment < HEIGHT; segment++) {
            var partPos = mainPos.offset(offset(segment));
            if (level.getBlockState(partPos).getBlock() instanceof MELoggerBoundingBlock) {
                // clear linkage first so bounding onRemove does not try to drop the main again
                var be = level.getBlockEntity(partPos);
                if (be instanceof MELoggerBoundingBlockEntity bounding) {
                    bounding.clearMainPos();
                }
                level.removeBlockEntity(partPos);
                level.removeBlock(partPos, false);
            }
        }
    }
}
