package net.meatwo310.appliedaccesssort.mixin.ae2;

import appeng.api.networking.IGridNode;
import appeng.api.storage.MEStorage;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.helpers.InventoryAction;
import appeng.menu.me.common.MEStorageMenu;
import net.meatwo310.appliedaccesssort.net.RecentAccessPayload;
import net.meatwo310.appliedaccesssort.sort.RecentInteractionAction;
import net.meatwo310.appliedaccesssort.sort.ServerRecentAccessTracker;
import net.meatwo310.appliedaccesssort.config.ServerConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(value = MEStorageMenu.class, remap = false)
public abstract class MEStorageMenuMixin {
    @Shadow
    @Nullable
    public abstract IGridNode getGridNode();
    @Shadow
    protected MEStorage storage;

    @Unique
    private final Map<AEKey, Long> trackedBeforeAmounts = new HashMap<>();
    @Unique
    private InventoryAction trackedAction;
    @Unique
    private AEKey carriedBeforeKey;
    @Unique
    private int carriedBeforeCount;
    @Unique
    private AEKey putCarriedBeforeKey;
    @Unique
    private int putCarriedBeforeCount;
    @Unique
    private boolean freezeReorderPulse;

    @Inject(method = "handleNetworkInteraction", at = @At("HEAD"))
    private void captureBeforeInteraction(ServerPlayer player, @Nullable AEKey clickedKey, InventoryAction action, CallbackInfo ci) {
        try {
            trackedBeforeAmounts.clear();
            trackedAction = action;
            carriedBeforeKey = null;
            carriedBeforeCount = 0;
            debugChat(player, "head action=" + action + " clicked=" + debugKey(clickedKey));

            if (storage == null) {
                return;
            }

            if (action == InventoryAction.AUTO_CRAFT) {
                if (clickedKey != null) {
                    trackedBeforeAmounts.put(clickedKey, readAvailableAmount(clickedKey));
                }
                return;
            }

            Set<AEKey> candidates = new HashSet<>();
            if (clickedKey != null) {
                candidates.add(clickedKey);
            }
            var carriedBefore = AEItemKey.of(((MEStorageMenu) (Object) this).getCarried());
            if (carriedBefore != null) {
                candidates.add(carriedBefore);
                carriedBeforeKey = carriedBefore;
                carriedBeforeCount = ((MEStorageMenu) (Object) this).getCarried().getCount();
                debugChat(player, "head carriedBefore=" + debugKey(carriedBeforeKey) + " count=" + carriedBeforeCount);
            }
            for (var key : candidates) {
                trackedBeforeAmounts.put(key, readAvailableAmount(key));
                debugChat(player, "head amountBefore " + debugKey(key) + "=" + readAvailableAmount(key));
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "handleNetworkInteraction", at = @At("TAIL"))
    private void trackInteraction(ServerPlayer player, @Nullable AEKey clickedKey, InventoryAction action, CallbackInfo ci) {
        try {
            debugChat(player, "tail action=" + trackedAction + " clicked=" + debugKey(clickedKey));
            if (action == InventoryAction.SHIFT_CLICK && clickedKey != null) {
                // shift-click exporting from terminal grid should freeze client row reorder until shift is released
                freezeReorderPulse = true;
            }
            if (storage == null) {
                trackedBeforeAmounts.clear();
                return;
            }
            if (trackedAction == InventoryAction.AUTO_CRAFT) {
                if (clickedKey != null) {
                    debugChat(player, "tail classify=requested key=" + debugKey(clickedKey));
                    sendTrackingUpdate(player, clickedKey, RecentInteractionAction.requested);
                }
                trackedBeforeAmounts.clear();
                return;
            }

            if (clickedKey == null
                    && (trackedAction == InventoryAction.PICKUP_OR_SET_DOWN
                    || trackedAction == InventoryAction.SPLIT_OR_PLACE_SINGLE
                    || trackedAction == InventoryAction.ROLL_DOWN)) {
                // insert tracking for carried-item placement is handled in putCarriedItemIntoNetwork
                trackedBeforeAmounts.clear();
                return;
            }

            boolean trackedByDelta = false;
            for (var entry : trackedBeforeAmounts.entrySet()) {
                var key = entry.getKey();
                long before = entry.getValue();
                long after = readAvailableAmount(key);
                debugChat(player, "tail delta key=" + debugKey(key) + " before=" + before + " after=" + after);
                if (after > before) {
                    debugChat(player, "tail classify=inserted key=" + debugKey(key));
                    sendTrackingUpdate(player, key, RecentInteractionAction.inserted);
                    trackedByDelta = true;
                } else if (after < before) {
                    debugChat(player, "tail classify=extracted key=" + debugKey(key));
                    sendTrackingUpdate(player, key, RecentInteractionAction.extracted);
                    trackedByDelta = true;
                }
            }
            if (!trackedByDelta) {
                var fallbackAction = classifyWithoutDelta(clickedKey, action);
                if (fallbackAction != null && clickedKey != null) {
                    debugChat(player, "tail classify=fallback " + fallbackAction + " key=" + debugKey(clickedKey));
                    sendTrackingUpdate(player, clickedKey, fallbackAction);
                }
            }
            trackedBeforeAmounts.clear();
        } catch (Throwable ignored) {
            trackedBeforeAmounts.clear();
        }
    }

    @Inject(method = "transferStackToMenu", at = @At("RETURN"))
    private void trackShiftInsert(ItemStack input, CallbackInfoReturnable<Integer> cir) {
        if (!(((MEStorageMenu) (Object) this).getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (cir.getReturnValueI() <= 0) {
            return;
        }
        var key = AEItemKey.of(input);
        if (key == null) {
            return;
        }
        sendTrackingUpdate(serverPlayer, key, RecentInteractionAction.inserted);
    }

    @Inject(method = "broadcastChanges", at = @At("TAIL"))
    private void syncRecentAccessState(CallbackInfo ci) {
        if (!(((MEStorageMenu) (Object) this).getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        var gridNode = getGridNode();
        if (gridNode == null) {
            return;
        }
        var grid = gridNode.getGrid();
        if (grid == null) {
            return;
        }

        var history = ServerRecentAccessTracker.snapshotHistory(gridNode);
        var details = ServerRecentAccessTracker.snapshotDetails(gridNode);
        var payload = new RecentAccessPayload(
                serverPlayer.containerMenu.containerId,
                history,
                details,
                ServerRecentAccessTracker.isHistoryPinEnabled(gridNode),
                Math.max(1, ServerConfig.historyRows.get()),
                consumeFreezeReorderPulse()
        );
        PacketDistributor.sendToPlayer(serverPlayer, payload);
    }

    private void sendTrackingUpdate(ServerPlayer player, AEKey key, RecentInteractionAction action) {
        var gridNode = getGridNode();
        if (gridNode == null) {
            debugChat(player, "track skipped reason=noGridNode action=" + action + " key=" + debugKey(key));
            return;
        }
        if (gridNode.getGrid() == null) {
            debugChat(player, "track skipped reason=noGrid action=" + action + " key=" + debugKey(key));
            return;
        }

        debugChat(player, "track send action=" + action + " key=" + debugKey(key));
        ServerRecentAccessTracker.markInteraction(gridNode, key, player.getGameProfile().name(), action);
        var payload = new RecentAccessPayload(
                player.containerMenu.containerId,
                ServerRecentAccessTracker.snapshotHistory(gridNode),
                ServerRecentAccessTracker.snapshotDetails(gridNode),
                ServerRecentAccessTracker.isHistoryPinEnabled(gridNode),
                Math.max(1, ServerConfig.historyRows.get()),
                consumeFreezeReorderPulse()
        );
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Unique
    private long readAvailableAmount(AEKey key) {
        if (storage == null || key == null) {
            return 0L;
        }
        return storage.getAvailableStacks().get(key);
    }

    @Inject(method = "putCarriedItemIntoNetwork", at = @At("HEAD"))
    private void capturePutCarriedBefore(boolean singleItem, CallbackInfo ci) {
        putCarriedBeforeKey = AEItemKey.of(((MEStorageMenu) (Object) this).getCarried());
        putCarriedBeforeCount = ((MEStorageMenu) (Object) this).getCarried().getCount();
    }

    @Inject(method = "putCarriedItemIntoNetwork", at = @At("TAIL"))
    private void trackPutCarriedAfter(boolean singleItem, CallbackInfo ci) {
        if (!(((MEStorageMenu) (Object) this).getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        int carriedAfterCount = ((MEStorageMenu) (Object) this).getCarried().getCount();
        debugChat(player, "putCarried single=" + singleItem
                + " key=" + debugKey(putCarriedBeforeKey)
                + " before=" + putCarriedBeforeCount
                + " after=" + carriedAfterCount);
        if (putCarriedBeforeKey != null && carriedAfterCount < putCarriedBeforeCount) {
            debugChat(player, "putCarried classify=inserted key=" + debugKey(putCarriedBeforeKey));
            sendTrackingUpdate(player, putCarriedBeforeKey, RecentInteractionAction.inserted);
        }
    }

    @Unique
    private void debugChat(ServerPlayer player, String message) {
        if (!ServerConfig.debugChat.get()) {
            return;
        }
        player.sendSystemMessage(Component.literal("[aas-debug] " + message));
    }

    @Unique
    private String debugKey(@Nullable AEKey key) {
        return key == null ? "null" : key.toString();
    }

    @Unique
    private boolean consumeFreezeReorderPulse() {
        boolean pulse = freezeReorderPulse;
        freezeReorderPulse = false;
        return pulse;
    }

    @Unique
    private @Nullable RecentInteractionAction classifyWithoutDelta(@Nullable AEKey clickedKey, InventoryAction action) {
        if (clickedKey == null) {
            return null;
        }
        var carriedAfterKey = AEItemKey.of(((MEStorageMenu) (Object) this).getCarried());
        int carriedAfterCount = ((MEStorageMenu) (Object) this).getCarried().getCount();

        if (action == InventoryAction.SHIFT_CLICK) {
            return RecentInteractionAction.extracted;
        }

        if (action == InventoryAction.PICKUP_OR_SET_DOWN
                || action == InventoryAction.SPLIT_OR_PLACE_SINGLE
                || action == InventoryAction.ROLL_DOWN) {
            if (carriedBeforeKey != null && carriedBeforeKey.equals(clickedKey)) {
                if (carriedAfterCount > carriedBeforeCount) {
                    return RecentInteractionAction.extracted;
                }
                if (carriedAfterCount < carriedBeforeCount) {
                    return RecentInteractionAction.inserted;
                }
            } else if (carriedBeforeKey == null && carriedAfterKey != null && carriedAfterKey.equals(clickedKey)) {
                if (carriedAfterCount > 0) {
                    return RecentInteractionAction.extracted;
                }
            }
        }

        return null;
    }
}

