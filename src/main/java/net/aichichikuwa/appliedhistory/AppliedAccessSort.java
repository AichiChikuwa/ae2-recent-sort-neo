package net.aichichikuwa.appliedhistory;

import appeng.api.AECapabilities;
import net.aichichikuwa.appliedhistory.net.DormantRitualClickPayload;
import net.aichichikuwa.appliedhistory.net.RecentAccessPayload;
import net.aichichikuwa.appliedhistory.net.RecentPinTogglePayload;
import net.aichichikuwa.appliedhistory.config.ServerConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(Constants.modId)
public class AppliedAccessSort {
    public AppliedAccessSort(IEventBus modEventBus, ModContainer modContainer) {
        AHRegistry.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::registerCapabilities);
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
        registrar.playToServer(
                DormantRitualClickPayload.type,
                DormantRitualClickPayload.streamCodec,
                DormantRitualClickPayload::handle
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
}
