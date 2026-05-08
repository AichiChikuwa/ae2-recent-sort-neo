package net.meatwo310.appliedaccesssort.net;

import appeng.menu.me.common.MEStorageMenu;
import net.meatwo310.appliedaccesssort.Constants;
import net.meatwo310.appliedaccesssort.sort.ServerRecentAccessTracker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RecentPinTogglePayload(
        int containerId,
        boolean enabled
) implements CustomPacketPayload {
    public static final Type<RecentPinTogglePayload> type = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.modId, "recent_pin_toggle")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, RecentPinTogglePayload> streamCodec = StreamCodec.composite(
            ByteBufCodecs.INT,
            RecentPinTogglePayload::containerId,
            ByteBufCodecs.BOOL,
            RecentPinTogglePayload::enabled,
            RecentPinTogglePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return type;
    }

    public static void handle(RecentPinTogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!(player.containerMenu instanceof MEStorageMenu menu)) {
                return;
            }
            if (menu.containerId != payload.containerId) {
                return;
            }

            var gridNode = menu.getGridNode();
            if (gridNode == null || gridNode.getGrid() == null) {
                return;
            }

            ServerRecentAccessTracker.setHistoryPinEnabled(gridNode, payload.enabled);
        });
    }
}
