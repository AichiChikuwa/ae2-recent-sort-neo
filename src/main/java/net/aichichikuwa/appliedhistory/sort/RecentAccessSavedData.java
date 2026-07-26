package net.aichichikuwa.appliedhistory.sort;

import appeng.api.stacks.AEKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RecentAccessSavedData extends SavedData {
    private record DetailEntry(AEKey key, long seq, long time, String player, String action) {
        static final Codec<DetailEntry> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                AEKey.CODEC.fieldOf("key").forGetter(DetailEntry::key),
                Codec.LONG.fieldOf("seq").forGetter(DetailEntry::seq),
                Codec.LONG.fieldOf("time").forGetter(DetailEntry::time),
                Codec.STRING.fieldOf("player").forGetter(DetailEntry::player),
                Codec.STRING.fieldOf("action").forGetter(DetailEntry::action)
        ).apply(builder, DetailEntry::new));
    }

    private record GridEntry(
            String gridKey,
            long sequence,
            boolean historyPinEnabled,
            List<DetailEntry> details
    ) {
        static final Codec<GridEntry> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                Codec.STRING.fieldOf("gridKey").forGetter(GridEntry::gridKey),
                Codec.LONG.fieldOf("sequence").forGetter(GridEntry::sequence),
                Codec.BOOL.fieldOf("historyPinEnabled").forGetter(GridEntry::historyPinEnabled),
                DetailEntry.CODEC.listOf().fieldOf("details").forGetter(GridEntry::details)
        ).apply(builder, GridEntry::new));
    }

    private record SavedPayload(List<GridEntry> grids) {
        static final Codec<SavedPayload> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                GridEntry.CODEC.listOf().fieldOf("grids").forGetter(SavedPayload::grids)
        ).apply(builder, SavedPayload::new));
    }

    private static final Identifier ID = Identifier.fromNamespaceAndPath("appliedhistory", "recent_access");

    private static final SavedDataType<RecentAccessSavedData> TYPE = new SavedDataType<>(
            ID,
            level -> new RecentAccessSavedData(),
            level -> RecordCodecBuilder.create(builder -> builder.group(
                    SavedPayload.CODEC.fieldOf("data").forGetter(RecentAccessSavedData::toPayload)
            ).apply(builder, RecentAccessSavedData::fromPayload))
    );

    public static RecentAccessSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private final Map<String, HashMap<AEKey, RecentInteractionInfo>> detailsByGridKey = new HashMap<>();
    private final Map<String, Long> sequenceByGridKey = new HashMap<>();
    private final Map<String, Boolean> historyPinEnabledByGridKey = new HashMap<>();

    private RecentAccessSavedData() {
    }

    private static RecentAccessSavedData fromPayload(SavedPayload payload) {
        var data = new RecentAccessSavedData();
        for (var grid : payload.grids()) {
            data.sequenceByGridKey.put(grid.gridKey(), grid.sequence());
            data.historyPinEnabledByGridKey.put(grid.gridKey(), grid.historyPinEnabled());
            var details = new HashMap<AEKey, RecentInteractionInfo>();
            for (var detail : grid.details()) {
                RecentInteractionAction action;
                try {
                    action = RecentInteractionAction.valueOf(detail.action());
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
                details.put(detail.key(), new RecentInteractionInfo(
                        detail.seq(),
                        detail.time(),
                        detail.player(),
                        action
                ));
            }
            data.detailsByGridKey.put(grid.gridKey(), details);
        }
        return data;
    }

    private SavedPayload toPayload() {
        var grids = new ArrayList<GridEntry>();
        var keys = keys();
        for (var gridKey : keys) {
            var details = detailsByGridKey.getOrDefault(gridKey, new HashMap<>());
            var detailEntries = new ArrayList<DetailEntry>();
            for (var entry : details.entrySet()) {
                var info = entry.getValue();
                detailEntries.add(new DetailEntry(
                        entry.getKey(),
                        info.sequence(),
                        info.timestampMillis(),
                        info.playerName(),
                        info.action().name()
                ));
            }
            grids.add(new GridEntry(
                    gridKey,
                    sequenceByGridKey.getOrDefault(gridKey, 0L),
                    historyPinEnabledByGridKey.getOrDefault(gridKey, false),
                    detailEntries
            ));
        }
        return new SavedPayload(grids);
    }

    public HashMap<AEKey, RecentInteractionInfo> getDetails(String gridKey) {
        var details = detailsByGridKey.get(gridKey);
        return details == null ? new HashMap<>() : new HashMap<>(details);
    }

    public long getSequence(String gridKey) {
        return sequenceByGridKey.getOrDefault(gridKey, 0L);
    }

    public void put(String gridKey, long sequence, Map<AEKey, RecentInteractionInfo> details) {
        sequenceByGridKey.put(gridKey, sequence);
        detailsByGridKey.put(gridKey, new HashMap<>(details));
        setDirty();
    }

    // completely forgets a logger's stored history so a purged uuid leaves nothing on disk
    public void remove(String gridKey) {
        boolean removed = detailsByGridKey.remove(gridKey) != null;
        removed |= sequenceByGridKey.remove(gridKey) != null;
        removed |= historyPinEnabledByGridKey.remove(gridKey) != null;
        if (removed) {
            setDirty();
        }
    }

    public Set<String> keys() {
        var keys = new HashSet<String>();
        keys.addAll(detailsByGridKey.keySet());
        keys.addAll(sequenceByGridKey.keySet());
        keys.addAll(historyPinEnabledByGridKey.keySet());
        return keys;
    }

    public boolean isHistoryPinEnabled(String gridKey) {
        return historyPinEnabledByGridKey.getOrDefault(gridKey, false);
    }

    public void putHistoryPinEnabled(String gridKey, boolean enabled) {
        historyPinEnabledByGridKey.put(gridKey, enabled);
        setDirty();
    }
}
