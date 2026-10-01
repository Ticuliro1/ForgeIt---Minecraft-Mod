package com.ticuliro.forgeit;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.attachment.AttachmentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.ticuliro.forgeit.economy.*;

@Mod(ForgeIt.MOD_ID)
public final class ForgeIt {
    public static final String MOD_ID = "forgeit";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Long>> WALLET = ATTACHMENTS.register("wallet",
        () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG.validate(value -> value >= 0 && value <= Wallet.MAX_BALANCE ? com.mojang.serialization.DataResult.success(value) : com.mojang.serialization.DataResult.error(() -> "Invalid wallet balance"))).copyOnDeath().build());
    public static final DeferredRegister<MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>> LOOT_MODIFIERS =
        DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID);
    public static final java.util.function.Supplier<MapCodec<CoinLootModifier>> COIN_LOOT = LOOT_MODIFIERS.register("mob_coins", () -> CoinLootModifier.CODEC);

    public static final DeferredBlock<ReforgingBlock> REFORGING_TABLE = BLOCKS.register("reforging_table",
        () -> new ReforgingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
            .strength(3.5F).sound(SoundType.ANVIL).requiresCorrectToolForDrops().noOcclusion()));
    // Legacy ID is hidden, preserved only to migrate currency from existing worlds.
    public static final DeferredItem<Item> COIN = ITEMS.registerSimpleItem("coin", new Item.Properties());
    public static final DeferredItem<Item> COPPER_COIN = ITEMS.registerSimpleItem("copper_coin", new Item.Properties());
    public static final DeferredItem<Item> SILVER_COIN = ITEMS.registerSimpleItem("silver_coin", new Item.Properties());
    public static final DeferredItem<Item> GOLD_COIN = ITEMS.registerSimpleItem("gold_coin", new Item.Properties());
    public static final DeferredItem<Item> PLATINUM_COIN = ITEMS.registerSimpleItem("platinum_coin", new Item.Properties());
    public static final DeferredItem<net.minecraft.world.item.BlockItem> TABLE_ITEM = ITEMS.registerSimpleBlockItem(REFORGING_TABLE);
    public static final DeferredHolder<MenuType<?>, MenuType<ReforgingMenu>> REFORGING_MENU = MENUS.register("reforging",
        () -> new MenuType<>(ReforgingMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("forgeit",
        () -> CreativeModeTab.builder().title(Component.literal("ForgeIt!"))
            .icon(() -> TABLE_ITEM.toStack()).displayItems((parameters, output) -> {
                output.accept(TABLE_ITEM.get());
                for (Coin coin : Coin.values()) output.accept(coin.item());
            }).build());

    public ForgeIt(IEventBus modBus, ModContainer container) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        MENUS.register(modBus);
        TABS.register(modBus);
        ATTACHMENTS.register(modBus);
        LOOT_MODIFIERS.register(modBus);
        modBus.addListener(WalletNetwork::register);
        container.registerConfig(ModConfig.Type.SERVER, ForgeItConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(ForgeItEvents::attributes);
        NeoForge.EVENT_BUS.addListener(CoinDrops::dragon);
        NeoForge.EVENT_BUS.register(WalletEvents.class);
        NeoForge.EVENT_BUS.addListener(ForgeItEvents::tooltip);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
