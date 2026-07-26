package net.aichichikuwa.appliedhistory.client;

import appeng.client.gui.style.StyleManager;
import net.aichichikuwa.appliedhistory.AHRegistry;
import net.aichichikuwa.appliedhistory.Constants;
import net.aichichikuwa.appliedhistory.item.DormantMELoggerItem;
import net.aichichikuwa.appliedhistory.menu.MELoggerMenu;
import net.aichichikuwa.appliedhistory.net.DormantRitualClickPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@EventBusSubscriber(modid = Constants.modId, value = Dist.CLIENT)
public final class AHClient {
    private AHClient() {
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AHRegistry.ME_LOGGER_BE.get(), MELoggerBlockEntityRenderer::new);
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

    @SubscribeEvent
    static void onAttackInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) {
            return;
        }
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof DormantMELoggerItem)) {
            return;
        }
        // left-click-empty only fires on the client; tell the server to count the ritual step
        ClientPacketDistributor.sendToServer(new DormantRitualClickPayload());
    }
}
