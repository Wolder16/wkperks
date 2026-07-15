package div.wkp.artifact;

import div.wkp.component.ArtifactStateComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

public class GravityGloveItem extends ArtifactItem {
    private static final int CHARGE_TICKS = 40;
    private static final int STARTUP_ENERGY_COST = 1;
    private static final int ACTIVE_DRAIN_INTERVAL = 5;
    private static final int ACTIVE_DRAIN_COST = 1;

    public GravityGloveItem(Settings settings) {
        super(settings, 20, 1, 5, true);
    }

    @Override
    public void onUseTick(
            ServerPlayerEntity player,
            Hand hand,
            ItemStack stack,
            ArtifactStateComponent component
    ) {
        int useTicks = component.getUseTicks();

        if (!component.isCharged()) {
            if (useTicks < CHARGE_TICKS) {
                return;
            }

            if (!consumeEnergy(stack, STARTUP_ENERGY_COST)) {
                player.sendMessage(
                        Text.literal("Перчатке не хватает энергии для активации.")
                                .formatted(Formatting.RED),
                        true
                );
                component.stopUsingArtifact();
                return;
            }

            component.setCharged(true);
        }

        if (useTicks > CHARGE_TICKS && useTicks % ACTIVE_DRAIN_INTERVAL == 0) {
            if (!consumeEnergy(stack, ACTIVE_DRAIN_COST)) {
                player.sendMessage(
                        Text.literal("Энергия перчатки иссякла.")
                                .formatted(Formatting.RED),
                        true
                );
                component.stopUsingArtifact();
                return;
            }
        }

        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SLOW_FALLING,
                6,
                0,
                false,
                false,
                true
        ));

        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.REGENERATION,
                6,
                0,
                false,
                false,
                true
        ));
    }

    @Override
    public void onUseReleased(
            ServerPlayerEntity player,
            Hand hand,
            ItemStack stack,
            ArtifactStateComponent component
    ) {
        component.setCharged(false);
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            java.util.List<Text> tooltip,
            net.minecraft.item.tooltip.TooltipType type
    ) {
        tooltip.add(
                Text.literal("Зажми на 2 секунды, затем удерживай для эффекта.")
                        .formatted(Formatting.DARK_AQUA)
        );
        super.appendTooltip(stack, context, tooltip, type);
    }
}
