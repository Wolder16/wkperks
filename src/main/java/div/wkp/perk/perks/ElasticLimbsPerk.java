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

public class ElasticLimbsPerk extends AbstractPerk {
    public static final String ID = "elastic_limbs";

    private static final int MAX_LEVEL = 5;
    private static final double RANGE_PER_LEVEL = 0.25D;

    private static final Identifier BLOCK_RANGE_MODIFIER_ID =
            Identifier.of("wkperks", "elastic_limbs_block_range");

    private static final Identifier ENTITY_RANGE_MODIFIER_ID =
            Identifier.of("wkperks", "elastic_limbs_entity_range");

    public ElasticLimbsPerk() {
        super(
                ID,
                Text.literal("Elastic Limbs")
                        .formatted(Formatting.GOLD),
                Text.literal(
                        "Увеличивает дальность взаимодействия на 25% за уровень."
                ).formatted(Formatting.GRAY),
                new ItemStack(Items.SLIME_BALL),
                MAX_LEVEL
        );
    }

    @Override
    public void onLevelChanged(PlayerEntity player, int level) {
        EntityAttributeInstance blockRange =
                player.getAttributeInstance(
                        EntityAttributes.PLAYER_BLOCK_INTERACTION_RANGE
                );

        EntityAttributeInstance entityRange =
                player.getAttributeInstance(
                        EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE
                );

        if (blockRange != null) {
            blockRange.removeModifier(BLOCK_RANGE_MODIFIER_ID);

            if (level > 0 && PerkUtil.arePerksEnabled(player)) {
                blockRange.addPersistentModifier(
                        new EntityAttributeModifier(
                                BLOCK_RANGE_MODIFIER_ID,
                                RANGE_PER_LEVEL * level,
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                        )
                );
            }
        }

        if (entityRange != null) {
            entityRange.removeModifier(ENTITY_RANGE_MODIFIER_ID);

            if (level > 0 && PerkUtil.arePerksEnabled(player)) {
                entityRange.addPersistentModifier(
                        new EntityAttributeModifier(
                                ENTITY_RANGE_MODIFIER_ID,
                                RANGE_PER_LEVEL * level,
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                        )
                );
            }
        }
    }
}