package div.wkp.component;

import div.wkp.ArtifactComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;

import java.util.ArrayList;
import java.util.List;

public class PlayerArtifactStateComponent implements ArtifactStateComponent {
    private final PlayerEntity provider;
    private final List<ItemStack> storedSoulboundArtifacts = new ArrayList<>();

    public PlayerArtifactStateComponent(PlayerEntity provider) {
        this.provider = provider;
    }

    @Override
    public List<ItemStack> getStoredSoulboundArtifacts() {
        return storedSoulboundArtifacts;
    }

    @Override
    public void storeSoulboundArtifact(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        storedSoulboundArtifacts.add(stack.copy());
        ArtifactComponents.ARTIFACT_STATE_COMPONENT.sync(provider);
    }

    @Override
    public void clearStoredSoulboundArtifacts() {
        storedSoulboundArtifacts.clear();
        ArtifactComponents.ARTIFACT_STATE_COMPONENT.sync(provider);
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList list = new NbtList();

        for (ItemStack stack : storedSoulboundArtifacts) {
            list.add(stack.encode(registryLookup));
        }

        tag.put("StoredSoulboundArtifacts", list);
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        storedSoulboundArtifacts.clear();

        NbtList list = tag.getList("StoredSoulboundArtifacts", 10);

        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.fromNbt(registryLookup, list.getCompound(i))
                    .orElse(ItemStack.EMPTY);

            if (!stack.isEmpty()) {
                storedSoulboundArtifacts.add(stack);
            }
        }
    }
}
