package net.meatwo310.appliedaccesssort.net;

import appeng.api.stacks.AEKey;
import com.google.common.collect.Maps;
import net.meatwo310.appliedaccesssort.Constants;
import net.meatwo310.appliedaccesssort.sort.ClientRecentAccessState;
import net.meatwo310.appliedaccesssort.sort.RecentInteractionAction;
import net.meatwo310.appliedaccesssort.sort.RecentInteractionInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;

public record RecentAccessPayload(
        int containerId,
        Map<AEKey, Long> history,
        Map<AEKey, RecentInteractionInfo> details,
        boolean historyPinEnabled,
        int maxHistoryRows,
        boolean freezeReorderPulse
) implements CustomPacketPayload {
    public static final Type<RecentAccessPayload> type = new Type<>(
            Identifier.fromNamespaceAndPath(Constants.modId, "recent_access")
    );
    private static final StreamCodec<RegistryFriendlyByteBuf, RecentInteractionInfo> infoCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            RecentInteractionInfo::sequence,
            ByteBufCodecs.VAR_LONG,
            RecentInteractionInfo::timestampMillis,
            ByteBufCodecs.STRING_UTF8,
            RecentInteractionInfo::playerName,
            ByteBufCodecs.idMapper(id -> RecentInteractionAction.values()[id], RecentInteractionAction::ordinal),
            RecentInteractionInfo::action,
            RecentInteractionInfo::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, RecentAccessPayload> streamCodec = StreamCodec.composite(
            ByteBufCodecs.INT,
            RecentAccessPayload::containerId,
            ByteBufCodecs.map(Maps::newHashMapWithExpectedSize, AEKey.STREAM_CODEC, ByteBufCodecs.VAR_LONG),
            RecentAccessPayload::history,
            ByteBufCodecs.map(Maps::newHashMapWithExpectedSize, AEKey.STREAM_CODEC, infoCodec),
            RecentAccessPayload::details,
            ByteBufCodecs.BOOL,
            RecentAccessPayload::historyPinEnabled,
            ByteBufCodecs.INT,
            RecentAccessPayload::maxHistoryRows,
            ByteBufCodecs.BOOL,
            RecentAccessPayload::freezeReorderPulse,
            RecentAccessPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return type;
    }

    public static void handle(RecentAccessPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientRecentAccessState.replaceHistory(payload.containerId, payload.history);
            ClientRecentAccessState.replaceDetails(payload.containerId, payload.details);
            ClientRecentAccessState.setRecentPinEnabled(payload.containerId, payload.historyPinEnabled);
            ClientRecentAccessState.setMaxHistoryRows(payload.containerId, payload.maxHistoryRows);
            if (payload.freezeReorderPulse) {
                ClientRecentAccessState.triggerHistoryReorderFreeze(payload.containerId);
            }
        });
    }
}

