package net.aichichikuwa.appliedhistory.mixin.ae2;

import appeng.client.api.AEKeyRendering;
import appeng.api.stacks.AEKey;
import appeng.client.gui.me.common.MEStorageScreen;
import net.aichichikuwa.appliedhistory.sort.ClientRecentAccessState;
import net.aichichikuwa.appliedhistory.sort.RecentTooltipFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = AEKeyRendering.class, remap = false)
public class AEKeyRenderingMixin {
    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    private static void appendRecentHistoryLine(AEKey stack, CallbackInfoReturnable<List<Component>> cir) {
        var screen = Minecraft.getInstance().screen;
        if (!(screen instanceof MEStorageScreen<?> meStorageScreen)) {
            return;
        }
        int containerId = meStorageScreen.getMenu().containerId;
        if (!ClientRecentAccessState.isRecentPinEnabled(containerId)) {
            return;
        }
        if (!ClientRecentAccessState.isRecentPinnedKey(containerId, stack)) {
            return;
        }
        var info = ClientRecentAccessState.getDetails(containerId, stack);
        if (info == null) {
            return;
        }
        var tooltip = cir.getReturnValue();
        tooltip.add(RecentTooltipFormatter.buildSentence(info, System.currentTimeMillis())
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        cir.setReturnValue(tooltip);
    }
}
