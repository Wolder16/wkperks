package div.wkp.artifact;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.UseAction;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public abstract class ArtifactItem extends Item {
    private final int maxEnergy;
    private final int rechargePerTick;
    private final int cooldownTicks;
    private final boolean soulbound;

    protected ArtifactItem(
            Settings settings,
            int maxEnergy,
            int rechargePerTick,
            int cooldownTicks,
            boolean soulbound
    ) {
        super(applyArtifactSettings(settings, maxEnergy));
        this.maxEnergy = maxEnergy;
        this.rechargePerTick = Math.max(0, rechargePerTick);
        this.cooldownTicks = Math.max(0, cooldownTicks);
        this.soulbound = soulbound;
    }

    private static Settings applyArtifactSettings(Settings settings, int maxEnergy) {
        settings.maxCount(1);

        if (maxEnergy > 0) {
            settings.maxDamage(maxEnergy);
        }

        return settings;
    }

    public int getMaxEnergy() {
        return maxEnergy;
    }

    public int getRechargePerTick() {
        return rechargePerTick;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public boolean isSoulbound(ItemStack stack) {
        return soulbound;
    }

    public int getEnergy(ItemStack stack) {
        if (maxEnergy <= 0) {
            return 0;
        }

        return Math.max(0, maxEnergy - stack.getDamage());
    }

    public void setEnergy(ItemStack stack, int energy) {
        if (maxEnergy <= 0) {
            return;
        }

        int clamped = Math.max(0, Math.min(maxEnergy, energy));
        stack.setDamage(maxEnergy - clamped);
    }

    public boolean consumeEnergy(ItemStack stack, int amount) {
        if (amount <= 0) {
            return true;
        }

        int current = getEnergy(stack);

        if (current < amount) {
            return false;
        }

        setEnergy(stack, current - amount);
        return true;
    }

    public void rechargeEnergy(ItemStack stack, int amount) {
        if (amount <= 0 || maxEnergy <= 0) {
            return;
        }

        setEnergy(stack, getEnergy(stack) + amount);
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            World world,
            Entity entity,
            int slot,
            boolean selected
    ) {
        boolean inUse = entity instanceof LivingEntity livingEntity
                && livingEntity.isUsingItem()
                && livingEntity.getActiveItem() == stack;

        if (!world.isClient
                && rechargePerTick > 0
                && getEnergy(stack) < maxEnergy
                && !inUse) {
            rechargeEnergy(stack, rechargePerTick);
        }

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    @Override
    public TypedActionResult<ItemStack> use(
            World world,
            PlayerEntity user,
            Hand hand
    ) {
        ItemStack stack = user.getStackInHand(hand);

        if (cooldownTicks > 0 && user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }

        if (isChargedUse(stack)) {
            user.setCurrentHand(hand);
            return TypedActionResult.consume(stack);
        }

        return onArtifactUse(world, user, hand, stack);
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        if (isChargedUse(stack)) {
            return getChargedUseMaxTime(stack, user);
        }

        return 0;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        if (isChargedUse(stack)) {
            return getChargedUseAction(stack);
        }

        return UseAction.NONE;
    }

    @Override
    public void usageTick(
            World world,
            LivingEntity user,
            ItemStack stack,
            int remainingUseTicks
    ) {
        if (isChargedUse(stack)) {
            onChargedUseTick(
                    world,
                    user,
                    stack,
                    getUsedTicks(stack, user, remainingUseTicks),
                    remainingUseTicks
            );
        }

        super.usageTick(world, user, stack, remainingUseTicks);
    }

    @Override
    public void onStoppedUsing(
            ItemStack stack,
            World world,
            LivingEntity user,
            int remainingUseTicks
    ) {
        if (isChargedUse(stack)) {
            onChargedUseReleased(
                    world,
                    user,
                    stack,
                    getUsedTicks(stack, user, remainingUseTicks),
                    remainingUseTicks
            );
        }

        super.onStoppedUsing(stack, world, user, remainingUseTicks);
    }

    protected TypedActionResult<ItemStack> finishActivation(
            World world,
            PlayerEntity user,
            ItemStack stack,
            int energyCost
    ) {
        if (!world.isClient) {
            if (!consumeEnergy(stack, energyCost)) {
                user.sendMessage(
                        Text.literal("Артефакт разряжен.")
                                .formatted(Formatting.RED),
                        true
                );
                return TypedActionResult.fail(stack);
            }

            if (cooldownTicks > 0) {
                user.getItemCooldownManager().set(this, cooldownTicks);
            }
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    protected boolean isChargedUse(ItemStack stack) {
        return false;
    }

    protected int getChargedUseMaxTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    protected UseAction getChargedUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    protected int getRequiredChargeTicks(ItemStack stack, LivingEntity user) {
        return 0;
    }

    protected int getUsedTicks(
            ItemStack stack,
            LivingEntity user,
            int remainingUseTicks
    ) {
        return getMaxUseTime(stack, user) - remainingUseTicks;
    }

    protected boolean hasReachedRequiredCharge(
            ItemStack stack,
            LivingEntity user,
            int usedTicks
    ) {
        return usedTicks >= getRequiredChargeTicks(stack, user);
    }

    protected void onChargedUseTick(
            World world,
            LivingEntity user,
            ItemStack stack,
            int usedTicks,
            int remainingUseTicks
    ) {
    }

    protected void onChargedUseReleased(
            World world,
            LivingEntity user,
            ItemStack stack,
            int usedTicks,
            int remainingUseTicks
    ) {
    }

    protected abstract TypedActionResult<ItemStack> onArtifactUse(
            World world,
            PlayerEntity user,
            Hand hand,
            ItemStack stack
    );

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        if (maxEnergy > 0) {
            tooltip.add(
                    Text.literal("Энергия: " + getEnergy(stack) + " / " + maxEnergy)
                            .formatted(Formatting.AQUA)
            );
        }

        if (rechargePerTick > 0) {
            tooltip.add(
                    Text.literal("Восстановление: " + rechargePerTick + " / тик")
                            .formatted(Formatting.GRAY)
            );
        }

        if (cooldownTicks > 0) {
            tooltip.add(
                    Text.literal("Кулдаун: " + cooldownTicks + " тиков")
                            .formatted(Formatting.DARK_GRAY)
            );
        }

        if (isChargedUse(stack)) {
            tooltip.add(
                    Text.literal(
                            "Минимальная зарядка: " + getRequiredChargeTicks(stack, null) + " тиков"
                    ).formatted(Formatting.DARK_AQUA)
            );
        }

        if (soulbound) {
            tooltip.add(
                    Text.literal("Soulbound")
                            .formatted(Formatting.GOLD)
            );
        }

        super.appendTooltip(stack, context, tooltip, type);
    }
}
