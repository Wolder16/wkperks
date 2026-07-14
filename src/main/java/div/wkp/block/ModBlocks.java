package div.wkp.block;

import div.wkp.WKPerks;
import div.wkp.terminal.TerminalBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    private ModBlocks() {}

    public static final Block RHO_ALTAR = registerBlock(
            "rho_altar",
            new AltarBlock(
                    AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
                            .strength(4.0F)
                            .requiresTool()
            )
    );
    public static final Block TERMINAL = registerBlock(
            "terminal",
            new TerminalBlock(
                    AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
                            .strength(3.0F)
                            .requiresTool()
            )
    );

    private static Block registerBlock(String id, Block block) {
        Identifier identifier = Identifier.of(WKPerks.MOD_ID, id);
        Registry.register(Registries.BLOCK, identifier, block);
        Registry.register(Registries.ITEM, identifier, new BlockItem(block, new Item.Settings()));
        return block;
    }

    public static void initialize() {
        WKPerks.LOGGER.info("Registering WKPerks blocks");
    }
}