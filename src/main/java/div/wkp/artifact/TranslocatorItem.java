package div.wkp.artifact;

import div.wkp.component.ArtifactStateComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

public class TranslocatorItem extends ArtifactItem {
    private static final double RANGE = 15.0D;
    private static final int CHARGE_TICKS = 10;
    private static final DustParticleEffect TARGET_PARTICLE =
            new DustParticleEffect(new Vector3f(0.92F, 0.12F, 0.14F), 1.15F);

    public TranslocatorItem(Settings settings) {
        super(settings, 0, 0, 200, true);
    }

    @Override
    public void onUseReleased(
            ServerPlayerEntity player,
            Hand hand,
            ItemStack stack,
            ArtifactStateComponent component
    ) {
        if (component.getUseTicks() < CHARGE_TICKS) {
            return;
        }

        Vec3d target = findTeleportTarget(player);

        if (target == null) {
            player.sendMessage(
                    Text.literal("Транслокатор не нашёл точку привязки.")
                            .formatted(Formatting.RED),
                    true
            );
            return;
        }

        if (!finishActivation(player.getWorld(), player, stack, 0)
                .getResult()
                .isAccepted()) {
            return;
        }

        Vec3d velocity = player.getVelocity();
        player.requestTeleport(target.x, target.y, target.z);
        player.setVelocity(velocity);
        player.velocityModified = true;
    }

    public static void spawnClientPreview(PlayerEntity player, int age) {
        Vec3d target = findTeleportTarget(player);

        if (target == null) {
            return;
        }

        spawnTargetParticles(player.getWorld(), target, age);
    }

    private static Vec3d findTeleportTarget(PlayerEntity user) {
        HitResult hitResult = user.raycast(RANGE, 0.0F, false);

        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            return null;
        }

        Direction side = blockHitResult.getSide();
        Vec3d hitPos = blockHitResult.getPos();

        Vec3d baseTarget;

        if (side.getAxis().isHorizontal()) {
            double offset = Math.max(user.getWidth() * 0.5D + 0.1D, 0.4D);
            baseTarget = new Vec3d(
                    hitPos.x + side.getOffsetX() * offset,
                    user.getY(),
                    hitPos.z + side.getOffsetZ() * offset
            );
        } else if (side == Direction.UP) {
            baseTarget = new Vec3d(
                    hitPos.x,
                    blockHitResult.getBlockPos().getY() + 1.01D,
                    hitPos.z
            );
        } else {
            baseTarget = new Vec3d(
                    hitPos.x,
                    blockHitResult.getBlockPos().getY() - user.getHeight() - 0.01D,
                    hitPos.z
            );
        }

        return findSafeTeleportPosition(user, baseTarget);
    }

    private static Vec3d findSafeTeleportPosition(PlayerEntity user, Vec3d baseTarget) {
        Box currentBox = user.getBoundingBox();
        double offsetX = baseTarget.x - user.getX();
        double offsetZ = baseTarget.z - user.getZ();

        double[] verticalOffsets = {0.0D, 0.25D, 0.5D, 0.75D, 1.0D};

        for (double verticalOffset : verticalOffsets) {
            double candidateY = baseTarget.y + verticalOffset;
            Box candidateBox = currentBox.offset(
                    offsetX,
                    candidateY - user.getY(),
                    offsetZ
            );

            if (user.getWorld().isSpaceEmpty(user, candidateBox)) {
                return new Vec3d(baseTarget.x, candidateY, baseTarget.z);
            }
        }

        return null;
    }

    private static void spawnTargetParticles(World world, Vec3d center, int age) {
        if (!world.isClient || age % 2 != 0) {
            return;
        }

        int points = 24;
        double radius = 0.33D;
        double rotation = age * 0.08D;
        double goldenAngle = Math.PI * (3.0D - Math.sqrt(5.0D));

        for (int i = 0; i < points; i++) {
            double t = (i + 0.5D) / points;
            double y = 1.0D - 2.0D * t;
            double horizontalRadius = Math.sqrt(1.0D - y * y);
            double theta = goldenAngle * i + rotation;

            double x = Math.cos(theta) * horizontalRadius;
            double z = Math.sin(theta) * horizontalRadius;

            world.addImportantParticle(
                    TARGET_PARTICLE,
                    true,
                    center.x + x * radius,
                    center.y + y * radius,
                    center.z + z * radius,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            java.util.List<Text> tooltip,
            net.minecraft.item.tooltip.TooltipType type
    ) {
        tooltip.add(
                Text.literal("Зажми на 2 секунды и отпусти для телепортации.")
                        .formatted(Formatting.DARK_AQUA)
        );
        super.appendTooltip(stack, context, tooltip, type);
    }
}
