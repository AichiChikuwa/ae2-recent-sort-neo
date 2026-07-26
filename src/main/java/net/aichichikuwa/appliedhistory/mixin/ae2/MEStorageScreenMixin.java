package net.aichichikuwa.appliedhistory.mixin.ae2;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.common.Repo;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.TerminalStyle;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.menu.me.common.MEStorageMenu;
import net.aichichikuwa.appliedhistory.client.RecentPinToggleButton;
import net.aichichikuwa.appliedhistory.Constants;
import net.aichichikuwa.appliedhistory.net.RecentPinTogglePayload;
import net.aichichikuwa.appliedhistory.sort.ClientRecentAccessState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
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
    private static final Identifier historyRowTexture = Identifier.fromNamespaceAndPath(
            Constants.modId,
            "textures/gui/history_row.png"
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
        var button = new RecentPinToggleButton(containerId, initialEnabled, enabled -> {
            ClientRecentAccessState.setRecentPinEnabled(containerId, enabled);
            ClientPacketDistributor.sendToServer(new RecentPinTogglePayload(containerId, enabled));
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
        if (ClientRecentAccessState.isHistoryReorderFrozen(containerId) && !Minecraft.getInstance().hasShiftDown()) {
            ClientRecentAccessState.setHistoryReorderFrozen(containerId, false);
            repo.updateView();
            adjustScrollbarForPinnedRows();
        }
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
    private void drawRecentPinnedRowsOverlay(GuiGraphicsExtractor guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        MEStorageScreen<?> screen = (MEStorageScreen<?>) (Object) this;
        int containerId = screen.getMenu().containerId;
        if (!ClientRecentAccessState.isRecentPinEnabled(containerId)) {
            return;
        }
        int historyRows = ClientRecentAccessState.getPinnedRowCount(containerId);
        if (historyRows <= 0) {
            return;
        }

        int rowPixelHeight = style.getRow().getSrcHeight();
        int slotsPerRow = style.getSlotsPerRow();
        boolean autocraft = ClientRecentAccessState.hasAutoCraftPinnedRow(containerId);
        int gridInnerWidth = slotsPerRow * 18;
        int gridLeft = offsetX + 7;
        int gridTop = offsetY + style.getHeader().getSrcHeight();
        int gridBottom = gridTop + this.rows * rowPixelHeight;

        guiGraphics.enableScissor(gridLeft, gridTop, gridLeft + gridInnerWidth, gridBottom);
        try {
            int drawnRows = 0;
            for (int r = 0; r < this.rows; r++) {
                if (autocraft && r == 0) {
                    continue;
                }
                boolean rowHasHistory = false;
                int base = autocraft ? slotsPerRow : 0;
                int mainRow = r - (autocraft ? 1 : 0);
                for (int c = 0; c < slotsPerRow; c++) {
                    int idx = base + mainRow * slotsPerRow + c;
                    var entry = this.repo.get(idx);
                    if (entry != null && ClientRecentAccessState.isRecentPinnedKey(containerId, entry.getWhat())) {
                        rowHasHistory = true;
                        break;
                    }
                }
                if (!rowHasHistory) {
                    continue;
                }
                int y = gridTop + r * rowPixelHeight;
                Blitter.texture(historyRowTexture, 162, 18)
                        .src(0, 0, 162, 18)
                        .dest(gridLeft, y, gridInnerWidth, rowPixelHeight)
                        .blit(guiGraphics);
                drawnRows++;
                if (drawnRows >= historyRows) {
                    break;
                }
            }
        } finally {
            guiGraphics.disableScissor();
        }
    }

    private void adjustScrollbarForPinnedRows() {
        int fixedRows = ClientRecentAccessState.hasAutoCraftPinnedRow(((MEStorageScreen<?>) (Object) this).getMenu().containerId) ? 1 : 0;
        int containerId = ((MEStorageScreen<?>) (Object) this).getMenu().containerId;
        int rowSize = this.style.getSlotsPerRow();
        int viewSize = ((RepoViewAccessor) (Object) this.repo).getScrollableView().size();
        int paddingSlots = ClientRecentAccessState.getRecentPriorityPaddingSlots(containerId, rowSize);
        int scrollableRows = (viewSize + paddingSlots + rowSize - 1) / rowSize;
        int totalContentRows = fixedRows + scrollableRows;
        int maxScroll = Math.max(0, totalContentRows - this.rows);
        this.scrollbar.setRange(0, maxScroll, Math.max(1, this.rows / 6));
    }

}

