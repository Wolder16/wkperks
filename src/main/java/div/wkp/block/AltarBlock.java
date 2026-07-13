package div.wkp.block;

import com.mojang.serialization.MapCodec;
import div.wkp.PerkUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.Hand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;

public class AltarBlock extends BlockWithEntity {
    public AltarBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return createCodec(AltarBlock::new);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new AltarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world,
            BlockState state,
            net.minecraft.block.entity.BlockEntityType<T> type
    ) {
        return world.isClient
                ? null
                : validateTicker(
                type,
                ModBlockEntities.RHO_ALTAR,
                AltarBlockEntity::tick
        );
    }

    public static boolean isComplete(World world, BlockPos pos) {
        return world.getBlockState(pos.north()).isOf(Blocks.RED_CANDLE)
                && world.getBlockState(pos.south()).isOf(Blocks.RED_CANDLE)
                && world.getBlockState(pos.east()).isOf(Blocks.RED_CANDLE)
                && world.getBlockState(pos.west()).isOf(Blocks.RED_CANDLE);
    }

    @Override
    protected ActionResult onUse(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit
    ) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        if (!(world.getBlockEntity(pos) instanceof AltarBlockEntity altar)) {
            return ActionResult.PASS;
        }

        /*
         * Сначала позволяем забрать уже обработанный результат.
         */
        if (!altar.getStack().isEmpty()) {
            if (altar.isProcessing()) {
                player.sendMessage(
                        Text.literal("Ро ещё обдумывает подношение...")
                                .formatted(Formatting.GOLD),
                        true
                );
                return ActionResult.SUCCESS;
            }

            if (player.getStackInHand(Hand.MAIN_HAND).isEmpty()) {
                altar.takeStack(player);
            } else {
                player.sendMessage(
                        Text.literal("Сначала освободи руку.")
                                .formatted(Formatting.RED),
                        true
                );
            }

            return ActionResult.SUCCESS;
        }

        if (!isComplete(world, pos)) {
            player.sendMessage(
                    Text.literal(
                            "Алтарь требует четыре красные свечи по сторонам."
                    ).formatted(Formatting.RED),
                    true
            );
            return ActionResult.SUCCESS;
        }

        if (!PerkUtil.arePerksEnabled(player)) {
            player.sendMessage(
                    Text.literal(
                            "Ро не принимает подношения в этом режиме."
                    ).formatted(Formatting.RED),
                    true
            );
            return ActionResult.SUCCESS;
        }

        if (altar.getCooldownTicks() > 0) {
            int seconds = (altar.getCooldownTicks() + 19) / 20;

            player.sendMessage(
                    Text.literal("Алтарь перезаряжается: " + seconds + " сек.")
                            .formatted(Formatting.YELLOW),
                    true
            );
            return ActionResult.SUCCESS;
        }

        ItemStack held = player.getStackInHand(Hand.MAIN_HAND);

        if (held.isEmpty()) {
            player.sendMessage(
                    Text.literal("В руке должен быть предмет для подношения.")
                            .formatted(Formatting.GRAY),
                    true
            );
            return ActionResult.SUCCESS;
        }

        altar.insert(player, held);
        return ActionResult.SUCCESS;
    }
    @Override
    protected void onStateReplaced(
            BlockState state,
            World world,
            BlockPos pos,
            BlockState newState,
            boolean moved
    ) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof AltarBlockEntity altar) {
                ItemStack stored = altar.removeStoredStack();

                if (!stored.isEmpty()) {
                    ItemEntity itemEntity = new ItemEntity(
                            world,
                            pos.getX() + 0.5,
                            pos.getY() + 1.0,
                            pos.getZ() + 0.5,
                            stored
                    );

                    world.spawnEntity(itemEntity);
                }
            }
        }

        super.onStateReplaced(state, world, pos, newState, moved);
    }
}