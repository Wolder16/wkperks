package div.wkp.perk.perks;

import div.wkp.component.PerkComponent;
import div.wkp.perk.AbstractPerk;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class RhoGracePerk extends AbstractPerk {
    public static final String ID = "rho_grace";
    public static final int MAX_LEVEL = 100;

    public RhoGracePerk() {
        super(
                ID,
                Text.literal("Благосклонность Ро").formatted(Formatting.GOLD),
                Text.literal("Репутация игрока у божества Ро.")
                        .formatted(Formatting.GRAY),
                new ItemStack(Items.NETHER_STAR),
                MAX_LEVEL
        );
    }

    @Override
    public List<Text> getExtraTooltip(PerkComponent component, int level) {
        List<Text> tooltip = new ArrayList<>();

        tooltip.add(
                Text.literal("Уровень благосклонности: " + level + " / " + MAX_LEVEL)
                        .formatted(Formatting.YELLOW)
        );

        if (level >= 100) {
            tooltip.add(
                    Text.literal("Ро готов принять золотой блок.")
                            .formatted(Formatting.GOLD)
            );
        } else if (level >= 85) {
            tooltip.add(
                    Text.literal("Доступен обмен Подарка для Ро.")
                            .formatted(Formatting.GREEN)
            );
        }

        return tooltip;
    }
}