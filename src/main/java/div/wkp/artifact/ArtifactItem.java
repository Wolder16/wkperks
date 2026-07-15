package div.wkp.artifact;

import div.wkp.ArtifactComponents;
import div.wkp.component.ArtifactStateComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
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
        if (amount <= 0 || maxEnergy <= 0) {
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
        if (!world.isClient
                && rechargePerTick > 0
                && getEnergy(stack) < maxEnergy
                && shouldRecharge(stack, entity)) {
            rechargeEnergy(stack, rechargePerTick);
        }

        super.inventoryTick(stack, world, entity, slot, selected);
    }

    protected boolean shouldRecharge(ItemStack stack, Entity entity) {
        if (!(entity instanceof PlayerEntity player)) {
            return true;
        }

        ArtifactStateComponent component =
                ArtifactComponents.ARTIFACT_STATE_COMPONENT.get(player);

        if (!component.isUsingArtifact()) {
            return true;
        }

        return player.getStackInHand(component.getActiveHand()) != stack;
    }

    @Override
    public TypedActionResult<ItemStack> use(
            World world,
            PlayerEntity user,
            Hand hand
    ) {
        return TypedActionResult.pass(user.getStackInHand(hand));
    }

    public boolean canStartUsing(
            ServerPlayerEntity player,
            Hand hand,
            ItemStack stack,
            ArtifactStateComponent component
    ) {
        return cooldownTicks <= 0
                || !player.getItemCooldownManager().isCoolingDown(this);
    }

    public void onUseStarted(
            ServerPlayerEntity player,
            Hand hand,
            ItemStack stack,
            ArtifactStateComponent component
    ) {
    }

    public void onUseTick(
            ServerPlayerEntity player,
            Hand hand,
            ItemStack stack,
            ArtifactStateComponent component
    ) {
    }

    public void onUseReleased(
            ServerPlayerEntity player,
            Hand hand,
            ItemStack stack,
            ArtifactStateComponent component
    ) {
    }

    public void onUseCancelled(
            ServerPlayerEntity player,
            Hand hand,
            ItemStack stack,
            ArtifactStateComponent component
    ) {
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

        if (soulbound) {
            tooltip.add(
                    Text.literal("Soulbound")
                            .formatted(Formatting.GOLD)
            );
        }

        super.appendTooltip(stack, context, tooltip, type);
    }
}
