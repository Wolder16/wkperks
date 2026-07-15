package div.wkp.perk.perks;

import div.wkp.PerkUtil;
import div.wkp.perk.AbstractPerk;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class SomaticPainkillersPerk extends AbstractPerk {
    public static final String ID = "somatic_painkillers";

    private static final double MAX_HEALTH_PER_LEVEL = 0.20D;
    private static final Identifier MAX_HEALTH_MODIFIER_ID =
            Identifier.of("wkperks", "somatic_painkillers_max_health");

    public SomaticPainkillersPerk() {
        super(
                ID,
                Text.literal("Somatic Painkillers")
                        .formatted(Formatting.GOLD),
                Text.literal(
                        "+20% к максимальному здоровью за уровень."
                ).formatted(Formatting.GRAY),
                new ItemStack(Items.GHAST_TEAR),
                5
        );
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void onLevelChanged(PlayerEntity player, int level) {
        EntityAttributeInstance maxHealth =
                player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);

        if (maxHealth == null) {
            return;
        }

        maxHealth.removeModifier(MAX_HEALTH_MODIFIER_ID);

        if (level > 0 && PerkUtil.arePerksEnabled(player)) {
            maxHealth.addPersistentModifier(
                    new EntityAttributeModifier(
                            MAX_HEALTH_MODIFIER_ID,
                            MAX_HEALTH_PER_LEVEL * level,
                            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    )
            );
        }

        if (!player.getWorld().isClient && player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }
}
