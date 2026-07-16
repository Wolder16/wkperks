package div.wkp.perk.perks;

import div.wkp.PerkComponents;
import div.wkp.PerkUtil;
import div.wkp.component.PerkComponent;
import div.wkp.item.ModItems;
import div.wkp.perk.AbstractPerk;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProfitMotivePerk extends AbstractPerk {
    public static final String ID = "profit_motive";

    private static final double SPEED_BONUS_PER_ROACH = 0.01D;
    private static final double JUMP_BONUS_PER_ROACH = 0.01D;
    private static final double REGEN_BONUS_PER_ROACH = 0.0005D;
    private static final int UPDATE_INTERVAL_TICKS = 10;

    private static final Identifier SPEED_MODIFIER_ID =
            Identifier.of("wkperks", "profit_motive_speed");

    private static final Identifier JUMP_MODIFIER_ID =
            Identifier.of("wkperks", "profit_motive_jump");

    private static final Map<UUID, Integer> TRACKED_ROACH_COUNTS = new HashMap<>();

    public ProfitMotivePerk() {
        super(
                ID,
                Text.literal("Profit Motive")
                        .formatted(Formatting.GOLD),
                Text.literal(
                        "Каждый gold roach в инвентаре даёт +1% скорости и прыжка, а также небольшой бонус к регенерации."
                ).formatted(Formatting.GRAY),
                new ItemStack(ModItems.GOLD_ROACH)
        );
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void onLevelChanged(PlayerEntity player, int level) {
        if (level <= 0) {
            clearBonuses(player);
            return;
        }

        applyBonuses(player, countGoldRoaches(player));
    }

    public static void tick(ServerPlayerEntity player) {
        if (player.age % UPDATE_INTERVAL_TICKS != 0) {
            return;
        }

        if (!player.isAlive() || !PerkUtil.arePerksEnabled(player)) {
            clearBonuses(player);
            return;
        }

        PerkComponent component = PerkComponents.PERK_COMPONENT.get(player);

        if (!component.hasPerk(ID)) {
            clearBonuses(player);
            return;
        }

        int roachCount = countGoldRoaches(player);
        Integer previousCount = TRACKED_ROACH_COUNTS.get(player.getUuid());

        if (previousCount == null || previousCount != roachCount) {
            applyBonuses(player, roachCount);
            TRACKED_ROACH_COUNTS.put(player.getUuid(), roachCount);
        }

        if (roachCount <= 0 || player.getHealth() >= player.getMaxHealth()) {
            return;
        }

        float healAmount = (float) (player.getMaxHealth()
                * REGEN_BONUS_PER_ROACH
                * roachCount
                * (UPDATE_INTERVAL_TICKS / 20.0D));

        if (healAmount > 0.0F) {
            player.heal(healAmount);
        }
    }

    public static void clearTracking(UUID playerUuid) {
        TRACKED_ROACH_COUNTS.remove(playerUuid);
    }

    @Override
    public java.util.List<Text> getExtraTooltip(
            PlayerEntity player,
            PerkComponent component,
            int level
    ) {
        int roachCount = countGoldRoaches(player);
        int speedPercent = (int) Math.round(roachCount * SPEED_BONUS_PER_ROACH * 100.0D);
        int jumpPercent = (int) Math.round(roachCount * JUMP_BONUS_PER_ROACH * 100.0D);
        double regenPercent = roachCount * REGEN_BONUS_PER_ROACH * 100.0D;

        return java.util.List.of(
                Text.literal("Золотых тараканов: " + roachCount)
                        .formatted(roachCount > 0 ? Formatting.YELLOW : Formatting.GRAY),
                Text.literal(
                        "Скорость: +" + speedPercent + "% | Прыжок: +" + jumpPercent + "%"
                ).formatted((speedPercent > 0 || jumpPercent > 0) ? Formatting.GREEN : Formatting.GRAY),
                Text.literal(String.format(java.util.Locale.ROOT,
                                "Регенерация: +%.2f%% от макс. HP в секунду",
                                regenPercent))
                        .formatted(regenPercent > 0.0D ? Formatting.GREEN : Formatting.GRAY)
        );
    }

    private static void clearBonuses(PlayerEntity player) {
        TRACKED_ROACH_COUNTS.remove(player.getUuid());
        applyBonuses(player, 0);
    }

    private static void applyBonuses(PlayerEntity player, int roachCount) {
        EntityAttributeInstance speed =
                player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);

        EntityAttributeInstance jump =
                player.getAttributeInstance(EntityAttributes.GENERIC_JUMP_STRENGTH);

        if (speed != null) {
            speed.removeModifier(SPEED_MODIFIER_ID);
        }

        if (jump != null) {
            jump.removeModifier(JUMP_MODIFIER_ID);
        }

        if (roachCount <= 0 || !PerkUtil.arePerksEnabled(player)) {
            return;
        }

        double speedBonus = roachCount * SPEED_BONUS_PER_ROACH;
        double jumpBonus = roachCount * JUMP_BONUS_PER_ROACH;

        if (speed != null) {
            speed.addPersistentModifier(
                    new EntityAttributeModifier(
                            SPEED_MODIFIER_ID,
                            speedBonus,
                            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    )
            );
        }

        if (jump != null) {
            jump.addPersistentModifier(
                    new EntityAttributeModifier(
                            JUMP_MODIFIER_ID,
                            jumpBonus,
                            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    )
            );
        }
    }

    private static int countGoldRoaches(PlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        int roachCount = 0;

        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);

            if (stack.isOf(ModItems.GOLD_ROACH)) {
                roachCount += stack.getCount();
            }
        }

        return roachCount;
    }
}
