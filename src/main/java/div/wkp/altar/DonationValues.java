package div.wkp.altar;

import div.wkp.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;

import java.util.Set;

public final class DonationValues {
    private static final Set<Block> RESOURCE_BLOCKS = Set.of(
            Blocks.COAL_BLOCK,
            Blocks.IRON_BLOCK,
            Blocks.GOLD_BLOCK,
            Blocks.DIAMOND_BLOCK,
            Blocks.EMERALD_BLOCK,
            Blocks.LAPIS_BLOCK,
            Blocks.REDSTONE_BLOCK
    );

    private static final Set<Block> RAW_ORE_BLOCKS = Set.of(
            Blocks.RAW_IRON_BLOCK,
            Blocks.RAW_GOLD_BLOCK,
            Blocks.RAW_COPPER_BLOCK
    );

    private DonationValues() {}

    public static int getValue(ItemStack stack) {
        if (stack.isOf(Items.ENDER_EYE)) return 2;
        if (stack.isOf(Items.SHULKER_SHELL)) return 2;
        if (stack.isOf(ModItems.RHO_GIFT)) return 10;

        Block block = Block.getBlockFromItem(stack.getItem());
        if (block == Blocks.AIR) return 0;

        if (block == Blocks.NETHERITE_BLOCK) return 30;
        if (RAW_ORE_BLOCKS.contains(block)) return -1;
        if (isCopperBlock(block)) return 1;

        BlockState state = block.getDefaultState();
        if (state.isIn(BlockTags.DIRT) || state.isIn(BlockTags.BASE_STONE_OVERWORLD) || state.isIn(BlockTags.BASE_STONE_NETHER)) {
            return -1;
        }

        if (RESOURCE_BLOCKS.contains(block)) return 2;

        return 0;
    }

    private static boolean isCopperBlock(Block block) {
        return block == Blocks.COPPER_BLOCK || block == Blocks.EXPOSED_COPPER || block == Blocks.WEATHERED_COPPER || block == Blocks.OXIDIZED_COPPER
                || block == Blocks.WAXED_COPPER_BLOCK || block == Blocks.WAXED_EXPOSED_COPPER || block == Blocks.WAXED_WEATHERED_COPPER || block == Blocks.WAXED_OXIDIZED_COPPER;
    }
}