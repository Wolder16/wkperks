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

public class VelocityAugments extends AbstractPerk {
    private static final Identifier MODIFIER_ID = Identifier.of("wkperks", "perk_speed");
    private static final double SPEED_PER_LEVEL = 0.10;

    public VelocityAugments() {
        super(
                "speed",
                Text.literal("Velocity Augments").formatted(Formatting.GOLD),
                Text.literal("Увеличивает скорость на 10% за уровень").formatted(Formatting.GRAY),
                new ItemStack(Items.SUGAR),
                5
        );
    }

    @Override
    public void onLevelChanged(PlayerEntity player, int level) {
        EntityAttributeInstance attr =
                player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (attr == null) return;

        attr.removeModifier(MODIFIER_ID);

        // Применяем только если режим позволяет и уровень > 0
        if (level > 0 && PerkUtil.arePerksEnabled(player)) {
            double bonus = SPEED_PER_LEVEL * level;
            attr.addPersistentModifier(new EntityAttributeModifier(
                    MODIFIER_ID, bonus, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }
}