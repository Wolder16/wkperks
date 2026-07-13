package div.wkp.perk.perks;

import div.wkp.PerkComponents;
import div.wkp.component.PerkComponent;
import div.wkp.perk.AbstractPerk;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class UnstoppablePerk extends AbstractPerk {
    public UnstoppablePerk() {
        super(
                "unstoppable",
                Text.literal("Consumptive Reflex").formatted(Formatting.GOLD),
                Text.literal("Поедание еды даёт 1 временный прыжок").formatted(Formatting.GRAY),
                new ItemStack(Items.GOLDEN_APPLE)
        );
    }

    @Override
    public boolean isActive() { return true; }

    @Override
    public void onLevelChanged(PlayerEntity player, int level) {
        if (level <= 0) {
            PerkComponents.PERK_COMPONENT.get(player).clearTempJumps();
        }
    }

    @Override
    public List<Text> getExtraTooltip(PerkComponent component, int level) {
        List<Text> lines = new ArrayList<>();
        int stored = component.getTempJumps();
        lines.add(Text.literal("Запас прыжков: " + stored)
                .formatted(stored > 0 ? Formatting.GREEN : Formatting.RED));
        return lines;
    }
}