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

public class SteadiedStancePerk extends AbstractPerk {
    public static final String ID = "steadied_stance";

    private static final Identifier ARMOR_MODIFIER_ID =
            Identifier.of("wkperks", "steadied_stance_armor");
    private static final Identifier KNOCKBACK_MODIFIER_ID =
            Identifier.of("wkperks", "steadied_stance_knockback");
    private static final Identifier EXPLOSION_KNOCKBACK_MODIFIER_ID =
            Identifier.of("wkperks", "steadied_stance_explosion_knockback");
    private static final Identifier FALL_DAMAGE_MODIFIER_ID =
            Identifier.of("wkperks", "steadied_stance_fall_damage");

    public SteadiedStancePerk() {
        super(
                ID,
                Text.literal("Steadied Stance")
                        .formatted(Formatting.GOLD),
                Text.literal(
                        "+2.5 брони, +0.3 сопротивления отбрасыванию и взрывам, -50% урона от падения."
                ).formatted(Formatting.GRAY),
                new ItemStack(Items.IRON_BOOTS)
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
        EntityAttributeInstance knockbackResistance =
                player.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        EntityAttributeInstance explosionKnockbackResistance =
                player.getAttributeInstance(EntityAttributes.GENERIC_EXPLOSION_KNOCKBACK_RESISTANCE);
        EntityAttributeInstance fallDamageMultiplier =
                player.getAttributeInstance(EntityAttributes.GENERIC_FALL_DAMAGE_MULTIPLIER);

        if (armor != null) {
            armor.removeModifier(ARMOR_MODIFIER_ID);
        }

        if (knockbackResistance != null) {
            knockbackResistance.removeModifier(KNOCKBACK_MODIFIER_ID);
        }

        if (explosionKnockbackResistance != null) {
            explosionKnockbackResistance.removeModifier(EXPLOSION_KNOCKBACK_MODIFIER_ID);
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
                    2.5D,
                    EntityAttributeModifier.Operation.ADD_VALUE
            ));
        }

        if (knockbackResistance != null) {
            knockbackResistance.addPersistentModifier(new EntityAttributeModifier(
                    KNOCKBACK_MODIFIER_ID,
                    0.3D,
                    EntityAttributeModifier.Operation.ADD_VALUE
            ));
        }

        if (explosionKnockbackResistance != null) {
            explosionKnockbackResistance.addPersistentModifier(new EntityAttributeModifier(
                    EXPLOSION_KNOCKBACK_MODIFIER_ID,
                    0.3D,
                    EntityAttributeModifier.Operation.ADD_VALUE
            ));
        }

        if (fallDamageMultiplier != null) {
            fallDamageMultiplier.addPersistentModifier(new EntityAttributeModifier(
                    FALL_DAMAGE_MODIFIER_ID,
                    -0.125D,
                    EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }
}
