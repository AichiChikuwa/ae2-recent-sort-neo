package net.meatwo310.appliedaccesssort.menu;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import net.meatwo310.appliedaccesssort.AHRegistry;
import net.meatwo310.appliedaccesssort.block.MELoggerBlockEntity;
import net.meatwo310.appliedaccesssort.block.MELoggerStatus;
import net.minecraft.world.entity.player.Inventory;

public class MELoggerMenu extends AEBaseMenu {
    private static final String ACTION_PURGE = "purge_history";

    private final MELoggerBlockEntity logger;

    @GuiSync(0)
    public int entryCount = 0;
    @GuiSync(1)
    public int maxEntries = 0;
    @GuiSync(2)
    public MELoggerStatus status = MELoggerStatus.OFF;

    public MELoggerMenu(int id, Inventory playerInventory, MELoggerBlockEntity logger) {
        super(AHRegistry.ME_LOGGER_MENU_TYPE, id, playerInventory, logger);
        this.logger = logger;
        registerClientAction(ACTION_PURGE, this::purgeHistory);
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            this.entryCount = logger.getEntryCount();
            this.maxEntries = logger.getMaxEntries();
            this.status = logger.getStatus();
        }
        super.broadcastChanges();
    }

    public boolean isConflicted() {
        return status == MELoggerStatus.ERROR;
    }

    public boolean isOffline() {
        return status == MELoggerStatus.OFF;
    }

    // client asks the server to wipe this logger's history and pop it as a fresh item
    public void purgeHistory() {
        if (isClientSide()) {
            sendClientAction(ACTION_PURGE);
            return;
        }
        logger.purgeHistory();
        var player = getPlayer();
        if (player != null) {
            player.closeContainer();
        }
    }
}
