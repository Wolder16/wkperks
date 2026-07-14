package div.wkp.perk;

import div.wkp.component.PerkComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

public interface Perk {
    String getId();
    Text getName();
    Text getDescription();
    ItemStack getIcon();
    default int getMaxLevel() { return 1; }
    default void onLevelChanged(PlayerEntity player, int level) {}
    default boolean isActive() { return false; }

    /**
     * Дополнительные строки подсказки, зависящие от состояния игрока.
     * Вызывается GUI при отрисовке tooltip.
     */
    default List<Text> getExtraTooltip(
            PlayerEntity player,
            PerkComponent component,
            int level
    ) {
        return List.of();
    }
}