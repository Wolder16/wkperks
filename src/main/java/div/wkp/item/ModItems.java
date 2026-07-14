package div.wkp.item;

import div.wkp.WKPerks;
import div.wkp.banner.ModBannerPatterns;
import div.wkp.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.BannerPatternItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItems {
    public static final RegistryKey<ItemGroup> WKPERKS_ITEM_GROUP_KEY =
            RegistryKey.of(
                    RegistryKeys.ITEM_GROUP,
                    Identifier.of(WKPerks.MOD_ID, "main")
            );

    public static final ItemGroup WKPERKS_ITEM_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModBlocks.TERMINAL))
            .displayName(Text.translatable("itemGroup.wkperks.main"))
            .build();

    private ModItems() {
    }

    public static final Item RHO_GIFT = register(
            "rho_gift",
            new Item(new Item.Settings())
    );

    public static final Item FLOPPY_DISK = register(
            "floppy_disk",
            new Item(new Item.Settings())
    );

    public static final Item GOLD_ROACH = register(
            "gold_roach",
            new Item(new Item.Settings())
    );

    public static final Item RHO_BANNER_PATTERN = register(
            "rho_banner_pattern",
            new BannerPatternItem(
                    ModBannerPatterns.RHO_BANNER_PATTERN_ITEM,
                    new Item.Settings().maxCount(1)
            )
    );
    private static Item register(String id, Item item) {
        return Registry.register(
                Registries.ITEM,
                Identifier.of(WKPerks.MOD_ID, id),
                item
        );
    }

    public static void initialize() {
        Registry.register(
                Registries.ITEM_GROUP,
                WKPERKS_ITEM_GROUP_KEY,
                WKPERKS_ITEM_GROUP
        );

        ItemGroupEvents.modifyEntriesEvent(WKPERKS_ITEM_GROUP_KEY).register(entries -> {
            entries.add(ModBlocks.TERMINAL);
            entries.add(ModBlocks.RHO_ALTAR);
            entries.add(FLOPPY_DISK);
            entries.add(GOLD_ROACH);
            entries.add(RHO_GIFT);
            entries.add(RHO_BANNER_PATTERN);
        });

        WKPerks.LOGGER.info("Registering WKPerks items");
    }
}
