package div.wkp.block;

import div.wkp.PerkComponents;
import div.wkp.PerkUtil;
import div.wkp.altar.AltarRegistry;
import div.wkp.altar.AltarResult;
import div.wkp.component.PerkComponent;
import div.wkp.altar.offerings.DonationOffering;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

public class AltarBlockEntity extends BlockEntity {
    private static final int PROCESS_TIME = 60;
    private static final int COOLDOWN_TIME = 1200;

    private ItemStack stack = ItemStack.EMPTY;

    private int processingTicks = 0;
    private int cooldownTicks = 0;

    private UUID ownerUuid;

    public AltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RHO_ALTAR, pos, state);
    }

    public static void tick(
            World world,
            BlockPos pos,
            BlockState state,
            AltarBlockEntity altar
    ) {
        if (world.isClient) {
            return;
        }

        if (altar.processingTicks > 0) {
            altar.processingTicks--;

            if (altar.processingTicks <= 0) {
                altar.completeOffering((ServerWorld) world);
            }

            return;
        }

        if (altar.cooldownTicks > 0) {
            altar.cooldownTicks--;
            altar.markDirty();
        }
    }

    public void insert(PlayerEntity player, ItemStack source) {
        if (!stack.isEmpty() || processingTicks > 0 || cooldownTicks > 0) {
            return;
        }

        stack = source.copy();
        stack.setCount(1);

        source.decrement(1);

        ownerUuid = player.getUuid();
        processingTicks = PROCESS_TIME;

        markDirty();
        updateListeners();
    }

    public void takeStack(PlayerEntity player) {
        if (stack.isEmpty() || processingTicks > 0) {
            return;
        }

        ItemStack result = stack.copy();
        stack = ItemStack.EMPTY;

        if (!player.giveItemStack(result)) {
            player.dropItem(result, false);
        }

        markDirty();
        updateListeners();
    }

    private void completeOffering(ServerWorld world) {
        if (ownerUuid == null) {
            clearInput();
            return;
        }

        ServerPlayerEntity owner =
                world.getServer()
                        .getPlayerManager()
                        .getPlayer(ownerUuid);

        /*
         * Если игрок временно вышел, предмет остаётся на алтаре,
         * а обработка повторится через следующий тик.
         */
        if (owner == null || !PerkUtil.arePerksEnabled(owner)) {
            processingTicks = 1;
            return;
        }

        PerkComponent component =
                PerkComponents.PERK_COMPONENT.get(owner);

        int currentGrace =
                component.getPerkLevel("rho_grace");

        AltarResult result =
                AltarRegistry.resolve(stack, currentGrace);

        component.changePerkLevel(
                "rho_grace",
                result.graceChange()
        );

        stack = result.output().copy();
        processingTicks = 0;
        cooldownTicks = COOLDOWN_TIME;
        ownerUuid = null;

        world.spawnParticles(
                ParticleTypes.ENCHANT,
                getPos().getX() + 0.5,
                getPos().getY() + 1.1,
                getPos().getZ() + 0.5,
                25,
                0.35,
                0.45,
                0.35,
                0.15
        );

        markDirty();
        updateListeners();

        owner.sendMessage(
                net.minecraft.text.Text.literal("Ро принял подношение.")
                        .formatted(net.minecraft.util.Formatting.GOLD),
                true
        );
    }

    private void clearInput() {
        stack = ItemStack.EMPTY;
        processingTicks = 0;
        ownerUuid = null;
        markDirty();
        updateListeners();
    }

    private void updateListeners() {
        if (world != null) {
            world.updateListeners(
                    pos,
                    getCachedState(),
                    getCachedState(),
                    3
            );
        }
    }

    public ItemStack getStack() {
        return stack;
    }

    public boolean isProcessing() {
        return processingTicks > 0;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public ItemStack removeStoredStack() {
        ItemStack result = stack;
        stack = ItemStack.EMPTY;
        return result;
    }

    @Override
    protected void writeNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup lookup
    ) {
        super.writeNbt(nbt, lookup);

        if (!stack.isEmpty()) {
            nbt.put("Item", stack.encode(lookup));
        }

        nbt.putInt("ProcessingTicks", processingTicks);
        nbt.putInt("CooldownTicks", cooldownTicks);

        if (ownerUuid != null) {
            nbt.putUuid("Owner", ownerUuid);
        }
    }

    @Override
    protected void readNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup lookup
    ) {
        super.readNbt(nbt, lookup);

        if (nbt.contains("Item")) {
            stack = ItemStack.fromNbt(
                    lookup,
                    nbt.getCompound("Item")
            ).orElse(ItemStack.EMPTY);
        } else {
            stack = ItemStack.EMPTY;
        }

        processingTicks = nbt.getInt("ProcessingTicks");
        cooldownTicks = nbt.getInt("CooldownTicks");

        if (nbt.containsUuid("Owner")) {
            ownerUuid = nbt.getUuid("Owner");
        }
    }
}