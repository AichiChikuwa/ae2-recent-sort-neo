package net.aichichikuwa.appliedhistory.mixin.ae2;

import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.AEKey;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.menu.me.crafting.CraftConfirmMenu;
import net.aichichikuwa.appliedhistory.config.ServerConfig;
import net.aichichikuwa.appliedhistory.sort.RecentInteractionAction;
import net.aichichikuwa.appliedhistory.sort.ServerRecentAccessTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftConfirmMenu.class, remap = false)
public abstract class CraftConfirmMenuMixin {
    @Shadow
    private AEKey whatToCraft;

    @Inject(method = "planJob", at = @At("RETURN"))
    private void trackCraftRequest(AEKey what, int amount, CalculationStrategy strategy, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        if (!(((CraftConfirmMenu) (Object) this).getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        var target = ((CraftConfirmMenu) (Object) this).getTarget();
        if (!(target instanceof IActionHost actionHost)) {
            return;
        }
        var gridNode = actionHost.getActionableNode();
        if (gridNode == null || gridNode.getGrid() == null) {
            return;
        }

        var trackedKey = this.whatToCraft != null ? this.whatToCraft : what;
        if (trackedKey == null) {
            return;
        }

        debugChat(player, "craftConfirm planJob amount=" + amount + " key=" + trackedKey + " strategy=" + strategy);
        ServerRecentAccessTracker.markInteraction(
                gridNode,
                trackedKey,
                player.getGameProfile().name(),
                RecentInteractionAction.requested);
        debugChat(player, "craftConfirm classify=requested key=" + trackedKey);
    }

    @Unique
    private void debugChat(ServerPlayer player, String message) {
        if (!ServerConfig.debugChat.get()) {
            return;
        }
        player.sendSystemMessage(Component.literal("[aas-debug] " + message));
    }
}
