package net.meatwo310.appliedaccesssort.client;

import appeng.client.gui.style.StyleManager;
import net.meatwo310.appliedaccesssort.AHRegistry;
import net.meatwo310.appliedaccesssort.Constants;
import net.meatwo310.appliedaccesssort.menu.MELoggerMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Constants.modId, value = Dist.CLIENT)
public final class AHClient {
    private AHClient() {
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        // style json is resolved under the ae2 namespace by StyleManager, so it lives there with a mod-prefixed name.
        // the factory is explicitly typed so the M/U type variables of register(...) can be inferred.
        MenuScreens.ScreenConstructor<MELoggerMenu, MELoggerScreen> factory =
                (menu, inventory, title) -> new MELoggerScreen(menu, inventory, title,
                        StyleManager.loadStyleDoc("/screens/appliedhistory_me_logger.json"));
        event.register(AHRegistry.ME_LOGGER_MENU_TYPE, factory);
    }
}
