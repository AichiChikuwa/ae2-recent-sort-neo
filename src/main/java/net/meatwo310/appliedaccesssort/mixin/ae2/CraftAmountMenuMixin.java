package net.meatwo310.appliedaccesssort.mixin.ae2;

import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.AEKey;
import appeng.menu.me.crafting.CraftAmountMenu;
import net.meatwo310.appliedaccesssort.config.ServerConfig;
import net.meatwo310.appliedaccesssort.sort.RecentInteractionAction;
import net.meatwo310.appliedaccesssort.sort.ServerRecentAccessTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftAmountMenu.class, remap = false)
public abstract class CraftAmountMenuMixin {
    @Shadow
    private AEKey whatToCraft;

    @Inject(method = "confirm", at = @At("HEAD"))
    private void trackRequestOnConfirm(int amount, boolean craftMissingAmount, boolean autoStart, CallbackInfo ci) {
        if (amount <= 0) {
            return;
        }
        if (!(((CraftAmountMenu) (Object) this).getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        debugChat(player, "craftAmount confirm amount=" + amount + " key=" + (whatToCraft == null ? "null" : whatToCraft));

        var target = ((CraftAmountMenu) (Object) this).getTarget();
        if (!(target instanceof IActionHost actionHost)) {
            return;
        }
        var gridNode = actionHost.getActionableNode();
        if (gridNode == null || gridNode.getGrid() == null) {
            return;
        }
        if (whatToCraft == null) {
            return;
        }

        ServerRecentAccessTracker.markInteraction(
                gridNode,
                whatToCraft,
                player.getGameProfile().name(),
                RecentInteractionAction.requested);
        debugChat(player, "craftAmount classify=requested key=" + whatToCraft);
    }

    @Unique
    private void debugChat(ServerPlayer player, String message) {
        if (!ServerConfig.debugChat.get()) {
            return;
        }
        player.sendSystemMessage(Component.literal("[aas-debug] " + message));
    }
}
