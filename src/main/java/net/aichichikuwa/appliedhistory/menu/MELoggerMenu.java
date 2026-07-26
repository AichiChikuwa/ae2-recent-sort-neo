package net.aichichikuwa.appliedhistory.menu;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.ClientActionKey;
import appeng.menu.guisync.GuiSync;
import net.aichichikuwa.appliedhistory.AHRegistry;
import net.aichichikuwa.appliedhistory.block.MELoggerBlockEntity;
import net.aichichikuwa.appliedhistory.block.MELoggerStatus;
import net.minecraft.world.entity.player.Inventory;

public class MELoggerMenu extends AEBaseMenu {
    private static final ClientActionKey<Void> ACTION_PURGE = new ClientActionKey<>("purge_history");

    private final MELoggerBlockEntity logger;

    @GuiSync(0)
    public int entryCount = 0;
    @GuiSync(1)
    public int maxEntries = 0;
    @GuiSync(2)
    public MELoggerStatus status = MELoggerStatus.OFF;
    @GuiSync(3)
    public boolean blank = false;

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
            this.blank = logger.isBlank();
        }
        super.broadcastChanges();
    }

    public boolean isBlank() {
        return blank;
    }

    public boolean isConflicted() {
        return !blank && status == MELoggerStatus.ERROR;
    }

    public boolean isOffline() {
        return !blank && status == MELoggerStatus.OFF;
    }

    // client asks the server to wipe this logger's history and pop it as a dormant item
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
