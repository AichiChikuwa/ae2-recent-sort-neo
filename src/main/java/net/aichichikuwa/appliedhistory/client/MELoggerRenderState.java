package net.aichichikuwa.appliedhistory.client;

import appeng.api.util.AEColor;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.Nullable;

public class MELoggerRenderState extends BlockEntityRenderState {
    final BlockModelRenderState modelRenderState = new BlockModelRenderState();
    boolean underCable;
    @Nullable
    AEColor underCableColor;
    @Nullable
    TextureAtlasSprite underCableSprite;
    int structureLight;
}
