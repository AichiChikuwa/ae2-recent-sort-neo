package net.aichichikuwa.appliedhistory.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.aichichikuwa.appliedhistory.Constants;
import net.aichichikuwa.appliedhistory.sort.RecentAccessSavedData;
import net.aichichikuwa.appliedhistory.sort.ServerRecentAccessTracker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.Comparator;

@EventBusSubscriber(modid = Constants.modId)
public final class ShowUuidsCommand {
    private ShowUuidsCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("appliedhistory")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("showUUIDs")
                        .then(Commands.literal("summary").executes(ctx -> summary(ctx.getSource())))
                        .then(Commands.literal("list").executes(ctx -> list(ctx.getSource()))));
        event.getDispatcher().register(root);
    }

    private static int summary(CommandSourceStack source) {
        var level = source.getLevel();
        var uuids = collectUuids(level);
        int liveInMemory = ServerRecentAccessTracker.countLoadedKeys();
        source.sendSuccess(() -> Component.literal(
                "appliedhistory uuid summary: " + uuids.size() + " stored, " + liveInMemory + " currently loaded"), true);
        return uuids.size();
    }

    private static int list(CommandSourceStack source) {
        var uuids = collectUuids(source.getLevel());
        if (uuids.isEmpty()) {
            source.sendSuccess(() -> Component.literal("appliedhistory uuid list: (none)"), true);
            return 0;
        }
        uuids.sort(Comparator.naturalOrder());
        source.sendSuccess(() -> Component.literal("appliedhistory uuid list (" + uuids.size() + "):"), true);
        for (var id : uuids) {
            source.sendSuccess(() -> Component.literal(" - " + id), false);
        }
        return uuids.size();
    }

    private static ArrayList<String> collectUuids(ServerLevel level) {
        return new ArrayList<>(RecentAccessSavedData.get(level).keys());
    }
}
