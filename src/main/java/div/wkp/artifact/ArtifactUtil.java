package div.wkp.artifact;

import div.wkp.ArtifactComponents;
import div.wkp.component.ArtifactStateComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

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

    public static void startUsingArtifact(ServerPlayerEntity player, Hand hand) {
        ArtifactStateComponent component =
                ArtifactComponents.ARTIFACT_STATE_COMPONENT.get(player);

        if (component.isUsingArtifact()) {
            return;
        }

        ItemStack stack = player.getStackInHand(hand);

        if (!(stack.getItem() instanceof ArtifactItem artifact)) {
            return;
        }

        if (!artifact.canStartUsing(player, hand, stack, component)) {
            return;
        }

        component.startUsingArtifact(getArtifactId(stack), hand);
        artifact.onUseStarted(player, hand, stack, component);
    }

    public static void releaseUsingArtifact(ServerPlayerEntity player, Hand hand) {
        ArtifactStateComponent component =
                ArtifactComponents.ARTIFACT_STATE_COMPONENT.get(player);

        if (!component.isUsingArtifact() || component.getActiveHand() != hand) {
            return;
        }

        ItemStack stack = player.getStackInHand(hand);

        if (!(stack.getItem() instanceof ArtifactItem artifact)
                || !getArtifactId(stack).equals(component.getActiveArtifactId())) {
            component.stopUsingArtifact();
            return;
        }

        artifact.onUseReleased(player, hand, stack, component);
        component.stopUsingArtifact();
    }

    public static void tickUsingArtifact(ServerPlayerEntity player) {
        ArtifactStateComponent component =
                ArtifactComponents.ARTIFACT_STATE_COMPONENT.get(player);

        if (!component.isUsingArtifact()) {
            return;
        }

        Hand hand = component.getActiveHand();
        ItemStack stack = player.getStackInHand(hand);

        if (!(stack.getItem() instanceof ArtifactItem artifact)
                || !getArtifactId(stack).equals(component.getActiveArtifactId())) {
            cancelUsingArtifact(player);
            return;
        }

        component.tickUsingArtifact();
        artifact.onUseTick(player, hand, stack, component);

        if (component.isUsingArtifact() && player.getStackInHand(hand) != stack) {
            cancelUsingArtifact(player);
        }
    }

    public static void cancelUsingArtifact(ServerPlayerEntity player) {
        ArtifactStateComponent component =
                ArtifactComponents.ARTIFACT_STATE_COMPONENT.get(player);

        if (!component.isUsingArtifact()) {
            return;
        }

        Hand hand = component.getActiveHand();
        ItemStack stack = player.getStackInHand(hand);

        if (stack.getItem() instanceof ArtifactItem artifact) {
            artifact.onUseCancelled(player, hand, stack, component);
        }

        component.stopUsingArtifact();
    }

    public static boolean isHoldingArtifact(PlayerEntity player, Hand hand) {
        return player.getStackInHand(hand).getItem() instanceof ArtifactItem;
    }

    private static String getArtifactId(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).toString();
    }
}
