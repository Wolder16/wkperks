package div.wkp.terminal;

import div.wkp.PerkComponents;
import div.wkp.block.ModBlockEntities;
import div.wkp.component.PerkComponent;
import div.wkp.perk.Perk;
import div.wkp.perk.PerkRegistry;
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

    private final Map<UUID, PlayerTerminalData> playerData =
            new HashMap<>();

    public TerminalBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(ModBlockEntities.TERMINAL, pos, state);
    }

    // =====================================================================
    // Открытие интерфейса
    // =====================================================================

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

    @Override
    public Text getDisplayName() {
        return Text.literal("QuietOS Terminal");
    }

    // =====================================================================
    // Рероллы
    // =====================================================================

    public void addRerollCharge(UUID playerUuid) {
        PlayerTerminalData data =
                getOrCreateData(playerUuid);

        data.rerollCharges++;

        markDirty();
        updateListeners();
    }

    public int getRerollCharges(UUID playerUuid) {
        return getOrCreateData(playerUuid).rerollCharges;
    }

    public boolean reroll(ServerPlayerEntity player) {
        PlayerTerminalData data =
                getOrCreateData(player.getUuid());

        if (data.rerollCharges <= 0) {
            return false;
        }

        data.rerollCharges--;

        data.currentOffers =
                generateOffers(player);

        markDirty();
        updateListeners();

        return true;
    }

    // =====================================================================
    // Покупка перка
    // =====================================================================

    public boolean purchasePerk(
            ServerPlayerEntity player,
            int offerSlot
    ) {
        ensureOffers(player);

        PlayerTerminalData data =
                getOrCreateData(player.getUuid());

        if (offerSlot < 0
                || offerSlot >= data.currentOffers.size()) {
            return false;
        }

        String perkId =
                data.currentOffers.get(offerSlot);

        Perk perk =
                PerkRegistry.get(perkId);

        if (perk == null) {
            return false;
        }

        PerkComponent component =
                PerkComponents.PERK_COMPONENT.get(player);

        int currentLevel =
                component.getPerkLevel(perkId);

        if (currentLevel >= perk.getMaxLevel()) {
            return false;
        }

        // Покупка бесплатная.
        component.changePerkLevel(perkId, 1);

        int levelAfter =
                component.getPerkLevel(perkId);

        if (levelAfter <= currentLevel) {
            return false;
        }

        /*
         * Убираем купленный перк из текущих предложений.
         * Затем добавляем другой доступный перк, если он существует.
         */
        data.currentOffers.remove(offerSlot);
        ensureOffers(player);

        markDirty();
        updateListeners();

        return true;
    }

    // =====================================================================
    // Работа с предложениями
    // =====================================================================

    public int getOfferIndex(
            UUID playerUuid,
            int offerSlot
    ) {
        PlayerTerminalData data =
                getOrCreateData(playerUuid);

        if (offerSlot < 0
                || offerSlot >= data.currentOffers.size()) {
            return -1;
        }

        String offerId =
                data.currentOffers.get(offerSlot);

        int index = 0;

        for (Perk perk : PerkRegistry.getAll()) {
            if (perk.getId().equals(offerId)) {
                return index;
            }

            index++;
        }

        return -1;
    }

    /**
     * Проверяет текущий список и добавляет недостающие предложения.
     */
    private void ensureOffers(ServerPlayerEntity player) {
        PlayerTerminalData data =
                getOrCreateData(player.getUuid());

        PerkComponent component =
                PerkComponents.PERK_COMPONENT.get(player);

        data.currentOffers.removeIf(id -> {
            Perk perk = PerkRegistry.get(id);

            return perk == null
                    || component.getPerkLevel(id)
                    >= perk.getMaxLevel();
        });

        List<Perk> available = new ArrayList<>();

        for (Perk perk : PerkRegistry.getAll()) {
            boolean alreadyOffered =
                    data.currentOffers.contains(perk.getId());

            boolean notMaxed =
                    component.getPerkLevel(perk.getId())
                            < perk.getMaxLevel();

            if (!alreadyOffered && notMaxed) {
                available.add(perk);
            }
        }

        Collections.shuffle(available);

        for (Perk perk : available) {
            if (data.currentOffers.size() >= 2) {
                break;
            }

            data.currentOffers.add(perk.getId());
        }
    }

    /**
     * Генерирует полностью новый ассортимент.
     */
    private List<String> generateOffers(
            ServerPlayerEntity player
    ) {
        PerkComponent component =
                PerkComponents.PERK_COMPONENT.get(player);

        List<Perk> available =
                new ArrayList<>();

        for (Perk perk : PerkRegistry.getAll()) {
            if (component.getPerkLevel(perk.getId())
                    < perk.getMaxLevel()) {
                available.add(perk);
            }
        }

        Collections.shuffle(available);

        List<String> result =
                new ArrayList<>();

        for (Perk perk : available) {
            if (result.size() >= 2) {
                break;
            }

            result.add(perk.getId());
        }

        return result;
    }

    private PlayerTerminalData getOrCreateData(
            UUID playerUuid
    ) {
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

        NbtCompound playersTag =
                new NbtCompound();

        for (Map.Entry<UUID, PlayerTerminalData> entry
                : playerData.entrySet()) {

            NbtCompound playerTag =
                    new NbtCompound();

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

        NbtCompound playersTag =
                nbt.getCompound("PlayerData");

        for (String uuidString : playersTag.getKeys()) {
            try {
                UUID uuid =
                        UUID.fromString(uuidString);

                PlayerTerminalData data =
                        new PlayerTerminalData();

                data.readNbt(
                        playersTag.getCompound(uuidString)
                );

                playerData.put(uuid, data);
            } catch (IllegalArgumentException ignored) {
                // Игнорируем повреждённую запись UUID.
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
    // Данные одного игрока
    // =====================================================================

    private static class PlayerTerminalData {
        private int rerollCharges = 0;
        private List<String> currentOffers = new ArrayList<>();

        private void writeNbt(NbtCompound nbt) {
            nbt.putInt("Rerolls", rerollCharges);

            NbtCompound offersTag =
                    new NbtCompound();

            for (int i = 0; i < currentOffers.size(); i++) {
                offersTag.putString(
                        "Offer" + i,
                        currentOffers.get(i)
                );
            }

            nbt.put("Offers", offersTag);
        }

        private void readNbt(NbtCompound nbt) {
            rerollCharges =
                    nbt.getInt("Rerolls");

            currentOffers.clear();

            NbtCompound offersTag =
                    nbt.getCompound("Offers");

            for (int i = 0; i < 2; i++) {
                String id =
                        offersTag.getString("Offer" + i);

                if (!id.isEmpty()) {
                    currentOffers.add(id);
                }
            }
        }
    }
}