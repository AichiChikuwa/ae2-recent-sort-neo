package net.meatwo310.appliedaccesssort;

import appeng.api.AECapabilities;
import net.meatwo310.appliedaccesssort.net.RecentAccessPayload;
import net.meatwo310.appliedaccesssort.net.RecentPinTogglePayload;
import net.meatwo310.appliedaccesssort.config.ServerConfig;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(Constants.modId)
public class AppliedAccessSort {
    public AppliedAccessSort(IEventBus modEventBus, ModContainer modContainer) {
        AHRegistry.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::addCreativeTabContents);
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.spec);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(Constants.modId).versioned("1");
        registrar.playToClient(
                RecentAccessPayload.type,
                RecentAccessPayload.streamCodec,
                RecentAccessPayload::handle
        );
        registrar.playToServer(
                RecentPinTogglePayload.type,
                RecentPinTogglePayload.streamCodec,
                RecentPinTogglePayload::handle
        );
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        // required for adjacent AE2 nodes to discover the logger as an in-world grid host
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                AHRegistry.ME_LOGGER_BE.get(),
                (blockEntity, context) -> blockEntity
        );
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(AHRegistry.ME_LOGGER_ITEM.get());
        }
    }
}
