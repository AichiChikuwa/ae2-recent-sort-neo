package net.meatwo310.appliedaccesssort;

import net.meatwo310.appliedaccesssort.net.RecentAccessPayload;
import net.meatwo310.appliedaccesssort.net.RecentPinTogglePayload;
import net.meatwo310.appliedaccesssort.config.ServerConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(Constants.modId)
public class AppliedAccessSort {
    public AppliedAccessSort(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::registerPayloads);
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
}

