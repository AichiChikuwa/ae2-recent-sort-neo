package net.aichichikuwa.appliedhistory.item;

import net.aichichikuwa.appliedhistory.Constants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = Constants.modId)
public final class DormantLoggerHandler {
    private static final TransformInput[] SEQUENCE = {
            TransformInput.CROUCH,
            TransformInput.CROUCH,
            TransformInput.LEFT_CLICK,
            TransformInput.LEFT_CLICK,
            TransformInput.CROUCH
    };

    private static final Map<UUID, Boolean> wasCrouching = new HashMap<>();
    private static final Map<UUID, Long> lastLeftClickTick = new HashMap<>();

    private DormantLoggerHandler() {
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        LoggerAdvancements.onLunaticItemReceived(player, event.getCrafting());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (player.level().isClientSide()) {
            trackClientCrouch(player);
            return;
        }

        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof DormantMELoggerItem)) {
            wasCrouching.remove(player.getUUID());
            return;
        }

        var level = (ServerLevel) player.level();
        long gameTime = level.getGameTime();
        int progress = DormantMELoggerItem.getProgress(stack);

        if (progress >= DormantMELoggerItem.MAX_PROGRESS) {
            if (player instanceof ServerPlayer serverPlayer) {
                handleCompletion(level, serverPlayer, stack, gameTime);
            }
            return;
        }

        if (progress > 0) {
            long idle = gameTime - DormantMELoggerItem.getLastActionTick(stack);
            if (idle > DormantMELoggerItem.ROLLBACK_DELAY_TICKS) {
                int rollbackSteps = (int) ((idle - DormantMELoggerItem.ROLLBACK_DELAY_TICKS)
                        / DormantMELoggerItem.ROLLBACK_STEP_TICKS) + 1;
                int newProgress = Math.max(0, progress - rollbackSteps);
                if (newProgress != progress) {
                    DormantMELoggerItem.setProgress(stack, newProgress);
                }
            }
        }

        boolean crouching = player.isCrouching();
        boolean previouslyCrouching = wasCrouching.getOrDefault(player.getUUID(), false);
        if (crouching && !previouslyCrouching) {
            tryAdvance(player, stack, TransformInput.CROUCH, gameTime);
        }
        wasCrouching.put(player.getUUID(), crouching);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        handleLeftClick(event.getEntity());
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        handleLeftClick(event.getEntity());
    }

    public static void handleLeftClick(Player player) {
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof DormantMELoggerItem)) {
            return;
        }
        long gameTime = player.level().getGameTime();
        if (lastLeftClickTick.getOrDefault(player.getUUID(), -1L) == gameTime) {
            return;
        }
        lastLeftClickTick.put(player.getUUID(), gameTime);
        tryAdvance(player, stack, TransformInput.LEFT_CLICK, gameTime);
    }

    private static void tryAdvance(Player player, ItemStack stack, TransformInput input, long gameTime) {
        int progress = DormantMELoggerItem.getProgress(stack);
        if (progress >= DormantMELoggerItem.MAX_PROGRESS) {
            return;
        }

        if (SEQUENCE[progress] != input) {
            if (progress > 0) {
                DormantMELoggerItem.setProgress(stack, 0);
                DormantMELoggerItem.setLastActionTick(stack, gameTime);
                DormantMELoggerItem.setCompleteTick(stack, -1L);
                DormantMELoggerItem.setConduitPlayed(stack, false);
            }
            return;
        }

        progress++;
        DormantMELoggerItem.setProgress(stack, progress);
        DormantMELoggerItem.setLastActionTick(stack, gameTime);

        var level = player.level();
        if (input == TransformInput.CROUCH) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 0.8f, 1.0f);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.8f, 1.0f);
        }

        if (progress >= DormantMELoggerItem.MAX_PROGRESS) {
            DormantMELoggerItem.setCompleteTick(stack, gameTime);
            DormantMELoggerItem.setConduitPlayed(stack, false);
        }
    }

    private static void handleCompletion(ServerLevel level, ServerPlayer player, ItemStack stack, long gameTime) {
        long completeTick = DormantMELoggerItem.getCompleteTick(stack);
        if (completeTick < 0) {
            return;
        }

        long elapsed = gameTime - completeTick;
        if (elapsed >= DormantMELoggerItem.COMPLETION_CONDUIT_DELAY_TICKS
                && !DormantMELoggerItem.isConduitPlayed(stack)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 0.9f, 1.0f);
            DormantMELoggerItem.setConduitPlayed(stack, true);
        }

        if (elapsed >= DormantMELoggerItem.COMPLETION_TRANSFORM_DELAY_TICKS) {
            boolean lunatic = MELoggerItems.isLunaticOrigin(stack);
            var logger = MELoggerItems.createReadyLogger(UUID.randomUUID(), lunatic);
            player.setItemInHand(InteractionHand.MAIN_HAND, logger);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FROG_DEATH, SoundSource.PLAYERS, 0.7f, 0.8f);
            LoggerAdvancements.grantObtainLogger(player);
            LoggerAdvancements.onLunaticItemReceived(player, logger);
        }
    }

    private static void trackClientCrouch(Player player) {
        wasCrouching.put(player.getUUID(), player.isCrouching());
    }

    private enum TransformInput {
        CROUCH,
        LEFT_CLICK
    }
}
