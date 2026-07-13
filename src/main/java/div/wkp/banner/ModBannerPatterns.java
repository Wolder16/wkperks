package div.wkp.banner;

import net.minecraft.block.entity.BannerPattern;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public final class ModBannerPatterns {
    private ModBannerPatterns() {}

    public static final RegistryKey<BannerPattern> RHO_SYMBOL =
            RegistryKey.of(
                    RegistryKeys.BANNER_PATTERN,
                    Identifier.of("wkperks", "rho_symbol")
            );

    public static final TagKey<BannerPattern> RHO_BANNER_PATTERN_ITEM =
            TagKey.of(
                    RegistryKeys.BANNER_PATTERN,
                    Identifier.of("wkperks", "pattern_item/rho_banner_pattern")
            );
}