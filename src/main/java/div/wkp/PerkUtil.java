package div.wkp;

import div.wkp.component.PerkComponent;
import div.wkp.perk.Perk;
import div.wkp.perk.PerkRegistry;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Map;

public final class PerkUtil {
    private PerkUtil() {}

    /** Перки работают только в выживании и приключении */
    public static boolean arePerksEnabled(PlayerEntity player) {
        return !player.isCreative() && !player.isSpectator();
    }

    /**
     * Заново применяет/снимает эффекты всех перков в зависимости от режима.
     * Вызывается при смене режима игры.
     */
    public static void refreshPlayer(PlayerEntity player) {
        PerkComponent comp = PerkComponents.PERK_COMPONENT.get(player);
        boolean enabled = arePerksEnabled(player);

        for (Map.Entry<String, Integer> entry : comp.getPerks().entrySet()) {
            Perk perk = PerkRegistry.get(entry.getKey());
            if (perk == null) continue;
            // Если перки выключены — передаём уровень 0 (снятие),
            // иначе реальный уровень (применение)
            perk.onLevelChanged(player, enabled ? entry.getValue() : 0);
        }
    }
}