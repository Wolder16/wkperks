package div.wkp.artifact;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class GravityGloveItem extends ArtifactItem {
    private static final int ENERGY_COST = 1;
    private static final int REQUIRED_CHARGE_TICKS = 40;

    public GravityGloveItem(Settings settings) {
        super(settings, 20, 1, 5, true);
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

        if (!hasReachedRequiredCharge(stack, user, usedTicks)) {
            return;
        }

        TypedActionResult<ItemStack> result =
                finishActivation(world, player, stack, ENERGY_COST);

        if (result.getResult().isAccepted() && !world.isClient) {
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOW_FALLING,
                    40,
                    0,
                    false,
                    true,
                    true
            ));

            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.REGENERATION,
                    40,
                    0,
                    false,
                    true,
                    true
            ));

            player.sendMessage(
                    Text.literal("Перчатка облегчает твоё тело.")
                            .formatted(Formatting.AQUA),
                    true
            );
        }

        user.stopUsingItem();
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
