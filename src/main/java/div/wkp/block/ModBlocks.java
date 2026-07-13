package div.wkp.block;

import div.wkp.WKPerks;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.BlockSoundGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    private ModBlocks() {
    }

    public static final Block RHO_ALTAR = Registry.register(
            Registries.BLOCK,
            Identifier.of(WKPerks.MOD_ID, "rho_altar"),
            new AltarBlock(
                    AbstractBlock.Settings.create()
                            .strength(4.0f)
                            .requiresTool()
                            .sounds(BlockSoundGroup.METAL)
            )
    );

    public static void initialize() {
        WKPerks.LOGGER.info("Registering WKPerks blocks");
    }
}