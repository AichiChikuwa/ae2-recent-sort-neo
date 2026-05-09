package net.meatwo310.appliedaccesssort.sort;

import appeng.api.stacks.AEKey;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.util.datafix.DataFixTypes;

import java.util.HashMap;
import java.util.Map;

public final class RecentAccessSavedData extends SavedData {
    private static final Codec<RecentAccessSavedData> codec = Codec.EMPTY.xmap(
            unit -> new RecentAccessSavedData(),
            data -> Unit.INSTANCE).codec();
    private static final SavedDataType<RecentAccessSavedData> type = new SavedDataType<RecentAccessSavedData>(
            Identifier.fromNamespaceAndPath("appliedhistory", "recent_access"),
            RecentAccessSavedData::new,
            codec,
            DataFixTypes.LEVEL
    );

    public static RecentAccessSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(type);
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
}
