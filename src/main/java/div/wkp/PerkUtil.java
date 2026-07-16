package div.wkp;

import div.wkp.component.PerkComponent;
import div.wkp.perk.Perk;
import div.wkp.perk.PerkRegistry;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Map;

public final class PerkUtil {
    private static final double BASE_PLAYER_JUMP_STRENGTH = 0.42D;
    private static final double BASE_DOUBLE_JUMP_VERTICAL_VELOCITY = 0.5D;

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

    public static double getScaledDoubleJumpVerticalVelocity(PlayerEntity player) {
        EntityAttributeInstance jumpStrength =
                player.getAttributeInstance(EntityAttributes.GENERIC_JUMP_STRENGTH);

        if (jumpStrength == null) {
            return BASE_DOUBLE_JUMP_VERTICAL_VELOCITY;
        }

        double currentJumpStrength = jumpStrength.getValue();
        if (currentJumpStrength <= 0.0D) {
            return BASE_DOUBLE_JUMP_VERTICAL_VELOCITY;
        }

        return BASE_DOUBLE_JUMP_VERTICAL_VELOCITY
                * (currentJumpStrength / BASE_PLAYER_JUMP_STRENGTH);
    }
}