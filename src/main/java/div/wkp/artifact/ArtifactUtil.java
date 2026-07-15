package div.wkp.artifact;

import div.wkp.ArtifactComponents;
import div.wkp.PerkComponents;
import div.wkp.component.ArtifactStateComponent;
import div.wkp.component.PerkComponent;
import div.wkp.entity.SpearProjectileEntity;
import div.wkp.item.ModItems;
import div.wkp.perk.perks.AnomalousBondsPerk;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

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

    public static void preserveInventoryOnDeath(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        ArtifactStateComponent artifactComponent =
                ArtifactComponents.ARTIFACT_STATE_COMPONENT.get(player);
        PerkComponent perkComponent = PerkComponents.PERK_COMPONENT.get(player);

        artifactComponent.clearStoredSoulboundArtifacts();

        boolean preserveEntireInventory = perkComponent.hasPerk(AnomalousBondsPerk.ID)
                && perkComponent.getAnomalousBondsCharges() > 0;

        List<ArtifactStateComponent.StoredStack> captured = new ArrayList<>();

        if (artifactComponent.hasActiveSpear()) {
            captured.add(new ArtifactStateComponent.StoredStack(
                    artifactComponent.getActiveSpearSlot(),
                    new ItemStack(ModItems.ARTIFACT_SPEAR)
            ));
            discardActiveSpearEntity(player, artifactComponent);
            artifactComponent.clearActiveSpear();
        }

        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.getStack(slot);

            if (stack.isEmpty()) {
                continue;
            }

            if (!preserveEntireInventory && !isSoulboundArtifact(stack)) {
                continue;
            }

            captured.add(new ArtifactStateComponent.StoredStack(slot, stack.copy()));
            inventory.setStack(slot, ItemStack.EMPTY);
        }

        for (ArtifactStateComponent.StoredStack storedStack : captured) {
            artifactComponent.storeSoulboundArtifact(
                    storedStack.slot(),
                    storedStack.stack()
            );
        }

        if (preserveEntireInventory) {
            int remainingCharges = perkComponent.getAnomalousBondsCharges() - 1;
            perkComponent.setAnomalousBondsCharges(remainingCharges);

            if (remainingCharges <= 0) {
                perkComponent.clearPerk(AnomalousBondsPerk.ID);
            }
        }

        inventory.markDirty();
    }

    public static void restoreSoulboundArtifacts(ServerPlayerEntity player) {
        ArtifactStateComponent component =
                ArtifactComponents.ARTIFACT_STATE_COMPONENT.get(player);

        if (component.getStoredSoulboundArtifacts().isEmpty()) {
            return;
        }

        List<ArtifactStateComponent.StoredStack> restored = new ArrayList<>(
                component.getStoredSoulboundArtifacts()
        );

        restored.sort(Comparator.comparingInt(ArtifactStateComponent.StoredStack::slot));
        component.clearStoredSoulboundArtifacts();

        List<ItemStack> overflow = new ArrayList<>();

        for (ArtifactStateComponent.StoredStack storedStack : restored) {
            int slot = storedStack.slot();
            ItemStack stack = storedStack.stack().copy();

            if (slot >= 0
                    && slot < player.getInventory().size()
                    && player.getInventory().getStack(slot).isEmpty()) {
                player.getInventory().setStack(slot, stack);
            } else {
                overflow.add(stack);
            }
        }

        for (ItemStack stack : overflow) {
            if (!player.getInventory().insertStack(stack)) {
                player.getInventory().offerOrDrop(stack);
            }
        }

        player.getInventory().markDirty();
        player.currentScreenHandler.sendContentUpdates();
    }

    public static boolean hasActiveSpear(PlayerEntity player) {
        return ArtifactComponents.ARTIFACT_STATE_COMPONENT
                .get(player)
                .hasActiveSpear();
    }

    public static void recallActiveSpear(ServerPlayerEntity player, Hand hand) {
        ArtifactStateComponent component =
                ArtifactComponents.ARTIFACT_STATE_COMPONENT.get(player);

        if (!component.hasActiveSpear()) {
            return;
        }

        Entity entity = getActiveSpearEntity(player, component);
        if (entity instanceof SpearProjectileEntity spearProjectile) {
            spearProjectile.startRecall(hand);
        } else {
            component.clearActiveSpear();
        }
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

    private static Entity getActiveSpearEntity(
            ServerPlayerEntity player,
            ArtifactStateComponent component
    ) {
        if (!component.hasActiveSpear()) {
            return null;
        }

        try {
            return player.getServerWorld().getEntity(UUID.fromString(component.getActiveSpearUuid()));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void discardActiveSpearEntity(
            ServerPlayerEntity player,
            ArtifactStateComponent component
    ) {
        Entity entity = getActiveSpearEntity(player, component);
        if (entity != null) {
            entity.discard();
        }
    }

    private static String getArtifactId(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).toString();
    }
}
