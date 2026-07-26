package net.aichichikuwa.appliedhistory.client;

import appeng.api.util.AEColor;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.client.render.CubeBuilder;
import appeng.core.AppEng;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.aichichikuwa.appliedhistory.block.MELoggerBlock;
import net.aichichikuwa.appliedhistory.block.MELoggerBlockEntity;
import net.aichichikuwa.appliedhistory.block.MELoggerMultiblock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class MELoggerBlockEntityRenderer implements BlockEntityRenderer<MELoggerBlockEntity, MELoggerRenderState> {
    // model origin sits 16px below the block floor; shift up one block unit when drawing
    private static final float MODEL_Y_OFFSET = 1.0f;
    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();

    public MELoggerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    // cover the full 1x1x3 so the ber is not culled when only upper cells are on screen
    @Override
    public AABB getRenderBoundingBox(MELoggerBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1,
                pos.getY() + MELoggerMultiblock.HEIGHT,
                pos.getZ() + 1);
    }

    @Override
    public MELoggerRenderState createRenderState() {
        return new MELoggerRenderState();
    }

    @Override
    public void extractRenderState(MELoggerBlockEntity blockEntity, MELoggerRenderState state, float partialTicks,
            Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPos, crumblingOverlay);

        var level = blockEntity.getLevel();
        if (level == null) {
            state.modelRenderState.clear();
            state.underCable = false;
            state.underCableSprite = null;
            return;
        }

        BlockState renderState = blockEntity.getBlockState().setValue(MELoggerBlock.STATUS, blockEntity.getStatus());
        state.modelRenderState.clear();
        Minecraft.getInstance().getModelManager().getBlockModelSet().get(renderState)
                .update(state.modelRenderState, renderState, BLOCK_DISPLAY_CONTEXT, 42L);
        state.structureLight = structurePackedLight(level, blockEntity.getBlockPos());

        state.underCable = false;
        state.underCableSprite = null;
        state.underCableColor = null;
        if (level.getBlockEntity(blockEntity.getBlockPos().below()) instanceof CableBusBlockEntity cableBus) {
            AEColor color = cableBus.getCableBus().getColor();
            state.underCable = true;
            state.underCableColor = color;
            Identifier spriteId = AppEng.makeId("part/cable/glass/" + color.name().toLowerCase(Locale.ROOT));
            state.underCableSprite = Minecraft.getInstance().getAtlasManager()
                    .getAtlasOrThrow(AtlasIds.BLOCKS)
                    .getSprite(spriteId);
        }
    }

    @Override
    public void submit(MELoggerRenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
            CameraRenderState cameraRenderState) {
        if (!state.modelRenderState.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0, MODEL_Y_OFFSET, 0);
            state.modelRenderState.submit(poseStack, nodes, state.structureLight, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        if (!state.underCable || state.underCableSprite == null) {
            return;
        }

        TextureAtlasSpriteHolder sprite = new TextureAtlasSpriteHolder(state.underCableSprite);
        int light = state.structureLight;
        nodes.submitCustomGeometry(poseStack, Sheets.translucentBlockItemSheet(), (pose, consumer) -> {
            var qi = new QuadInstance();
            qi.setLightCoords(light);
            var builder = new CubeBuilder(bakedQuad -> consumer.putBakedQuad(pose, bakedQuad, qi));
            builder.setTexture(sprite.sprite);
            // skip the floor face to avoid z-fighting with the cable's up arm in the block below
            builder.setDrawFaces(EnumSet.complementOf(EnumSet.of(Direction.DOWN)));
            // match CableBuilder.addGlassConnection(DOWN): 4px glass arm from the floor up into the machine
            builder.addCube(6, 0, 6, 10, 6, 10);
        });
    }

    private static int structurePackedLight(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos mainPos) {
        int packed = LightCoordsUtil.FULL_BRIGHT;
        int best = 0;
        for (int segment = 0; segment < MELoggerMultiblock.HEIGHT; segment++) {
            int light = LevelRenderer.getLightCoords(level, mainPos.above(segment));
            if (light > best) {
                best = light;
                packed = light;
            }
        }
        return packed;
    }

    // tiny holder so the lambda can capture a final sprite reference cleanly
    private record TextureAtlasSpriteHolder(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
    }
}
