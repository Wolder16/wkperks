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
    private final List<StoredStack> storedSoulboundArtifacts = new ArrayList<>();

    private String activeSpearUuid = "";
    private int activeSpearSlot = -1;

    private boolean usingArtifact = false;
    private String activeArtifactId = "";
    private net.minecraft.util.Hand activeHand = net.minecraft.util.Hand.MAIN_HAND;
    private int useTicks = 0;
    private boolean charged = false;

    public PlayerArtifactStateComponent(PlayerEntity provider) {
        this.provider = provider;
    }

    @Override
    public List<StoredStack> getStoredSoulboundArtifacts() {
        return storedSoulboundArtifacts;
    }

    @Override
    public void storeSoulboundArtifact(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        storedSoulboundArtifacts.add(new StoredStack(slot, stack.copy()));
        ArtifactComponents.ARTIFACT_STATE_COMPONENT.sync(provider);
    }

    @Override
    public void clearStoredSoulboundArtifacts() {
        storedSoulboundArtifacts.clear();
        ArtifactComponents.ARTIFACT_STATE_COMPONENT.sync(provider);
    }

    @Override
    public boolean hasActiveSpear() {
        return !activeSpearUuid.isEmpty();
    }

    @Override
    public String getActiveSpearUuid() {
        return activeSpearUuid;
    }

    @Override
    public int getActiveSpearSlot() {
        return activeSpearSlot;
    }

    @Override
    public void setActiveSpear(String spearUuid, int slot) {
        activeSpearUuid = spearUuid;
        activeSpearSlot = slot;
        ArtifactComponents.ARTIFACT_STATE_COMPONENT.sync(provider);
    }

    @Override
    public void clearActiveSpear() {
        activeSpearUuid = "";
        activeSpearSlot = -1;
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

        for (StoredStack storedStack : storedSoulboundArtifacts) {
            NbtCompound entry = new NbtCompound();
            entry.putInt("Slot", storedStack.slot());
            entry.put("Stack", storedStack.stack().encode(registryLookup));
            list.add(entry);
        }

        tag.put("StoredSoulboundArtifacts", list);
        tag.putString("ActiveSpearUuid", activeSpearUuid);
        tag.putInt("ActiveSpearSlot", activeSpearSlot);
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        storedSoulboundArtifacts.clear();
        stopUsingArtifact();
        activeSpearUuid = tag.getString("ActiveSpearUuid");
        activeSpearSlot = tag.getInt("ActiveSpearSlot");

        NbtList list = tag.getList("StoredSoulboundArtifacts", 10);

        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            int slot = entry.getInt("Slot");
            ItemStack stack = ItemStack.fromNbt(registryLookup, entry.getCompound("Stack"))
                    .orElse(ItemStack.EMPTY);

            if (!stack.isEmpty()) {
                storedSoulboundArtifacts.add(new StoredStack(slot, stack));
            }
        }
    }
}
