package net.aichichikuwa.appliedhistory.net;

import net.aichichikuwa.appliedhistory.Constants;
import net.aichichikuwa.appliedhistory.item.DormantLoggerHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DormantRitualClickPayload() implements CustomPacketPayload {
    public static final Type<DormantRitualClickPayload> type = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.modId, "dormant_ritual_click")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, DormantRitualClickPayload> streamCodec =
            StreamCodec.unit(new DormantRitualClickPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return type;
    }

    public static void handle(DormantRitualClickPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                DormantLoggerHandler.handleLeftClick(player);
            }
        });
    }
}
