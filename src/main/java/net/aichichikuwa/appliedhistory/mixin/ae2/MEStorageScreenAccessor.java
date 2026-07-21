package net.aichichikuwa.appliedhistory.mixin.ae2;

import appeng.client.gui.me.common.MEStorageScreen;
import appeng.client.gui.me.common.Repo;
import appeng.client.gui.style.TerminalStyle;
import appeng.client.gui.widgets.Scrollbar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = MEStorageScreen.class, remap = false)
public interface MEStorageScreenAccessor {
    @Accessor("rows")
    int getTerminalGridRows();

    @Accessor("style")
    TerminalStyle getTerminalStyle();

    @Accessor("repo")
    Repo getRepo();

    @Accessor("scrollbar")
    Scrollbar getScrollbar();
}
