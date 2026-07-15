package div.wkp.perk.perks;

import div.wkp.PerkComponents;
import div.wkp.component.PerkComponent;
import div.wkp.perk.AbstractPerk;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class AnomalousBondsPerk extends AbstractPerk {
    public static final String ID = "anomalous_bonds";
    public static final int MAX_USES = 2;

    public AnomalousBondsPerk() {
        super(
                ID,
                Text.literal("Anomalous Bonds").formatted(Formatting.GOLD),
                Text.literal("Сохраняет весь инвентарь при смерти. Имеет ограниченное число использований.")
                        .formatted(Formatting.GRAY),
                new ItemStack(Items.TOTEM_OF_UNDYING)
        );
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void onLevelChanged(PlayerEntity player, int level) {
        PerkComponent component = PerkComponents.PERK_COMPONENT.get(player);

        if (level <= 0) {
            component.setAnomalousBondsCharges(0);
            return;
        }

        if (component.getAnomalousBondsCharges() <= 0) {
            component.setAnomalousBondsCharges(MAX_USES);
        }
    }

    @Override
    public List<Text> getExtraTooltip(
            PlayerEntity player,
            PerkComponent component,
            int level
    ) {
        int charges = component.getAnomalousBondsCharges();
        return List.of(
                Text.literal("Осталось сохранений: " + charges + " / " + MAX_USES)
                        .formatted(charges > 0 ? Formatting.GREEN : Formatting.RED)
        );
    }
}
