package div.wkp.altar;

import net.minecraft.item.ItemStack;

public record AltarResult(ItemStack output, int graceChange) {
    public AltarResult {
        output = output.copy();
    }

    public static AltarResult empty(int graceChange) {
        return new AltarResult(ItemStack.EMPTY, graceChange);
    }
}