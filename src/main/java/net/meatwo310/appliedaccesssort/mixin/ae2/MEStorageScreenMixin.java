package net.meatwo310.appliedaccesssort.mixin.ae2;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.common.Repo;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.TerminalStyle;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.menu.me.common.MEStorageMenu;
import net.meatwo310.appliedaccesssort.client.RecentPinToggleButton;
import net.meatwo310.appliedaccesssort.Constants;
import net.meatwo310.appliedaccesssort.net.RecentPinTogglePayload;
import net.meatwo310.appliedaccesssort.sort.ClientRecentAccessState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MEStorageScreen.class, remap = false)
public abstract class MEStorageScreenMixin {
    @Unique
    private static final ResourceLocation historyRowTexture = ResourceLocation.fromNamespaceAndPath(
            Constants.modId,
            "history_row.png"
    );
    @Shadow
    @Final
    private TerminalStyle style;
    @Shadow
    @Final
    protected Repo repo;
    @Shadow
    private SettingToggleButton<?> sortByToggle;
    @Shadow
    private Scrollbar scrollbar;
    @Shadow
    private int rows;
    @Unique
    private RecentPinToggleButton recentPinToggleButton;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectRecentPinButton(MEStorageMenu menu, Inventory playerInventory, Component title, ScreenStyle screenStyle, CallbackInfo ci) {
        if (!style.isSortable()) {
            return;
        }

        var toolbar = ((AEBaseScreenAccessor) this).getVerticalToolbar();
        var buttons = ((VerticalButtonBarAccessor) toolbar).getButtons();
        var containerId = menu.containerId;
        var initialEnabled = ClientRecentAccessState.isRecentPinEnabled(containerId);
        var button = new RecentPinToggleButton(initialEnabled, enabled -> {
            ClientRecentAccessState.setRecentPinEnabled(containerId, enabled);
            PacketDistributor.sendToServer(new RecentPinTogglePayload(containerId, enabled));
            repo.updateView();
            adjustScrollbarForPinnedRows();
        });
        this.recentPinToggleButton = button;

        int index = Math.max(0, buttons.indexOf(sortByToggle));
        buttons.add(Math.min(buttons.size(), index + 1), button);
    }

    @Inject(method = "onMenuReceivedClientUpdate", at = @At("TAIL"))
    private void syncRepoState(CallbackInfo ci) {
        adjustScrollbarForPinnedRows();
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void refreshOnHistoryUpdate(CallbackInfo ci) {
        MEStorageScreen<?> screen = (MEStorageScreen<?>) (Object) this;
        int containerId = screen.getMenu().containerId;
        if (recentPinToggleButton != null) {
            var latestEnabled = ClientRecentAccessState.isRecentPinEnabled(containerId);
            if (recentPinToggleButton.isEnabledState() != latestEnabled) {
                recentPinToggleButton.setEnabledState(latestEnabled);
            }
        }
        if (ClientRecentAccessState.consumeDirty(containerId)) {
            repo.updateView();
            adjustScrollbarForPinnedRows();
        }
    }

    @Inject(method = "updateScrollbar", at = @At("TAIL"))
    private void patchScrollbarForPinnedRows(CallbackInfo ci) {
        adjustScrollbarForPinnedRows();
    }

    @Inject(method = "drawBG", at = @At("TAIL"))
    private void drawRecentPinnedRowsOverlay(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        MEStorageScreen<?> screen = (MEStorageScreen<?>) (Object) this;
        int containerId = screen.getMenu().containerId;
        int recentRows = ClientRecentAccessState.getPinnedRowCount(containerId);
        if (recentRows <= 0) {
            return;
        }

        int firstRecentRowIndex = ClientRecentAccessState.hasAutoCraftPinnedRow(containerId) ? 1 : 0;
        int startY = offsetY + style.getHeader().getSrcHeight() + (firstRecentRowIndex * style.getRow().getSrcHeight());
        for (int i = 0; i < recentRows; i++) {
            Blitter.texture(historyRowTexture, 162, 18)
                    .src(0, 0, 162, 18)
                    .dest(offsetX + 7, startY + i * style.getRow().getSrcHeight())
                    .blit(guiGraphics);
        }
    }

    private void adjustScrollbarForPinnedRows() {
        MEStorageScreen<?> screen = (MEStorageScreen<?>) (Object) this;
        var containerId = screen.getMenu().containerId;
        int fixedRows = ClientRecentAccessState.getPinnedRowCount(containerId);

        if (ClientRecentAccessState.hasAutoCraftPinnedRow(containerId)) {
            fixedRows += 1;
        }

        int totalRows = (this.repo.size() + 8) / 9;
        this.scrollbar.setRange(0, Math.max(0, totalRows - this.rows - fixedRows), Math.max(1, this.rows / 6));
    }

}

