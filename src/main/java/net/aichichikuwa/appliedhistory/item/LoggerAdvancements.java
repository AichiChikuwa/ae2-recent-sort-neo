package net.aichichikuwa.appliedhistory.item;

import net.aichichikuwa.appliedhistory.Constants;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class LoggerAdvancements {
    private LoggerAdvancements() {
    }

    public static void grantObtainLogger(ServerPlayer player) {
        award(player, "obtain_me_logger", "has_me_logger");
    }

    public static void grantLunaticDormant(ServerPlayer player) {
        award(player, "obtain_lunatic_dormant", "has_lunatic_dormant");
    }

    public static void onLunaticItemReceived(ServerPlayer player, ItemStack stack) {
        if (MELoggerItems.isLunaticOrigin(stack)) {
            grantLunaticDormant(player);
        }
    }

    private static void award(ServerPlayer player, String id, String criterion) {
        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(
                Identifier.fromNamespaceAndPath(Constants.modId, id));
        if (advancement != null) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
