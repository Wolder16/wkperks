package div.wkp.artifact;

import div.wkp.ArtifactComponents;
import div.wkp.component.ArtifactStateComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public final class ArtifactUtil {
    private ArtifactUtil() {
    }

    public static boolean isArtifact(ItemStack stack) {
        return stack.getItem() instanceof ArtifactItem;
    }

    public static boolean isSoulboundArtifact(ItemStack stack) {
        return stack.getItem() instanceof ArtifactItem artifact
                && artifact.isSoulbound(stack);
    }

    public static void storeSoulboundArtifacts(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        ArtifactStateComponent component =
                ArtifactComponents.ARTIFACT_STATE_COMPONENT.get(player);

        component.clearStoredSoulboundArtifacts();

        List<ItemStack> captured = new ArrayList<>();

        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);

            if (!isSoulboundArtifact(stack)) {
                continue;
            }

            captured.add(stack.copy());
            inventory.setStack(slot, ItemStack.EMPTY);
        }

        for (ItemStack stack : captured) {
            component.storeSoulboundArtifact(stack);
        }

        inventory.markDirty();
    }

    public static void restoreSoulboundArtifacts(ServerPlayerEntity player) {
        ArtifactStateComponent component =
                ArtifactComponents.ARTIFACT_STATE_COMPONENT.get(player);

        if (component.getStoredSoulboundArtifacts().isEmpty()) {
            return;
        }

        List<ItemStack> restored = new ArrayList<>(
                component.getStoredSoulboundArtifacts()
        );

        component.clearStoredSoulboundArtifacts();

        for (ItemStack stack : restored) {
            if (!player.getInventory().insertStack(stack)) {
                player.getInventory().offerOrDrop(stack);
            }
        }

        player.getInventory().markDirty();
        player.currentScreenHandler.sendContentUpdates();
    }
}
