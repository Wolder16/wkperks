package div.wkp.terminal;

import div.wkp.block.ModBlockEntities;
import net.minecraft.text.Text;
import div.wkp.item.ModItems;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient && state.get(UPPER)) {
            BlockPos lowerPos = pos.down();
            BlockState lowerState = world.getBlockState(lowerPos);
            if (lowerState.isOf(this) && !lowerState.get(UPPER)) {
                world.setBlockState(lowerPos, Blocks.AIR.getDefaultState(), 35);
                world.syncWorldEvent(player, 2001, lowerPos, Block.getRawIdFromState(lowerState));
            }
        }
        return super.onBreak(world, pos, state, player);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        // Если кликнули по верхней части — переключаемся на нижнюю
        BlockPos targetPos = state.get(UPPER) ? pos.down() : pos;
        BlockState targetState = world.getBlockState(targetPos);

        ItemStack held = player.getMainHandStack();

        // Использование дискеты для добавления заряда реролла
        if (held.isOf(ModItems.FLOPPY_DISK)) {
            BlockEntity be = world.getBlockEntity(targetPos);
            if (be instanceof TerminalBlockEntity entity) {
                entity.addRerollCharge(player.getUuid());
                held.decrement(1);
                return ActionResult.SUCCESS;
            }
        }

        // Открытие GUI
        NamedScreenHandlerFactory factory = targetState.createScreenHandlerFactory(world, targetPos);
        if (factory != null) {
            player.openHandledScreen(factory);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public @Nullable NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
        if (state.get(UPPER)) {
            pos = pos.down();
        }
        BlockEntity be = world.getBlockEntity(pos);
        return be instanceof TerminalBlockEntity ? (TerminalBlockEntity) be : null;
    }
}