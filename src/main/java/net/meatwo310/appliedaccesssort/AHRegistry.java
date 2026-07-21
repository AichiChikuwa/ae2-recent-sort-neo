package net.meatwo310.appliedaccesssort;

import appeng.blockentity.AEBaseBlockEntity;
import appeng.menu.implementations.MenuTypeBuilder;
import net.meatwo310.appliedaccesssort.block.MELoggerBlock;
import net.meatwo310.appliedaccesssort.block.MELoggerBlockEntity;
import net.meatwo310.appliedaccesssort.menu.MELoggerMenu;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> HISTORY_ID =
            DATA_COMPONENTS.register("history_id", () -> DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC)
                    .build());

    public static final DeferredBlock<MELoggerBlock> ME_LOGGER =
            BLOCKS.register("me_logger", () -> new MELoggerBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.2f, 11.0f)
                            .sound(SoundType.METAL)));

    public static final DeferredItem<BlockItem> ME_LOGGER_ITEM =
            ITEMS.registerSimpleBlockItem(ME_LOGGER);

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

    // built with AE2's MenuTypeBuilder so host resolution + the menu opener are wired for us;
    // buildUnregistered gives back a MenuType we can register through our own DeferredRegister
    public static final MenuType<MELoggerMenu> ME_LOGGER_MENU_TYPE =
            MenuTypeBuilder.create(MELoggerMenu::new, MELoggerBlockEntity.class)
                    .buildUnregistered(ResourceLocation.fromNamespaceAndPath(Constants.modId, "me_logger"));

    public static final DeferredHolder<MenuType<?>, MenuType<MELoggerMenu>> ME_LOGGER_MENU =
            MENUS.register("me_logger", () -> ME_LOGGER_MENU_TYPE);

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
    }
}
