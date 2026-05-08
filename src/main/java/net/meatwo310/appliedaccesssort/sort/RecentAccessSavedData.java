package net.meatwo310.appliedaccesssort.sort;

import appeng.api.stacks.AEKey;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public final class RecentAccessSavedData extends SavedData {
    private static final String name = "appliedhistory_recent_access";

    public static RecentAccessSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        RecentAccessSavedData::new,
                        RecentAccessSavedData::load,
                        null),
                name
        );
    }

    private static RecentAccessSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new RecentAccessSavedData();
        var grids = tag.getList("grids", Tag.TAG_COMPOUND);
        for (int i = 0; i < grids.size(); i++) {
            var gridTag = grids.getCompound(i);
            var gridKey = gridTag.getString("gridKey");
            var sequence = gridTag.getLong("sequence");
            data.sequenceByGridKey.put(gridKey, sequence);
            data.historyPinEnabledByGridKey.put(gridKey, gridTag.getBoolean("historyPinEnabled"));

            var details = new HashMap<AEKey, RecentInteractionInfo>();
            var detailList = gridTag.getList("details", Tag.TAG_COMPOUND);
            for (int j = 0; j < detailList.size(); j++) {
                var detailTag = detailList.getCompound(j);
                if (!detailTag.contains("key", Tag.TAG_COMPOUND)) {
                    continue;
                }
                var key = AEKey.fromTagGeneric(registries, detailTag.getCompound("key"));
                if (key == null) {
                    continue;
                }
                var info = new RecentInteractionInfo(
                        detailTag.getLong("seq"),
                        detailTag.getLong("time"),
                        detailTag.getString("player"),
                        RecentInteractionAction.valueOf(detailTag.getString("action"))
                );
                details.put(key, info);
            }
            data.detailsByGridKey.put(gridKey, details);
        }
        return data;
    }

    private final Map<String, HashMap<AEKey, RecentInteractionInfo>> detailsByGridKey = new HashMap<>();
    private final Map<String, Long> sequenceByGridKey = new HashMap<>();
    private final Map<String, Boolean> historyPinEnabledByGridKey = new HashMap<>();

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

    public boolean isHistoryPinEnabled(String gridKey) {
        return historyPinEnabledByGridKey.getOrDefault(gridKey, false);
    }

    public void putHistoryPinEnabled(String gridKey, boolean enabled) {
        historyPinEnabledByGridKey.put(gridKey, enabled);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var grids = new ListTag();
        for (var entry : detailsByGridKey.entrySet()) {
            var gridTag = new CompoundTag();
            gridTag.putString("gridKey", entry.getKey());
            gridTag.putLong("sequence", sequenceByGridKey.getOrDefault(entry.getKey(), 0L));
            gridTag.putBoolean("historyPinEnabled", historyPinEnabledByGridKey.getOrDefault(entry.getKey(), false));

            var detailList = new ListTag();
            for (var detailEntry : entry.getValue().entrySet()) {
                var detailTag = new CompoundTag();
                detailTag.put("key", detailEntry.getKey().toTagGeneric(registries));
                detailTag.putLong("seq", detailEntry.getValue().sequence());
                detailTag.putLong("time", detailEntry.getValue().timestampMillis());
                detailTag.putString("player", detailEntry.getValue().playerName());
                detailTag.putString("action", detailEntry.getValue().action().name());
                detailList.add(detailTag);
            }
            gridTag.put("details", detailList);
            grids.add(gridTag);
        }
        tag.put("grids", grids);
        return tag;
    }
}
