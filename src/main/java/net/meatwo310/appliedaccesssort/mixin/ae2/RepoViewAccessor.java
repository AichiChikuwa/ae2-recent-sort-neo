package net.meatwo310.appliedaccesssort.mixin.ae2;

import appeng.client.gui.me.common.Repo;
import appeng.menu.me.common.GridInventoryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.ArrayList;

@Mixin(value = Repo.class, remap = false)
public interface RepoViewAccessor {
    @Accessor("view")
    ArrayList<GridInventoryEntry> getScrollableView();
}
