package net.aichichikuwa.appliedhistory.client;

import appeng.api.util.AEColor;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.client.render.cablebus.CubeBuilder;
import appeng.core.AppEng;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.aichichikuwa.appliedhistory.block.MELoggerBlock;
import net.aichichikuwa.appliedhistory.block.MELoggerBlockEntity;
import net.aichichikuwa.appliedhistory.block.MELoggerMultiblock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.EnumSet;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class MELoggerBlockEntityRenderer implements BlockEntityRenderer<MELoggerBlockEntity> {
    // model origin sits 16px below the block floor; shift up one block unit when drawing
    private static final float MODEL_Y_OFFSET = 1.0f;

    private final BlockRenderDispatcher blockRenderer;

    public MELoggerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(MELoggerBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
            int packedLight, int packedOverlay) {
        var level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        BlockState renderState = blockEntity.getBlockState().setValue(MELoggerBlock.STATUS, blockEntity.getStatus());
        var model = blockRenderer.getBlockModelShaper().getBlockModel(renderState);
        // cutout respects the atlas alpha; solid would paint transparent texels opaque (black)
        var buffer = buffers.getBuffer(RenderType.cutout());
        // flat light across the tall mesh: tesselate* samples face-adjacent light at the be pos,
        // so a solid neighbor of the bottom block would darken the whole side including the visual top
        int structureLight = structurePackedLight(level, blockEntity.getBlockPos());

        poseStack.pushPose();
        poseStack.translate(0, MODEL_Y_OFFSET, 0);
        blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                buffer,
                renderState,
                model,
                1.0f,
                1.0f,
                1.0f,
                structureLight,
                packedOverlay,
                ModelData.EMPTY,
                RenderType.cutout());
        poseStack.popPose();

        if (level.getBlockEntity(blockEntity.getBlockPos().below()) instanceof CableBusBlockEntity cableBus) {
            renderUnderCableStub(cableBus.getCableBus().getColor(), poseStack, buffers, structureLight);
        }
    }

    private static int structurePackedLight(BlockAndTintGetter level, BlockPos mainPos) {
        int block = 0;
        int sky = 0;
        for (int segment = 0; segment < MELoggerMultiblock.HEIGHT; segment++) {
            int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, mainPos.above(segment));
            block = Math.max(block, LightTexture.block(light));
            sky = Math.max(sky, LightTexture.sky(light));
        }
        return LightTexture.pack(block, sky);
    }

    private void renderUnderCableStub(AEColor color, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        // same texture path ae2 uses for glass cable connections (TRANSPARENT → "transparent")
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(AppEng.makeId("part/cable/glass/" + color.name().toLowerCase(Locale.ROOT)));

        // match CableBuilder.addGlassConnection(DOWN): 4px glass arm from the floor up into the machine
        // body (logger body starts at y=2px after ber offset), so it overlaps the bottom face
        var cubeBuilder = new CubeBuilder();
        cubeBuilder.setTexture(sprite);
        // skip the floor face to avoid z-fighting with the cable's up arm in the block below
        cubeBuilder.setDrawFaces(EnumSet.complementOf(EnumSet.of(Direction.DOWN)));
        cubeBuilder.addCube(6, 0, 6, 10, 6, 10);

        VertexConsumer buffer = buffers.getBuffer(RenderType.cutout());
        poseStack.pushPose();
        for (var quad : cubeBuilder.getOutput()) {
            buffer.putBulkData(
                    poseStack.last(),
                    quad,
                    1.0f,
                    1.0f,
                    1.0f,
                    1.0f,
                    packedLight,
                    OverlayTexture.NO_OVERLAY);
        }
        poseStack.popPose();
    }
}
