package div.wkp.perk.perks;

import div.wkp.perk.AbstractPerk;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class PulseOrgan extends AbstractPerk {
    public PulseOrgan() {
        super(
                "double_jump",
                Text.literal("Pulse Organ").formatted(Formatting.GREEN),
                Text.literal("Позволяет прыгать дополнительно в воздухе").formatted(Formatting.GRAY),
                new ItemStack(Items.FEATHER),
                5 // максимум 5 уровней = 5 доп. прыжков
        );
    }

    @Override
    public boolean isActive() {
        return true;
    }

}