package div.wkp.entity;

import div.wkp.ArtifactComponents;
import div.wkp.artifact.ArtifactSpearItem;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SpearProjectileEntity extends ThrownItemEntity implements GeoEntity {
    private static final float THROW_SPEED = 5.0F;
    private static final double THROW_RECOIL = 0.5D;
    private static final double THROW_VERTICAL_RECOIL = 1.5D;
    private static final double RECALL_SPEED = 1.0D;
    private static final double RECALL_BOOST_HORIZONTAL = 1.5D;
    private static final double RECALL_BOOST_VERTICAL = 0.65D;
    private static final double RECALL_BOOST_MIN_UPWARD = 0.4D;
    private static final double CATCH_DISTANCE = 1.2D;
    private static final double MIN_RECALL_DISTANCE = 2.0D;
    private static final double AUTO_RECALL_DISTANCE = 50.0D;
    private static final double EMBED_DEPTH = -1.5D;
    private static final float BASE_DAMAGE = 6.0F;
    private static final int OVERHEAT_BURN_SECONDS = 3;
    private static final float OVERHEAT_DIRECT_DAMAGE = 2.0F;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private boolean embedded = false;
    private boolean recalling = false;
    private boolean launchedWhileOverheated = false;
    private Hand recallHand = Hand.MAIN_HAND;
    private int sourceSlot = -1;
    private float embeddedYaw = 0.0F;
    private float embeddedPitch = 0.0F;

    public SpearProjectileEntity(EntityType<? extends SpearProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    public SpearProjectileEntity(ServerPlayerEntity owner, Hand hand, int sourceSlot, ItemStack stack) {
        super(ModEntities.SPEAR_PROJECTILE, owner, owner.getWorld());
        this.setOwner(owner);
        this.setItem(stack.copy());
        this.launchedWhileOverheated = ArtifactSpearItem.isOverheated(stack);
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
        if (!recalling && this.getOwner() instanceof ServerPlayerEntity player) {
            if (this.squaredDistanceTo(player) >= AUTO_RECALL_DISTANCE * AUTO_RECALL_DISTANCE) {
                startRecall(Hand.OFF_HAND);
            }
        }

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

        Vec3d impactVelocity = this.getVelocity();
        Vec3d embedDirection = impactVelocity.lengthSquared() > 1.0E-6D
                ? impactVelocity.normalize()
                : Vec3d.fromPolar(this.getPitch(), this.getYaw());

        embedded = true;
        this.embeddedYaw = this.getYaw();
        this.embeddedPitch = this.getPitch();
        this.setVelocity(Vec3d.ZERO);
        this.setPosition(blockHitResult.getPos().add(embedDirection.multiply(EMBED_DEPTH)));
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

        if (launchedWhileOverheated) {
            applyOverheatPenalty(entity);
        }
    }

    public boolean startRecall(Hand hand) {
        if (this.isRemoved() || this.getOwner() == null) {
            return false;
        }

        if (this.squaredDistanceTo(this.getOwner()) < MIN_RECALL_DISTANCE * MIN_RECALL_DISTANCE) {
            return false;
        }

        this.recalling = true;
        this.embedded = false;
        this.recallHand = hand;
        this.noClip = true;
        return true;
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
        this.prevYaw = this.getYaw();
        this.prevPitch = this.getPitch();
        this.setVelocity(velocity);
        this.updateRotation();
        this.velocityModified = true;
        this.setPosition(this.getPos().add(velocity));
    }

    private void catchBy(ServerPlayerEntity player) {
        ArtifactStateComponent component =
                ArtifactComponents.ARTIFACT_STATE_COMPONENT.get(player);

        ItemStack returnedSpear = this.getStack().copy();
        ArtifactSpearItem.addHeatOnCatch(returnedSpear);

        if (!player.getAbilities().creativeMode) {
            if (player.getStackInHand(recallHand).isEmpty()) {
                player.setStackInHand(recallHand, returnedSpear);
            } else if (!player.getInventory().insertStack(returnedSpear)) {
                player.getInventory().offerOrDrop(returnedSpear);
            }
        }

        if (launchedWhileOverheated) {
            applyOverheatPenalty(player);
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

    private void applyOverheatPenalty(Entity entity) {
        entity.setOnFireFor(OVERHEAT_BURN_SECONDS);
        entity.damage(entity.getDamageSources().generic(), OVERHEAT_DIRECT_DAMAGE);
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

    public float getEmbeddedYaw() {
        return embeddedYaw;
    }

    public float getEmbeddedPitch() {
        return embeddedPitch;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Embedded", embedded);
        nbt.putBoolean("Recalling", recalling);
        nbt.putBoolean("LaunchedWhileOverheated", launchedWhileOverheated);
        nbt.putString("RecallHand", recallHand.name());
        nbt.putInt("SourceSlot", sourceSlot);
        nbt.putFloat("EmbeddedYaw", embeddedYaw);
        nbt.putFloat("EmbeddedPitch", embeddedPitch);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        embedded = nbt.getBoolean("Embedded");
        recalling = nbt.getBoolean("Recalling");
        launchedWhileOverheated = nbt.getBoolean("LaunchedWhileOverheated");
        recallHand = Hand.valueOf(nbt.getString("RecallHand"));
        sourceSlot = nbt.getInt("SourceSlot");
        embeddedYaw = nbt.getFloat("EmbeddedYaw");
        embeddedPitch = nbt.getFloat("EmbeddedPitch");
        noClip = recalling;
    }

    public static void applyThrowRecoil(PlayerEntity player) {
        Vec3d look = player.getRotationVector();
        player.addVelocity(-look.x * THROW_RECOIL, -look.y * THROW_VERTICAL_RECOIL, -look.z * THROW_RECOIL);
        player.velocityModified = true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
