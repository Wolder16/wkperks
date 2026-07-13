package div.wkp.altar.offerings;

import div.wkp.altar.AltarOffering;
import div.wkp.altar.AltarResult;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class NetheriteScrapOffering implements AltarOffering {
    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public boolean matches(ItemStack input, int currentGrace) {
        return input.isOf(Items.GOLD_BLOCK)
                && currentGrace >= 100;
    }

    @Override
    public AltarResult process(ItemStack input, int currentGrace) {
        return new AltarResult(
                new ItemStack(Items.NETHERITE_SCRAP),
                -50
        );
    }
}