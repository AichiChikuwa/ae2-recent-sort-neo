package net.meatwo310.appliedaccesssort.client;

import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.IconButton;
import net.meatwo310.appliedaccesssort.Constants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class RecentPinToggleButton extends IconButton {
    private static final ResourceLocation historyToggleTexture = ResourceLocation.fromNamespaceAndPath(
            Constants.modId,
            "history_toggle.png"
    );
    private boolean enabled;
    private final ToggleHandler onToggle;

    @FunctionalInterface
    public interface ToggleHandler {
        void onToggle(boolean enabled);
    }

    public RecentPinToggleButton(boolean initialEnabled, ToggleHandler onToggle) {
        super(btn -> ((RecentPinToggleButton) btn).toggle());
        this.enabled = initialEnabled;
        this.onToggle = onToggle;
    }

    public boolean isEnabledState() {
        return enabled;
    }

    public void setEnabledState(boolean enabled) {
        this.enabled = enabled;
    }

    private void toggle() {
        this.enabled = !this.enabled;
        this.onToggle.onToggle(this.enabled);
    }

    @Override
    protected Icon getIcon() {
        return enabled ? Icon.LOCKED : Icon.UNLOCKED;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        if (!this.visible) {
            return;
        }
        var yOffset = isHovered() ? 1 : 0;
        Icon bgIcon = isHovered() ? Icon.TOOLBAR_BUTTON_BACKGROUND_HOVER
                : isFocused() ? Icon.TOOLBAR_BUTTON_BACKGROUND_FOCUS : Icon.TOOLBAR_BUTTON_BACKGROUND;
        bgIcon.getBlitter()
                .dest(getX() - 1, getY() + yOffset, 18, 20)
                .zOffset(2)
                .blit(guiGraphics);

        int srcX = enabled ? 0 : 16;
        Blitter.texture(historyToggleTexture, 32, 16)
                .src(srcX, 0, 16, 16)
                .dest(getX(), getY() + 1 + yOffset)
                .zOffset(3)
                .blit(guiGraphics);
    }

    @Override
    public List<Component> getTooltipMessage() {
        return List.of(
                Component.translatable("tooltip.appliedhistory.history"),
                Component.translatable(enabled
                        ? "tooltip.appliedhistory.toggle.enabled"
                        : "tooltip.appliedhistory.toggle.disabled"));
    }
}
