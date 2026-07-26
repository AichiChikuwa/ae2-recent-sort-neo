package net.aichichikuwa.appliedhistory.net;

import appeng.api.stacks.AEKey;
import com.google.common.collect.Maps;
import net.aichichikuwa.appliedhistory.Constants;
import net.aichichikuwa.appliedhistory.sort.ClientRecentAccessState;
import net.aichichikuwa.appliedhistory.sort.RecentInteractionAction;
import net.aichichikuwa.appliedhistory.sort.RecentInteractionInfo;
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
        boolean freezeReorderPulse,
        boolean hasLogger,
        boolean loggerConflict
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
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<AEKey, Long>> historyCodec =
            ByteBufCodecs.map(Maps::newHashMapWithExpectedSize, AEKey.STREAM_CODEC, ByteBufCodecs.VAR_LONG);
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<AEKey, RecentInteractionInfo>> detailsCodec =
            ByteBufCodecs.map(Maps::newHashMapWithExpectedSize, AEKey.STREAM_CODEC, infoCodec);

    // written manually because StreamCodec.composite only supports up to 6 fields
    public static final StreamCodec<RegistryFriendlyByteBuf, RecentAccessPayload> streamCodec = StreamCodec.of(
            (buffer, payload) -> {
                ByteBufCodecs.INT.encode(buffer, payload.containerId);
                historyCodec.encode(buffer, payload.history);
                detailsCodec.encode(buffer, payload.details);
                buffer.writeBoolean(payload.historyPinEnabled);
                ByteBufCodecs.INT.encode(buffer, payload.maxHistoryRows);
                buffer.writeBoolean(payload.freezeReorderPulse);
                buffer.writeBoolean(payload.hasLogger);
                buffer.writeBoolean(payload.loggerConflict);
            },
            buffer -> new RecentAccessPayload(
                    ByteBufCodecs.INT.decode(buffer),
                    historyCodec.decode(buffer),
                    detailsCodec.decode(buffer),
                    buffer.readBoolean(),
                    ByteBufCodecs.INT.decode(buffer),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean()
            )
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
            ClientRecentAccessState.setHasLogger(payload.containerId, payload.hasLogger);
            ClientRecentAccessState.setLoggerConflict(payload.containerId, payload.loggerConflict);
            if (payload.freezeReorderPulse) {
                ClientRecentAccessState.triggerHistoryReorderFreeze(payload.containerId);
            }
        });
    }
}

