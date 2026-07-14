package div.wkp.component;

import div.wkp.PerkComponents;
import div.wkp.perk.Perk;
import div.wkp.perk.PerkRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;

import java.util.HashMap;
import java.util.Map;

public class PlayerPerkComponent implements PerkComponent {
    private final PlayerEntity provider;
    private final Map<String, Integer> perks = new HashMap<>();
    private int tempJumps = 0;

    public PlayerPerkComponent(PlayerEntity provider) {
        this.provider = provider;
    }

    @Override
    public boolean hasPerk(String perkId) {
        return perks.getOrDefault(perkId, 0) > 0;
    }

    @Override
    public int getPerkLevel(String perkId) {
        return perks.getOrDefault(perkId, 0);
    }

    @Override
    public void addPerk(String perkId) {
        changePerkLevel(perkId, 1);
    }

    @Override
    public void removePerk(String perkId) {
        changePerkLevel(perkId, -1);
    }

    @Override
    public void clearPerk(String perkId) {
        int currentLevel = perks.getOrDefault(perkId, 0);

        if (currentLevel > 0) {
            changePerkLevel(perkId, -currentLevel);
        }
    }

    @Override
    public Map<String, Integer> getPerks() {
        return perks;
    }

    // === Временные прыжки ===

    @Override
    public int getTempJumps() {
        return tempJumps;
    }

    @Override
    public void addTempJumps(int amount) {
        tempJumps += amount;
        PerkComponents.PERK_COMPONENT.sync(provider);
    }

    @Override
    public void useTempJump() {
        if (tempJumps > 0) {
            tempJumps--;
            PerkComponents.PERK_COMPONENT.sync(provider);
        }
    }

    @Override
    public void clearTempJumps() {
        tempJumps = 0;
        PerkComponents.PERK_COMPONENT.sync(provider);
    }

    // === NBT ===

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound perksTag = new NbtCompound();
        for (Map.Entry<String, Integer> entry : perks.entrySet()) {
            perksTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("UnlockedPerks", perksTag);
        tag.putInt("TempJumps", tempJumps);
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        perks.clear();
        NbtCompound perksTag = tag.getCompound("UnlockedPerks");
        for (String key : perksTag.getKeys()) {
            perks.put(key, perksTag.getInt(key));
        }
        tempJumps = tag.getInt("TempJumps");

        for (Map.Entry<String, Integer> entry : perks.entrySet()) {
            Perk perk = PerkRegistry.get(entry.getKey());
            if (perk != null) {
                perk.onLevelChanged(provider, entry.getValue());
            }
        }
    }
    @Override
    public void changePerkLevel(String perkId, int delta) {
        Perk perk = PerkRegistry.get(perkId);

        if (perk == null || delta == 0) {
            return;
        }

        int currentLevel = perks.getOrDefault(perkId, 0);

        int newLevel = Math.max(
                0,
                Math.min(perk.getMaxLevel(), currentLevel + delta)
        );

        if (newLevel == currentLevel) {
            return;
        }

        if (newLevel == 0) {
            perks.remove(perkId);
        } else {
            perks.put(perkId, newLevel);
        }

        perk.onLevelChanged(provider, newLevel);

        PerkComponents.PERK_COMPONENT.sync(provider);
    }
}