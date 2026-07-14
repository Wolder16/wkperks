package div.wkp.terminal;

import div.wkp.PerkComponents;
import div.wkp.block.ModBlockEntities;
import div.wkp.component.PerkComponent;
import div.wkp.perk.Perk;
import div.wkp.perk.PerkRegistry;
import div.wkp.perk.perks.RhoGracePerk;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TerminalBlockEntity
        extends BlockEntity
        implements NamedScreenHandlerFactory {

    private static final int OFFER_COUNT = 2;

    private final Map<UUID, PlayerTerminalData> playerData = new HashMap<>();

    public TerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TERMINAL, pos, state);
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("QuietOS Terminal");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(
            int syncId,
            PlayerInventory playerInventory,
            PlayerEntity player
    ) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return null;
        }

        ensureOffers(serverPlayer);

        return new TerminalScreenHandler(
                syncId,
                playerInventory,
                this
        );
    }

    // =====================================================================
    // Rerolls
    // =====================================================================

    public void addRerollCharge(UUID playerUuid) {
        PlayerTerminalData data = getOrCreateData(playerUuid);

        data.rerollCharges++;

        markDirty();
        updateListeners();
    }

    public int getRerollCharges(UUID playerUuid) {
        return getOrCreateData(playerUuid).rerollCharges;
    }

    public boolean reroll(ServerPlayerEntity player) {
        PlayerTerminalData data = getOrCreateData(player.getUuid());

        if (data.rerollCharges <= 0) {
            return false;
        }

        data.rerollCharges--;
        data.currentOffers = generateOffers(player);
        data.purchasedSlot = -1;

        markDirty();
        updateListeners();

        return true;
    }

    // =====================================================================
    // Purchase
    // =====================================================================

    public boolean purchasePerk(ServerPlayerEntity player, int offerSlot) {
        ensureOffers(player);

        PlayerTerminalData data = getOrCreateData(player.getUuid());

        if (data.purchasedSlot != -1) {
            return false;
        }

        if (offerSlot < 0 || offerSlot >= data.currentOffers.size()) {
            return false;
        }

        String perkId = data.currentOffers.get(offerSlot);
        Perk perk = PerkRegistry.get(perkId);

        if (perk == null) {
            return false;
        }

        if (isTerminalExcluded(perkId)) {
            return false;
        }

        PerkComponent component = PerkComponents.PERK_COMPONENT.get(player);

        int oldLevel = component.getPerkLevel(perkId);

        if (oldLevel >= perk.getMaxLevel()) {
            return false;
        }

        component.changePerkLevel(perkId, 1);

        int newLevel = component.getPerkLevel(perkId);

        if (newLevel <= oldLevel) {
            return false;
        }

        /*
         * ВАЖНО:
         * После покупки НЕ обновляем offers.
         * Просто запоминаем, какой слот куплен.
         */
        data.purchasedSlot = offerSlot;

        markDirty();
        updateListeners();

        return true;
    }

    // =====================================================================
    // Properties for ScreenHandler
    // =====================================================================

    public int getOfferIndex(UUID playerUuid, int offerSlot) {
        PlayerTerminalData data = getOrCreateData(playerUuid);

        if (offerSlot < 0 || offerSlot >= data.currentOffers.size()) {
            return -1;
        }

        String offerId = data.currentOffers.get(offerSlot);

        int index = 0;

        for (Perk perk : PerkRegistry.getAll()) {
            if (perk.getId().equals(offerId)) {
                return index;
            }

            index++;
        }

        return -1;
    }

    public int getPurchasedSlot(UUID playerUuid) {
        return getOrCreateData(playerUuid).purchasedSlot;
    }

    // =====================================================================
    // Offer generation
    // =====================================================================

    private void ensureOffers(ServerPlayerEntity player) {
        PlayerTerminalData data = getOrCreateData(player.getUuid());

        /*
         * Если игрок уже купил один из текущих перков,
         * НЕ трогаем список вообще до ручного REFRESH.
         */
        if (data.purchasedSlot != -1) {
            return;
        }

        PerkComponent component = PerkComponents.PERK_COMPONENT.get(player);

        data.currentOffers.removeIf(id -> {
            Perk perk = PerkRegistry.get(id);
            return !canOfferPerk(perk, component);
        });

        fillOffers(data, component);

        markDirty();
    }

    private void fillOffers(
            PlayerTerminalData data,
            PerkComponent component
    ) {
        if (data.currentOffers.size() >= OFFER_COUNT) {
            return;
        }

        List<Perk> available = new ArrayList<>();

        for (Perk perk : PerkRegistry.getAll()) {
            if (data.currentOffers.contains(perk.getId())) {
                continue;
            }

            if (canOfferPerk(perk, component)) {
                available.add(perk);
            }
        }

        Collections.shuffle(available);

        for (Perk perk : available) {
            if (data.currentOffers.size() >= OFFER_COUNT) {
                break;
            }

            data.currentOffers.add(perk.getId());
        }
    }

    private List<String> generateOffers(ServerPlayerEntity player) {
        PerkComponent component = PerkComponents.PERK_COMPONENT.get(player);

        List<Perk> available = new ArrayList<>();

        for (Perk perk : PerkRegistry.getAll()) {
            if (canOfferPerk(perk, component)) {
                available.add(perk);
            }
        }

        Collections.shuffle(available);

        List<String> result = new ArrayList<>();

        for (Perk perk : available) {
            if (result.size() >= OFFER_COUNT) {
                break;
            }

            result.add(perk.getId());
        }

        return result;
    }

    private static boolean canOfferPerk(
            @Nullable Perk perk,
            PerkComponent component
    ) {
        if (perk == null) {
            return false;
        }

        if (isTerminalExcluded(perk.getId())) {
            return false;
        }

        return component.getPerkLevel(perk.getId()) < perk.getMaxLevel();
    }

    private static boolean isTerminalExcluded(String perkId) {
        return RhoGracePerk.ID.equals(perkId);
    }

    private PlayerTerminalData getOrCreateData(UUID playerUuid) {
        return playerData.computeIfAbsent(
                playerUuid,
                uuid -> new PlayerTerminalData()
        );
    }

    // =====================================================================
    // NBT
    // =====================================================================

    @Override
    protected void writeNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup lookup
    ) {
        super.writeNbt(nbt, lookup);

        NbtCompound playersTag = new NbtCompound();

        for (Map.Entry<UUID, PlayerTerminalData> entry : playerData.entrySet()) {
            NbtCompound playerTag = new NbtCompound();

            entry.getValue().writeNbt(playerTag);

            playersTag.put(
                    entry.getKey().toString(),
                    playerTag
            );
        }

        nbt.put("PlayerData", playersTag);
    }

    @Override
    protected void readNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup lookup
    ) {
        super.readNbt(nbt, lookup);

        playerData.clear();

        NbtCompound playersTag = nbt.getCompound("PlayerData");

        for (String uuidString : playersTag.getKeys()) {
            try {
                UUID uuid = UUID.fromString(uuidString);

                PlayerTerminalData data = new PlayerTerminalData();
                data.readNbt(playersTag.getCompound(uuidString));

                playerData.put(uuid, data);
            } catch (IllegalArgumentException ignored) {
                // повреждённая UUID-запись игнорируется
            }
        }
    }

    private void updateListeners() {
        if (world == null) {
            return;
        }

        world.updateListeners(
                pos,
                getCachedState(),
                getCachedState(),
                3
        );
    }

    // =====================================================================
    // Player-specific terminal data
    // =====================================================================

    private static class PlayerTerminalData {
        private int rerollCharges = 0;
        private List<String> currentOffers = new ArrayList<>();

        /*
         * -1 = ещё ничего не куплено
         *  0 = куплен левый слот
         *  1 = куплен правый слот
         */
        private int purchasedSlot = -1;

        private void writeNbt(NbtCompound nbt) {
            nbt.putInt("Rerolls", rerollCharges);
            nbt.putInt("PurchasedSlot", purchasedSlot);

            NbtCompound offersTag = new NbtCompound();

            for (int i = 0; i < currentOffers.size(); i++) {
                offersTag.putString(
                        "Offer" + i,
                        currentOffers.get(i)
                );
            }

            nbt.put("Offers", offersTag);
        }

        private void readNbt(NbtCompound nbt) {
            rerollCharges = nbt.getInt("Rerolls");

            if (nbt.contains("PurchasedSlot")) {
                purchasedSlot = nbt.getInt("PurchasedSlot");
            } else {
                purchasedSlot = -1;
            }

            if (purchasedSlot < -1 || purchasedSlot > 1) {
                purchasedSlot = -1;
            }

            currentOffers.clear();

            NbtCompound offersTag = nbt.getCompound("Offers");

            for (int i = 0; i < OFFER_COUNT; i++) {
                String id = offersTag.getString("Offer" + i);

                if (!id.isEmpty()) {
                    currentOffers.add(id);
                }
            }
        }
    }
}