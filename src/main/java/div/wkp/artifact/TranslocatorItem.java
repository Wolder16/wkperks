package div.wkp.artifact;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class TranslocatorItem extends ArtifactItem {
    private static final int ENERGY_COST = 0;
    private static final double RANGE = 15.0D;
    private static final int REQUIRED_CHARGE_TICKS = 40;

    public TranslocatorItem(Settings settings) {
        super(settings, 0, 0, 200, true);
    }

    @Override
    protected boolean isChargedUse(ItemStack stack) {
        return true;
    }

    @Override
    protected int getRequiredChargeTicks(ItemStack stack, LivingEntity user) {
        return REQUIRED_CHARGE_TICKS;
    }

    @Override
    protected void onChargedUseTick(
            World world,
            LivingEntity user,
            ItemStack stack,
            int usedTicks,
            int remainingUseTicks
    ) {
        if (!(user instanceof PlayerEntity player)) {
            return;
        }

        Vec3d target = findTeleportTarget(player);

        if (target == null) {
            return;
        }

        spawnTargetParticles(world, target, user.age);
    }

    @Override
    protected void onChargedUseReleased(
            World world,
            LivingEntity user,
            ItemStack stack,
            int usedTicks,
            int remainingUseTicks
    ) {
        if (!(user instanceof PlayerEntity player)) {
            return;
        }

        if (!hasReachedRequiredCharge(stack, user, usedTicks)) {
            return;
        }

        Vec3d target = findTeleportTarget(player);

        if (target == null) {
            if (!world.isClient) {
                player.sendMessage(
                        Text.literal("Транслокатор не нашёл точку привязки.")
                                .formatted(Formatting.RED),
                        true
                );
            }

            return;
        }

        TypedActionResult<ItemStack> result =
                finishActivation(world, player, stack, ENERGY_COST);

        if (!result.getResult().isAccepted() || world.isClient) {
            return;
        }

        Vec3d velocity = player.getVelocity();
        player.requestTeleport(target.x, target.y, target.z);
        player.setVelocity(velocity);
        player.velocityModified = true;
    }

    private static Vec3d findTeleportTarget(PlayerEntity user) {
        HitResult hitResult = user.raycast(RANGE, 0.0F, false);

        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            return null;
        }

        Direction side = blockHitResult.getSide();

        return blockHitResult.getPos().add(
                side.getOffsetX() * 0.15D,
                side.getOffsetY() * 0.15D,
                side.getOffsetZ() * 0.15D
        );
    }

    private static void spawnTargetParticles(World world, Vec3d center, int age) {
        if (!world.isClient || age % 2 != 0) {
            return;
        }

        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2.0D / 8.0D) * i + age * 0.08D;
            double x = center.x + Math.cos(angle) * 0.25D;
            double y = center.y + ((i % 2 == 0) ? 0.18D : 0.02D);
            double z = center.z + Math.sin(angle) * 0.25D;

            world.addParticle(
                    ParticleTypes.PORTAL,
                    x,
                    y,
                    z,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }

    @Override
    protected TypedActionResult<ItemStack> onArtifactUse(
            World world,
            PlayerEntity user,
            Hand hand,
            ItemStack stack
    ) {
        return TypedActionResult.pass(stack);
    }
}
