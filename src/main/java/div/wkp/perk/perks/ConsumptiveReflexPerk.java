
package div.wkp.perk.perks;

import div.wkp.PerkComponents;
import div.wkp.PerkUtil;
import div.wkp.component.PerkComponent;
import div.wkp.perk.AbstractPerk;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import div.wkp.config.WKPerksConfig;

import java.util.Set;

public class ConsumptiveReflexPerk extends AbstractPerk {
    public static final String ID = "consumptive_reflex";

    private static final java.util.Set<java.util.UUID> overeatingPlayers =
            new java.util.HashSet<>();
    private static final double MAX_HEALTH_BONUS = 0.30D;

    private static final Identifier MAX_HEALTH_MODIFIER_ID =
            Identifier.of("wkperks", "consumptive_reflex_max_health");

    /**
     * Еда, которую перк никогда не будет есть автоматически.
     */
    private static final Set<Item> FOOD_BLACKLIST = Set.of(
            // Сырое мясо
            Items.BEEF,
            Items.CHICKEN,
            Items.PORKCHOP,
            Items.MUTTON,
            Items.RABBIT,
            Items.COD,
            Items.SALMON,

            // Гнилая / ядовитая еда
            Items.ROTTEN_FLESH,
            Items.SPIDER_EYE,
            Items.POISONOUS_POTATO,
            Items.PUFFERFISH,

            // Непредсказуемые эффекты
            Items.SUSPICIOUS_STEW,
            Items.CHORUS_FRUIT
    );

    public ConsumptiveReflexPerk() {
        super(
                ID,
                Text.literal("Consumptive Reflex")
                        .formatted(Formatting.GOLD),
                Text.literal(
                        "Автоматически употребляет лучшую еду при низком голоде и увеличивает здоровье на 30%."
                ).formatted(Formatting.GRAY),
                new ItemStack(Items.GOLDEN_CARROT)
        );
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void onLevelChanged(PlayerEntity player, int level) {
        if (level <= 0) {
            overeatingPlayers.remove(player.getUuid());
        }

        EntityAttributeInstance maxHealth =
                player.getAttributeInstance(
                        EntityAttributes.GENERIC_MAX_HEALTH
                );

        if (maxHealth == null) {
            return;
        }

        maxHealth.removeModifier(MAX_HEALTH_MODIFIER_ID);

        if (level > 0 && PerkUtil.arePerksEnabled(player)) {
            maxHealth.addPersistentModifier(
                    new EntityAttributeModifier(
                            MAX_HEALTH_MODIFIER_ID,
                            MAX_HEALTH_BONUS,
                            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    )
            );
        }

        if (!player.getWorld().isClient
                && player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /**
     * Вызывается один раз за серверный тик для каждого игрока.
     */
    public static void tick(ServerPlayerEntity player) {
        if (!player.isAlive()) {
            overeatingPlayers.remove(player.getUuid());
            return;
        }

        if (!PerkUtil.arePerksEnabled(player)) {
            overeatingPlayers.remove(player.getUuid());
            return;
        }

        PerkComponent component =
                PerkComponents.PERK_COMPONENT.get(player);

        if (!component.hasPerk(ID)) {
            overeatingPlayers.remove(player.getUuid());
            return;
        }

        WKPerksConfig config = WKPerksConfig.get();

        if (!config.consumptiveReflexOvereat) {
            overeatingPlayers.remove(player.getUuid());
        }

        int foodLevel = player.getHungerManager().getFoodLevel();
        int threshold = config.consumptiveReflexEatBelowFoodLevel;

        boolean shouldEat;

        if (config.consumptiveReflexOvereat) {
            // Начали доедать, когда упали ниже порога.
            if (foodLevel < threshold) {
                overeatingPlayers.add(player.getUuid());
            }

            // Пока сыты не полностью и режим доедания активен — едим.
            if (overeatingPlayers.contains(player.getUuid())) {
                if (foodLevel >= 20) {
                    overeatingPlayers.remove(player.getUuid());
                    shouldEat = false;
                } else {
                    shouldEat = true;
                }
            } else {
                shouldEat = false;
            }
        } else {
            shouldEat = foodLevel < threshold;
        }

        if (!shouldEat) {
            return;
        }

        consumeBestFood(player);
    }

    public static void clearTracking(java.util.UUID playerUuid) {
        overeatingPlayers.remove(playerUuid);
    }

    /**
     * Находит во всём инвентаре еду с наибольшим фактическим
     * насыщением (пропуская чёрный список) и мгновенно
     * завершает её употребление.
     */
    private static void consumeBestFood(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();

        int bestSlot = -1;
        float bestSaturation = -1.0F;
        int bestNutrition = -1;

        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);

            if (stack.isEmpty()) {
                continue;
            }

            // Чёрный список
            if (FOOD_BLACKLIST.contains(stack.getItem())) {
                continue;
            }

            FoodComponent food =
                    stack.get(DataComponentTypes.FOOD);

            if (food == null) {
                continue;
            }

            float saturation =
                    food.nutrition()
                            * food.saturation();

            boolean betterSaturation =
                    saturation > bestSaturation;

            boolean equalSaturationButMoreFood =
                    Float.compare(saturation, bestSaturation) == 0
                            && food.nutrition() > bestNutrition;

            if (betterSaturation || equalSaturationButMoreFood) {
                bestSlot = slot;
                bestSaturation = saturation;
                bestNutrition = food.nutrition();
            }
        }

        if (bestSlot < 0) {
            return;
        }

        ItemStack foodStack = inventory.getStack(bestSlot);

        ItemStack result =
                foodStack.finishUsing(
                        player.getWorld(),
                        player
                );

        inventory.setStack(bestSlot, result);
        inventory.markDirty();

        player.currentScreenHandler.sendContentUpdates();
    }
}

