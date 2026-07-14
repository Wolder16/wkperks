package div.wkp.terminal;

import div.wkp.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class TerminalBlock extends Block implements BlockEntityProvider {
    public static final BooleanProperty UPPER = BooleanProperty.of("upper");

    public TerminalBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(UPPER, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(UPPER);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(UPPER) ? null : new TerminalBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();

        if (pos.getY() >= world.getTopY() - 1) {
            return null;
        }

        if (!world.getBlockState(pos.up()).canReplace(ctx)) {
            return null;
        }

        return getDefaultState().with(UPPER, false);
    }

    @Override
    public void onPlaced(
            World world,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack itemStack
    ) {
        if (!world.isClient) {
            world.setBlockState(
                    pos.up(),
                    state.with(UPPER, true),
                    Block.NOTIFY_ALL
            );
        }
    }

    @Override
    public BlockState onBreak(
            World world,
            BlockPos pos,
            BlockState state,
            PlayerEntity player
    ) {
        if (!world.isClient) {
            if (state.get(UPPER)) {
                BlockPos lowerPos = pos.down();
                BlockState lowerState = world.getBlockState(lowerPos);

                if (lowerState.isOf(this) && !lowerState.get(UPPER)) {
                    world.setBlockState(
                            lowerPos,
                            Blocks.AIR.getDefaultState(),
                            Block.NOTIFY_ALL | Block.SKIP_DROPS
                    );

                    world.syncWorldEvent(
                            player,
                            2001,
                            lowerPos,
                            Block.getRawIdFromState(lowerState)
                    );
                }
            } else {
                BlockPos upperPos = pos.up();
                BlockState upperState = world.getBlockState(upperPos);

                if (upperState.isOf(this) && upperState.get(UPPER)) {
                    world.setBlockState(
                            upperPos,
                            Blocks.AIR.getDefaultState(),
                            Block.NOTIFY_ALL | Block.SKIP_DROPS
                    );

                    world.syncWorldEvent(
                            player,
                            2001,
                            upperPos,
                            Block.getRawIdFromState(upperState)
                    );
                }
            }
        }

        return super.onBreak(world, pos, state, player);
    }

    @Override
    protected ItemActionResult onUseWithItem(
            ItemStack stack,
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            Hand hand,
            BlockHitResult hit
    ) {
        if (!stack.isOf(ModItems.FLOPPY_DISK)) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (world.isClient) {
            return ItemActionResult.SUCCESS;
        }

        BlockPos terminalPos = state.get(UPPER) ? pos.down() : pos;

        BlockEntity blockEntity = world.getBlockEntity(terminalPos);

        if (!(blockEntity instanceof TerminalBlockEntity terminal)) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        terminal.addRerollCharge(player.getUuid());

        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }

        return ItemActionResult.SUCCESS;
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

        BlockPos terminalPos = state.get(UPPER) ? pos.down() : pos;
        BlockState terminalState = world.getBlockState(terminalPos);

        if (!terminalState.isOf(this) || terminalState.get(UPPER)) {
            return ActionResult.PASS;
        }

        NamedScreenHandlerFactory factory =
                terminalState.createScreenHandlerFactory(world, terminalPos);

        if (factory != null) {
            player.openHandledScreen(factory);
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Nullable
    @Override
    public NamedScreenHandlerFactory createScreenHandlerFactory(
            BlockState state,
            World world,
            BlockPos pos
    ) {
        if (state.get(UPPER)) {
            pos = pos.down();
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);

        return blockEntity instanceof TerminalBlockEntity terminal
                ? terminal
                : null;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        if (state.get(UPPER)) {
            BlockState below = world.getBlockState(pos.down());
            return below.isOf(this) && !below.get(UPPER);
        }

        return true;
    }
}