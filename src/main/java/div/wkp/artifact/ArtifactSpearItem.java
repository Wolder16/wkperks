package div.wkp.artifact;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ArtifactSpearItem extends ArtifactItem {
    public ArtifactSpearItem(Settings settings) {
        super(settings, 20, 1, 8, true);
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            java.util.List<Text> tooltip,
            net.minecraft.item.tooltip.TooltipType type
    ) {
        super.appendTooltip(stack, context, tooltip, type);
    }
}
