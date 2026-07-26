package net.aichichikuwa.appliedhistory.client;

import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.IconButton;
import appeng.util.Icon;
import net.aichichikuwa.appliedhistory.Constants;
import net.aichichikuwa.appliedhistory.sort.ClientRecentAccessState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public class RecentPinToggleButton extends IconButton {
    private static final Identifier historyToggleTexture = Identifier.fromNamespaceAndPath(
            Constants.modId,
            "textures/gui/history_toggle.png"
    );
    private final int containerId;
    private boolean enabled;
    private final ToggleHandler onToggle;

    @FunctionalInterface
    public interface ToggleHandler {
        void onToggle(boolean enabled);
    }

    public RecentPinToggleButton(int containerId, boolean initialEnabled, ToggleHandler onToggle) {
        super(btn -> ((RecentPinToggleButton) btn).toggle());
        this.containerId = containerId;
        this.enabled = initialEnabled;
        this.onToggle = onToggle;
    }

    private boolean hasLogger() {
        return ClientRecentAccessState.hasLogger(containerId);
    }

    private boolean hasConflict() {
        return ClientRecentAccessState.hasLoggerConflict(containerId);
    }

    public boolean isEnabledState() {
        return enabled;
    }

    public void setEnabledState(boolean enabled) {
        this.enabled = enabled;
    }

    private void toggle() {
        // the toggle is inert until a single, active me logger exists in the network
        if (!hasLogger()) {
            return;
        }
        this.enabled = !this.enabled;
        this.onToggle.onToggle(this.enabled);
    }

    @Override
    protected Icon getIcon() {
        return enabled ? Icon.LOCKED : Icon.UNLOCKED;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partial) {
        if (!this.visible) {
            return;
        }
        var yOffset = isHovered() ? 1 : 0;
        Icon bgIcon = isHovered() ? Icon.TOOLBAR_BUTTON_BACKGROUND_HOVER
                : isFocused() ? Icon.TOOLBAR_BUTTON_BACKGROUND_FOCUS : Icon.TOOLBAR_BUTTON_BACKGROUND;
        Blitter.icon(bgIcon)
                .dest(getX() - 1, getY() + yOffset, 18, 20)
                .blit(guiGraphics);

        int srcX = enabled ? 0 : 16;
        Blitter.texture(historyToggleTexture, 32, 16)
                .src(srcX, 0, 16, 16)
                .dest(getX(), getY() + 1 + yOffset)
                .blit(guiGraphics);
    }

    @Override
    public List<Component> getTooltipMessage() {
        if (hasConflict()) {
            return List.of(
                    Component.translatable("tooltip.appliedhistory.history"),
                    Component.translatable("tooltip.appliedhistory.toggle.conflict"));
        }
        if (!hasLogger()) {
            return List.of(
                    Component.translatable("tooltip.appliedhistory.history"),
                    Component.translatable("tooltip.appliedhistory.toggle.noLogger"));
        }
        return List.of(
                Component.translatable("tooltip.appliedhistory.history"),
                Component.translatable(enabled
                        ? "tooltip.appliedhistory.toggle.enabled"
                        : "tooltip.appliedhistory.toggle.disabled"));
    }
}
