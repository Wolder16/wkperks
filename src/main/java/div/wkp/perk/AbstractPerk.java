package div.wkp.perk;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public abstract class AbstractPerk implements Perk {
    private final String id;
    private final Text name;
    private final Text description;
    private final ItemStack icon;
    private final int maxLevel;

    // Конструктор с указанием максимального уровня
    protected AbstractPerk(String id, Text name, Text description, ItemStack icon, int maxLevel) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.maxLevel = maxLevel;
    }

    // Конструктор для перков с 1 уровнем (по умолчанию)
    protected AbstractPerk(String id, Text name, Text description, ItemStack icon) {
        this(id, name, description, icon, 1);
    }

    @Override
    public String getId() { return id; }

    @Override
    public Text getName() { return name; }

    @Override
    public Text getDescription() { return description; }

    @Override
    public ItemStack getIcon() { return icon; }

    @Override
    public int getMaxLevel() { return maxLevel; }
}