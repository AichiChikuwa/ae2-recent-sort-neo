package net.aichichikuwa.appliedhistory;

import appeng.blockentity.AEBaseBlockEntity;
import appeng.menu.implementations.MenuTypeBuilder;
import net.aichichikuwa.appliedhistory.block.MELoggerBlock;
import net.aichichikuwa.appliedhistory.block.MELoggerBlockEntity;
import net.aichichikuwa.appliedhistory.block.MELoggerBoundingBlock;
import net.aichichikuwa.appliedhistory.block.MELoggerBoundingBlockEntity;
import net.aichichikuwa.appliedhistory.item.DormantMELoggerItem;
import net.aichichikuwa.appliedhistory.item.MELoggerBlockItem;
import net.aichichikuwa.appliedhistory.item.MELoggerItems;
import net.aichichikuwa.appliedhistory.menu.MELoggerMenu;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class AHRegistry {
    private AHRegistry() {
    }

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.modId);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.modId);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.modId);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Constants.modId);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Constants.modId);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Constants.modId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> HISTORY_ID =
            DATA_COMPONENTS.register("history_id", () -> DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TRANSFORM_PROGRESS =
            DATA_COMPONENTS.register("transform_progress", () -> DataComponentType.<Integer>builder()
                    .persistent(com.mojang.serialization.Codec.INT)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.INT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> TRANSFORM_LAST_ACTION_TICK =
            DATA_COMPONENTS.register("transform_last_action_tick", () -> DataComponentType.<Long>builder()
                    .persistent(com.mojang.serialization.Codec.LONG)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_LONG)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> TRANSFORM_COMPLETE_TICK =
            DATA_COMPONENTS.register("transform_complete_tick", () -> DataComponentType.<Long>builder()
                    .persistent(com.mojang.serialization.Codec.LONG)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_LONG)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> TRANSFORM_CONDUIT_PLAYED =
            DATA_COMPONENTS.register("transform_conduit_played", () -> DataComponentType.<Boolean>builder()
                    .persistent(com.mojang.serialization.Codec.BOOL)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LUNATIC_ORIGIN =
            DATA_COMPONENTS.register("lunatic_origin", () -> DataComponentType.<Boolean>builder()
                    .persistent(com.mojang.serialization.Codec.BOOL)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.BOOL)
                    .build());

    private static final AtomicReference<BlockEntityType<MELoggerBoundingBlockEntity>> meLoggerBoundingBeType =
            new AtomicReference<>();

    public static final DeferredBlock<MELoggerBoundingBlock> ME_LOGGER_BOUNDING =
            BLOCKS.register("me_logger_bounding", () -> new MELoggerBoundingBlock(
                    meLoggerBoundingBeType::get,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.2f, 11.0f)
                            .sound(SoundType.METAL)
                            .forceSolidOn()
                            .noOcclusion()
                            .dynamicShape()));

    public static final DeferredBlock<MELoggerBlock> ME_LOGGER =
            BLOCKS.register("me_logger", () -> new MELoggerBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.2f, 11.0f)
                            .sound(SoundType.METAL)
                            .forceSolidOn()
                            .noOcclusion()
                            .dynamicShape(),
                    ME_LOGGER_BOUNDING));

    public static final DeferredItem<MELoggerBlockItem> ME_LOGGER_ITEM =
            ITEMS.register("me_logger", () -> new MELoggerBlockItem(
                    ME_LOGGER.get(),
                    new Item.Properties()));

    public static final DeferredItem<DormantMELoggerItem> DORMANT_ME_LOGGER =
            ITEMS.register("dormant_me_logger", () -> new DormantMELoggerItem(
                    new Item.Properties().stacksTo(1)));

    public static final Supplier<BlockEntityType<MELoggerBlockEntity>> ME_LOGGER_BE =
            BLOCK_ENTITIES.register("me_logger", () -> {
                var typeHolder = new AtomicReference<BlockEntityType<MELoggerBlockEntity>>();
                BlockEntityType.BlockEntitySupplier<MELoggerBlockEntity> supplier =
                        (pos, state) -> new MELoggerBlockEntity(typeHolder.get(), pos, state);
                var type = BlockEntityType.Builder.of(supplier, ME_LOGGER.get()).build(null);
                typeHolder.set(type);
                // wiring required by AEBaseEntityBlock (normally done by AE2's AEBlockEntities.create)
                AEBaseBlockEntity.registerBlockEntityItem(type, ME_LOGGER_ITEM.get());
                BlockEntityTicker<MELoggerBlockEntity> serverTicker =
                        (level, pos, state, be) -> be.serverTick();
                ME_LOGGER.get().setBlockEntity(MELoggerBlockEntity.class, type, null, serverTicker);
                return type;
            });

    public static final Supplier<BlockEntityType<MELoggerBoundingBlockEntity>> ME_LOGGER_BOUNDING_BE =
            BLOCK_ENTITIES.register("me_logger_bounding", () -> {
                var typeHolder = new AtomicReference<BlockEntityType<MELoggerBoundingBlockEntity>>();
                BlockEntityType.BlockEntitySupplier<MELoggerBoundingBlockEntity> supplier =
                        (pos, state) -> new MELoggerBoundingBlockEntity(typeHolder.get(), pos, state);
                var type = BlockEntityType.Builder.of(supplier, ME_LOGGER_BOUNDING.get()).build(null);
                typeHolder.set(type);
                meLoggerBoundingBeType.set(type);
                return type;
            });

    // built with AE2's MenuTypeBuilder so host resolution + the menu opener are wired for us;
    // buildUnregistered gives back a MenuType we can register through our own DeferredRegister
    public static final MenuType<MELoggerMenu> ME_LOGGER_MENU_TYPE =
            MenuTypeBuilder.create(MELoggerMenu::new, MELoggerBlockEntity.class)
                    .buildUnregistered(ResourceLocation.fromNamespaceAndPath(Constants.modId, "me_logger"));

    public static final DeferredHolder<MenuType<?>, MenuType<MELoggerMenu>> ME_LOGGER_MENU =
            MENUS.register("me_logger", () -> ME_LOGGER_MENU_TYPE);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.appliedhistory"))
                    .icon(() -> new ItemStack(ME_LOGGER_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(DORMANT_ME_LOGGER.get());
                        output.accept(MELoggerItems.createDormantLogger(true));
                        output.accept(new ItemStack(ME_LOGGER_ITEM.get()));
                    })
                    .build());

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
