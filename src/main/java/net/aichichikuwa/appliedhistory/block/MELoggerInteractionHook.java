package net.aichichikuwa.appliedhistory.block;

import appeng.hooks.WrenchHook;
import net.aichichikuwa.appliedhistory.Constants;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// forwards ae2 wrench interactions on bounding segments to the main logger block below
@EventBusSubscriber(modid = Constants.modId)
public final class MELoggerInteractionHook {
    private MELoggerInteractionHook() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.isCanceled()) {
            return;
        }
        if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof MELoggerBoundingBlock)) {
            return;
        }

        var mainPos = MELoggerBoundingBlock.getMainPos(event.getLevel(), event.getPos());
        if (mainPos == null) {
            return;
        }

        var redirectedHit = MELoggerBoundingBlock.redirectHit(event.getHitVec(), event.getPos(), mainPos);
        var result = WrenchHook.onPlayerUseBlock(
                event.getEntity(),
                event.getLevel(),
                InteractionHand.MAIN_HAND,
                redirectedHit);
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }
}
