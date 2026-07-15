package div.wkp.entity;

import div.wkp.ArtifactComponents;
import div.wkp.component.ArtifactStateComponent;
import div.wkp.item.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SpearProjectileEntity extends ThrownItemEntity {
    private static final float THROW_SPEED = 5.0F;
    private static final double THROW_RECOIL = 0.5D;
    private static final double RECALL_SPEED = 0.7D;
    private static final double RECALL_BOOST_HORIZONTAL = 1.5D;
    private static final double RECALL_BOOST_VERTICAL = 0.6D;
    private static final double RECALL_BOOST_MIN_UPWARD = 0.3D;
    private static final double CATCH_DISTANCE = 1.2D;
    private static final float BASE_DAMAGE = 6.0F;

    private boolean embedded = false;
    private boolean recalling = false;
    private Hand recallHand = Hand.MAIN_HAND;
    private int sourceSlot = -1;

    public SpearProjectileEntity(EntityType<? extends SpearProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    public SpearProjectileEntity(ServerPlayerEntity owner, Hand hand, int sourceSlot) {
        super(ModEntities.SPEAR_PROJECTILE, owner, owner.getWorld());
        this.setOwner(owner);
        this.setItem(new ItemStack(ModItems.ARTIFACT_SPEAR));
        this.recallHand = hand;
        this.sourceSlot = sourceSlot;
        this.setVelocity(owner, owner.getPitch(), owner.getYaw(), 0.0F, THROW_SPEED, 0.0F);
        this.updatePosition(owner.getX(), owner.getEyeY() - 0.1D, owner.getZ());
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.ARTIFACT_SPEAR;
    }

    @Override
    protected double getGravity() {
        return recalling || embedded ? 0.0D : 0.01D;
    }

    @Override
    public void tick() {
        if (recalling) {
            tickRecall();
            return;
        }

        if (embedded) {
            this.setVelocity(Vec3d.ZERO);
            return;
        }

        super.tick();
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        super.onBlockHit(blockHitResult);

        if (recalling) {
            return;
        }

        embedded = true;
        this.setVelocity(Vec3d.ZERO);
        this.setPosition(blockHitResult.getPos());
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);

        if (recalling) {
            return;
        }

        Entity entity = entityHitResult.getEntity();
        Entity owner = this.getOwner();

        if (owner instanceof LivingEntity livingOwner) {
            entity.damage(this.getDamageSources().thrown(this, livingOwner), BASE_DAMAGE);
        }
    }

    public void startRecall(Hand hand) {
        if (this.isRemoved()) {
            return;
        }

        this.recalling = true;
        this.embedded = false;
        this.recallHand = hand;
        this.noClip = true;
    }

    private void tickRecall() {
        if (!(this.getOwner() instanceof ServerPlayerEntity player)) {
            this.discard();
            return;
        }

        Vec3d target = player.getEyePos();
        Vec3d direction = target.subtract(this.getPos());
        double distance = direction.length();

        if (distance <= CATCH_DISTANCE) {
            catchBy(player);
            return;
        }

        Vec3d velocity = direction.normalize().multiply(RECALL_SPEED);
        this.setVelocity(velocity);
        this.velocityModified = true;
        this.setPosition(this.getPos().add(velocity));
    }

    private void catchBy(ServerPlayerEntity player) {
        ArtifactStateComponent component =
                ArtifactComponents.ARTIFACT_STATE_COMPONENT.get(player);

        ItemStack returnedSpear = new ItemStack(ModItems.ARTIFACT_SPEAR);

        if (!player.getAbilities().creativeMode) {
            if (player.getStackInHand(recallHand).isEmpty()) {
                player.setStackInHand(recallHand, returnedSpear);
            } else if (!player.getInventory().insertStack(returnedSpear)) {
                player.getInventory().offerOrDrop(returnedSpear);
            }
        }

        Vec3d velocity = this.getVelocity();
        if (velocity.lengthSquared() > 0.0001D) {
            Vec3d direction = velocity.normalize();
            double upwardBoost = Math.max(
                    RECALL_BOOST_MIN_UPWARD,
                    direction.y * RECALL_BOOST_VERTICAL + RECALL_BOOST_MIN_UPWARD
            );

            player.addVelocity(
                    direction.x * RECALL_BOOST_HORIZONTAL,
                    upwardBoost,
                    direction.z * RECALL_BOOST_HORIZONTAL
            );
            player.velocityModified = true;
        }

        player.currentScreenHandler.sendContentUpdates();
        component.clearActiveSpear();
        this.discard();
    }

    public int getSourceSlot() {
        return sourceSlot;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public boolean isRecalling() {
        return recalling;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Embedded", embedded);
        nbt.putBoolean("Recalling", recalling);
        nbt.putString("RecallHand", recallHand.name());
        nbt.putInt("SourceSlot", sourceSlot);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        embedded = nbt.getBoolean("Embedded");
        recalling = nbt.getBoolean("Recalling");
        recallHand = Hand.valueOf(nbt.getString("RecallHand"));
        sourceSlot = nbt.getInt("SourceSlot");
        noClip = recalling;
    }

    public static void applyThrowRecoil(PlayerEntity player) {
        Vec3d look = player.getRotationVector();
        player.addVelocity(-look.x * THROW_RECOIL, 0.4D, -look.z * THROW_RECOIL);
        player.velocityModified = true;
    }
}
