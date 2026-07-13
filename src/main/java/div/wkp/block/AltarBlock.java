package div.wkp.block;

import com.mojang.serialization.MapCodec;
import div.wkp.PerkUtil;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class AltarBlock extends BlockWithEntity {
    public static final MapCodec<AltarBlock> CODEC = createCodec(AltarBlock::new);

    public AltarBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    // ВОТ ЭТОГО У ТЕБЯ НЕ БЫЛО - без него блок невидимый
    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new AltarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ModBlockEntities.RHO_ALTAR, AltarBlockEntity::tick);
    }

    public static boolean isComplete(World world, BlockPos pos) {
        return world.getBlockState(pos.north()).isOf(Blocks.RED_CANDLE)
                && world.getBlockState(pos.south()).isOf(Blocks.RED_CANDLE)
                && world.getBlockState(pos.east()).isOf(Blocks.RED_CANDLE)
                && world.getBlockState(pos.west()).isOf(Blocks.RED_CANDLE);
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack held, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ItemActionResult.SUCCESS;
        if (!(world.getBlockEntity(pos) instanceof AltarBlockEntity altar)) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!altar.getStack().isEmpty()) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (!isComplete(world, pos)) {
            player.sendMessage(Text.literal("Алтарь не завершён, нужны 4 красные свечи.").formatted(Formatting.RED), true);
            return ItemActionResult.SUCCESS;
        }
        if (!PerkUtil.arePerksEnabled(player)) {
            player.sendMessage(Text.literal("Вы находитесь за гранью внимания Ро.").formatted(Formatting.RED), true);
            return ItemActionResult.SUCCESS;
        }
        if (altar.getCooldownTicks() > 0) {
            player.sendMessage(Text.literal("Ро пока не склонен к сделкам, вернись через " + (altar.getCooldownTicks() / 20) + " сек.").formatted(Formatting.YELLOW), true);
            return ItemActionResult.SUCCESS;
        }
        if (held.isEmpty()) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        altar.insert(player, held);
        return ItemActionResult.SUCCESS;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(world.getBlockEntity(pos) instanceof AltarBlockEntity altar)) return ActionResult.PASS;
        if (altar.getStack().isEmpty()) return ActionResult.PASS;

        if (altar.isProcessing()) {
            player.sendMessage(Text.literal("Ро ещё обдумывает подношение...").formatted(Formatting.GOLD), true);
            return ActionResult.SUCCESS;
        }
        if (!player.getMainHandStack().isEmpty()) {
            player.sendMessage(Text.literal("Сначала освободи руку.").formatted(Formatting.RED), true);
            return ActionResult.SUCCESS;
        }
        altar.takeStack(player);
        return ActionResult.SUCCESS;
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (world.getBlockEntity(pos) instanceof AltarBlockEntity altar) {
                ItemStack stored = altar.removeStoredStack();
                if (!stored.isEmpty()) {
                    world.spawnEntity(new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stored));
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}