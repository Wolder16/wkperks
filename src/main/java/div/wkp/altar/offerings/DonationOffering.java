package div.wkp.altar.offerings;

import div.wkp.altar.AltarOffering;
import div.wkp.altar.AltarResult;
import div.wkp.altar.DonationValues;
import net.minecraft.item.ItemStack;

public class DonationOffering implements AltarOffering {
    @Override
    public int priority() {
        return 0;
    }

    @Override
    public boolean matches(ItemStack input, int currentGrace) {
        return !input.isEmpty();
    }

    @Override
    public AltarResult process(ItemStack input, int currentGrace) {
        int value = DonationValues.getValue(input);

        return AltarResult.empty(value);
    }
}