package div.wkp.altar;

import net.minecraft.item.ItemStack;

public interface AltarOffering {
    /**
     * Чем выше число, тем раньше проверяется обмен.
     */
    int priority();

    boolean matches(ItemStack input, int currentGrace);

    AltarResult process(ItemStack input, int currentGrace);
}