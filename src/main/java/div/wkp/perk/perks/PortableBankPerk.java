package div.wkp.perk.perks;

import div.wkp.perk.AbstractPerk;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PortableBankPerk extends AbstractPerk {
    public static final String ID = "portable_bank";

    public PortableBankPerk() {
        super(
                ID,
                Text.literal("Portable Bank")
                        .formatted(Formatting.GOLD),
                Text.literal(
                        "Открывает эндер-сундук прямо из инвентаря."
                ).formatted(Formatting.GRAY),
                new ItemStack(Items.ENDER_CHEST)
        );
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
