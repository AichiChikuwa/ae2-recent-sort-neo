package net.aichichikuwa.appliedhistory.block;

import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

// collision / outline shapes for the logger multiblock; coordinates match the model after +16px elevation
public final class MELoggerShapes {
    private MELoggerShapes() {
    }

    // full structure shape in coordinates relative to the bottom (main) block
    private static final VoxelShape STRUCTURE = Shapes.or(
            // main bottom cube (y 2..16 in model space)
            box(0, 0.125f, 0, 1, 1, 1),
            // corner feet (y 0..2)
            box(0.875f, 0, 0, 1, 0.125f, 0.125f),
            box(0, 0, 0, 0.125f, 0.125f, 0.125f),
            box(0, 0, 0.875f, 0.125f, 0.125f, 1),
            box(0.875f, 0, 0.875f, 1, 0.125f, 1),
            // middle column (y 16..32)
            box(0.125f, 1, 0.125f, 0.875f, 2, 0.875f),
            // top cap (y 32..48)
            box(0, 2, 0, 1, 3, 1)
    );

    public static VoxelShape structureShape() {
        return STRUCTURE;
    }

    private static VoxelShape box(float x1, float y1, float z1, float x2, float y2, float z2) {
        return Shapes.box(x1, y1, z1, x2, y2, z2);
    }
}
