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

    private boolean usingArtifact = false;
    private String activeArtifactId = "";
    private net.minecraft.util.Hand activeHand = net.minecraft.util.Hand.MAIN_HAND;
    private int useTicks = 0;
    private boolean charged = false;

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
    public boolean isUsingArtifact() {
        return usingArtifact;
    }

    @Override
    public String getActiveArtifactId() {
        return activeArtifactId;
    }

    @Override
    public net.minecraft.util.Hand getActiveHand() {
        return activeHand;
    }

    @Override
    public int getUseTicks() {
        return useTicks;
    }

    @Override
    public boolean isCharged() {
        return charged;
    }

    @Override
    public void startUsingArtifact(String artifactId, net.minecraft.util.Hand hand) {
        usingArtifact = true;
        activeArtifactId = artifactId;
        activeHand = hand;
        useTicks = 0;
        charged = false;
    }

    @Override
    public void tickUsingArtifact() {
        if (usingArtifact) {
            useTicks++;
        }
    }

    @Override
    public void setCharged(boolean charged) {
        this.charged = charged;
    }

    @Override
    public void stopUsingArtifact() {
        usingArtifact = false;
        activeArtifactId = "";
        activeHand = net.minecraft.util.Hand.MAIN_HAND;
        useTicks = 0;
        charged = false;
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
        stopUsingArtifact();

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
