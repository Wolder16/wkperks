package div.wkp.altar.offerings;

import div.wkp.altar.AltarOffering;
import div.wkp.altar.AltarResult;
import div.wkp.item.ModItems;
import net.minecraft.item.ItemStack;

public class FloppyDiskOffering implements AltarOffering {
    @Override
    public int priority() {
        return 900;
    }

    @Override
    public boolean matches(ItemStack input, int currentGrace) {
        return input.isOf(ModItems.RHO_GIFT)
                && currentGrace >= 85;
    }

    @Override
    public AltarResult process(ItemStack input, int currentGrace) {
        return new AltarResult(
                new ItemStack(ModItems.FLOPPY_DISK),
                -20
        );
    }
}