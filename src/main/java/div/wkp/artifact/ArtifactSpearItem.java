package div.wkp.artifact;

import div.wkp.ArtifactComponents;
import div.wkp.component.ArtifactStateComponent;
import div.wkp.entity.SpearProjectileEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

public class ArtifactSpearItem extends ArtifactItem {
    public ArtifactSpearItem(Settings settings) {
        super(settings, 20, 1, 8, true);
    }

    @Override
    public boolean canStartUsing(
            ServerPlayerEntity player,
            Hand hand,
            ItemStack stack,
            ArtifactStateComponent component
    ) {
        return super.canStartUsing(player, hand, stack, component)
                && !ArtifactComponents.ARTIFACT_STATE_COMPONENT.get(player).hasActiveSpear();
    }

    @Override
    public void onUseStarted(
            ServerPlayerEntity player,
            Hand hand,
            ItemStack stack,
            ArtifactStateComponent component
    ) {
        int sourceSlot = hand == Hand.MAIN_HAND
                ? player.getInventory().selectedSlot
                : PlayerInventory.OFF_HAND_SLOT;

        SpearProjectileEntity spear = new SpearProjectileEntity(player, hand, sourceSlot);
        player.getWorld().spawnEntity(spear);
        component.setActiveSpear(spear.getUuidAsString(), sourceSlot);
        SpearProjectileEntity.applyThrowRecoil(player);

        if (!player.getAbilities().creativeMode) {
            player.setStackInHand(hand, ItemStack.EMPTY);
        }

        component.stopUsingArtifact();
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            java.util.List<Text> tooltip,
            net.minecraft.item.tooltip.TooltipType type
    ) {
        tooltip.add(
                Text.literal("ПКМ: бросок. Повторный ПКМ после броска: отзыв.")
                        .formatted(Formatting.DARK_AQUA)
        );

        super.appendTooltip(stack, context, tooltip, type);
    }
}
