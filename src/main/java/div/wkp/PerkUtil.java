package div.wkp;

import div.wkp.component.PerkComponent;
import div.wkp.perk.Perk;
import div.wkp.perk.PerkRegistry;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Map;

public final class PerkUtil {
    private PerkUtil() {
    }

    /**
     * Перки работают только в выживании и приключении.
     */
    public static boolean arePerksEnabled(PlayerEntity player) {
        return !player.isCreative() && !player.isSpectator();
    }

    /**
     * Повторно применяет или снимает эффекты всех перков.
     */
    public static void refreshPlayer(PlayerEntity player) {
        PerkComponent component =
                PerkComponents.PERK_COMPONENT.get(player);

        boolean enabled = arePerksEnabled(player);

        for (Map.Entry<String, Integer> entry : component.getPerks().entrySet()) {
            Perk perk = PerkRegistry.get(entry.getKey());

            if (perk == null) {
                continue;
            }

            perk.onLevelChanged(
                    player,
                    enabled ? entry.getValue() : 0
            );
        }
    }
}