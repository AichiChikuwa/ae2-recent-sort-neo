package net.aichichikuwa.appliedhistory.client;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import net.aichichikuwa.appliedhistory.menu.MELoggerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MELoggerScreen extends AEBaseScreen<MELoggerMenu> {
    // second click within this window confirms the purge; otherwise it disarms
    private static final long CONFIRM_WINDOW_MILLIS = 5000L;
    private static final int WARNING_COLOR = 0xFFE04040;

    private final Button purgeButton;
    private boolean armed;
    private long armedUntilMillis;

    public MELoggerScreen(MELoggerMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.purgeButton = Button.builder(
                        Component.translatable("gui.appliedhistory.me_logger.purge"),
                        btn -> onPurgeClicked())
                .bounds(0, 0, 100, 20)
                .build();
        widgets.add("purge", this.purgeButton);
    }

    private void onPurgeClicked() {
        long now = System.currentTimeMillis();
        if (armed && now <= armedUntilMillis) {
            menu.purgeHistory();
            armed = false;
            return;
        }
        // first click only arms the confirmation and shows the warning
        armed = true;
        armedUntilMillis = now + CONFIRM_WINDOW_MILLIS;
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (armed && System.currentTimeMillis() > armedUntilMillis) {
            armed = false;
        }
        this.purgeButton.setMessage(Component.translatable(armed
                ? "gui.appliedhistory.me_logger.purge.confirm"
                : "gui.appliedhistory.me_logger.purge"));
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        int textColor = style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB();
        int wrapWidth = this.imageWidth - 16;

        // main panel line reflects the logger state
        if (menu.isBlank()) {
            drawWrapped(guiGraphics, Component.translatable("gui.appliedhistory.me_logger.blank"),
                    wrapWidth, 22, WARNING_COLOR);
        } else if (menu.isConflicted()) {
            drawWrapped(guiGraphics, Component.translatable("gui.appliedhistory.me_logger.conflict"),
                    wrapWidth, 22, WARNING_COLOR);
        } else if (menu.isOffline()) {
            drawWrapped(guiGraphics, Component.translatable("gui.appliedhistory.me_logger.offline"),
                    wrapWidth, 22, textColor);
        } else {
            guiGraphics.drawString(this.font, Component.translatable(
                            "gui.appliedhistory.me_logger.entries", menu.entryCount, menu.maxEntries),
                    8, 22, textColor, false);
        }

        // the purge warning is long, so it is drawn as a banner above the panel to avoid overflowing it
        if (armed) {
            drawWarningBanner(guiGraphics);
        }
    }

    private void drawWarningBanner(GuiGraphics guiGraphics) {
        var lines = this.font.split(Component.translatable("gui.appliedhistory.me_logger.purge.warning"),
                this.imageWidth - 8);
        int lineHeight = this.font.lineHeight + 1;
        int bannerHeight = lines.size() * lineHeight + 6;
        int top = -bannerHeight - 3;
        // translucent backdrop so the text stays readable over the world / inventory
        guiGraphics.fill(0, top, this.imageWidth, top + bannerHeight, 0xD0000000);
        int y = top + 4;
        for (var line : lines) {
            guiGraphics.drawString(this.font, line, 4, y, WARNING_COLOR, false);
            y += lineHeight;
        }
    }

    private void drawWrapped(GuiGraphics guiGraphics, Component text, int wrapWidth, int startY, int color) {
        int y = startY;
        for (var line : this.font.split(text, wrapWidth)) {
            guiGraphics.drawString(this.font, line, 8, y, color, false);
            y += this.font.lineHeight + 1;
        }
    }
}
