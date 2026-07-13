package div.wkp.item;

import div.wkp.WKPerks;
import div.wkp.banner.ModBannerPatterns;
import net.minecraft.item.BannerPatternItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
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
        WKPerks.LOGGER.info("Registering WKPerks items");
    }
}