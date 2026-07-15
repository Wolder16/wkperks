package div.wkp.perk.perks;

import div.wkp.PerkComponents;
import div.wkp.PerkUtil;
import div.wkp.component.PerkComponent;
import div.wkp.perk.AbstractPerk;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class TissueRestorativesPerk extends AbstractPerk {
    public static final String ID = "tissue_restoratives";
    private static final double BONUS_REGEN_SPEED_PER_LEVEL = 0.10D;

    public TissueRestorativesPerk() {
        super(
                ID,
                Text.literal("Tissue Restoratives")
                        .formatted(Formatting.GOLD),
                Text.literal(
                        "+10% к скорости регенерации здоровья за уровень."
                ).formatted(Formatting.GRAY),
                new ItemStack(Items.POTION),
                5
        );
    }

    @Override
    public boolean isActive() {
        return true;
    }

    public static void tick(ServerPlayerEntity player) {
        if (!player.isAlive() || !PerkUtil.arePerksEnabled(player)) {
            return;
        }

        PerkComponent component = PerkComponents.PERK_COMPONENT.get(player);
        int level = component.getPerkLevel(ID);

        if (level <= 0 || !player.canFoodHeal()) {
            return;
        }

        int baseInterval = player.getHungerManager().getSaturationLevel() > 0.0F ? 10 : 80;
        float bonusHealPerTick = (float) ((BONUS_REGEN_SPEED_PER_LEVEL * level) / baseInterval);

        if (bonusHealPerTick > 0.0F) {
            player.heal(bonusHealPerTick);
        }
    }

    @Override
    public List<Text> getExtraTooltip(
            PlayerEntity player,
            PerkComponent component,
            int level
    ) {
        int totalPercent = (int) Math.round(level * BONUS_REGEN_SPEED_PER_LEVEL * 100.0D);
        return List.of(
                Text.literal("Итоговый бонус: +" + totalPercent + "% к скорости регенерации")
                        .formatted(totalPercent > 0 ? Formatting.GREEN : Formatting.GRAY)
        );
    }
}
