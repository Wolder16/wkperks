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

public class ArmoredPlatingPerk extends AbstractPerk {
    public static final String ID = "armored_plating";

    private static final Identifier ARMOR_MODIFIER_ID =
            Identifier.of("wkperks", "armored_plating_armor");
    private static final Identifier TOUGHNESS_MODIFIER_ID =
            Identifier.of("wkperks", "armored_plating_toughness");
    private static final Identifier FALL_DAMAGE_MODIFIER_ID =
            Identifier.of("wkperks", "armored_plating_fall_damage");

    public ArmoredPlatingPerk() {
        super(
                ID,
                Text.literal("Armored Plating")
                        .formatted(Formatting.GOLD),
                Text.literal(
                        "+5 брони, +5 прочности брони и -50% урона от падения."
                ).formatted(Formatting.GRAY),
                new ItemStack(Items.NETHERITE_CHESTPLATE)
        );
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void onLevelChanged(PlayerEntity player, int level) {
        EntityAttributeInstance armor =
                player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
        EntityAttributeInstance toughness =
                player.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
        EntityAttributeInstance fallDamageMultiplier =
                player.getAttributeInstance(EntityAttributes.GENERIC_FALL_DAMAGE_MULTIPLIER);

        if (armor != null) {
            armor.removeModifier(ARMOR_MODIFIER_ID);
        }

        if (toughness != null) {
            toughness.removeModifier(TOUGHNESS_MODIFIER_ID);
        }

        if (fallDamageMultiplier != null) {
            fallDamageMultiplier.removeModifier(FALL_DAMAGE_MODIFIER_ID);
        }

        if (level <= 0 || !PerkUtil.arePerksEnabled(player)) {
            return;
        }

        if (armor != null) {
            armor.addPersistentModifier(new EntityAttributeModifier(
                    ARMOR_MODIFIER_ID,
                    5.0D,
                    EntityAttributeModifier.Operation.ADD_VALUE
            ));
        }

        if (toughness != null) {
            toughness.addPersistentModifier(new EntityAttributeModifier(
                    TOUGHNESS_MODIFIER_ID,
                    5.0D,
                    EntityAttributeModifier.Operation.ADD_VALUE
            ));
        }

        if (fallDamageMultiplier != null) {
            fallDamageMultiplier.addPersistentModifier(new EntityAttributeModifier(
                    FALL_DAMAGE_MODIFIER_ID,
                    -0.25D,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }
}
