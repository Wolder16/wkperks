package div.wkp.perk.perks;

import div.wkp.perk.AbstractPerk;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class HeavyStrikePerk extends AbstractPerk {
    public static final String ID = "heavy_strike";

    public HeavyStrikePerk() {
        super(
                ID,
                Text.literal("Heavy Strike")
                        .formatted(Formatting.GOLD),
                Text.literal(
                        "Удваивает наносимый игроком урон."
                ).formatted(Formatting.GRAY),
                new ItemStack(Items.NETHERITE_AXE)
        );
    }

    @Override
    public boolean isActive() {
        return true;
    }
}