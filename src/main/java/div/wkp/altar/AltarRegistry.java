package div.wkp.altar;

import div.wkp.altar.offerings.DonationOffering;
import div.wkp.altar.offerings.FloppyDiskOffering;
import div.wkp.altar.offerings.NetheriteScrapOffering;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AltarRegistry {
    private static final List<AltarOffering> OFFERINGS = new ArrayList<>();

    private AltarRegistry() {
    }

    public static void init() {
        register(new NetheriteScrapOffering());
        register(new FloppyDiskOffering());
        register(new DonationOffering());
    }

    public static void register(AltarOffering offering) {
        OFFERINGS.add(offering);
        OFFERINGS.sort(
                Comparator.comparingInt(AltarOffering::priority).reversed()
        );
    }

    public static AltarResult resolve(ItemStack input, int grace) {
        for (AltarOffering offering : OFFERINGS) {
            if (offering.matches(input, grace)) {
                return offering.process(input, grace);
            }
        }

        return AltarResult.empty(0);
    }
}