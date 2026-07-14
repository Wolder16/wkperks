package div.wkp.terminal;

import div.wkp.perk.Perk;
import div.wkp.perk.PerkRegistry;
import div.wkp.screen.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class TerminalScreenHandler extends ScreenHandler {
    public static final int BUY_OFFER_0 = 0;
    public static final int BUY_OFFER_1 = 1;
    public static final int REROLL_BUTTON = 2;

    /**
     * Индексы PropertyDelegate:
     *
     * 0 — индекс первого предложенного перка
     * 1 — индекс второго предложенного перка
     * 2 — количество рероллов
     */
    private static final int PROPERTY_COUNT = 3;

    private final TerminalBlockEntity terminal;
    private final PropertyDelegate properties;

    /**
     * Серверный конструктор.
     */
    public TerminalScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            TerminalBlockEntity terminal
    ) {
        this(
                syncId,
                playerInventory,
                terminal,
                new TerminalPropertyDelegate(
                        terminal,
                        playerInventory.player.getUuid()
                )
        );
    }

    /**
     * Клиентский конструктор.
     * Используется ScreenHandlerType.
     */
    public TerminalScreenHandler(
            int syncId,
            PlayerInventory playerInventory
    ) {
        this(
                syncId,
                playerInventory,
                null,
                new ArrayPropertyDelegate(PROPERTY_COUNT)
        );
    }

    private TerminalScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            TerminalBlockEntity terminal,
            PropertyDelegate properties
    ) {
        super(ModScreenHandlers.TERMINAL, syncId);

        this.terminal = terminal;
        this.properties = properties;
        UUID playerUuid = playerInventory.player.getUuid();

        this.addProperties(properties);
    }

    /**
     * Возвращает перк, отображаемый в указанной карточке.
     */
    public Perk getOffer(int slot) {
        if (slot < 0 || slot > 1) {
            return null;
        }

        int perkIndex = properties.get(slot);

        if (perkIndex < 0) {
            return null;
        }

        return getPerkByIndex(perkIndex);
    }

    public int getRerollCharges() {
        return properties.get(2);
    }

    /**
     * Получает перк по его индексу в PerkRegistry.
     */
    private static Perk getPerkByIndex(int index) {
        int currentIndex = 0;

        for (Perk perk : PerkRegistry.getAll()) {
            if (currentIndex == index) {
                return perk;
            }

            currentIndex++;
        }

        return null;
    }

    /**
     * Серверная обработка нажатий кнопок.
     */
    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (terminal == null) {
            return false;
        }

        if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)) {
            return false;
        }

        boolean changed;

        if (id == BUY_OFFER_0) {
            changed = terminal.purchasePerk(serverPlayer, 0);
        } else if (id == BUY_OFFER_1) {
            changed = terminal.purchasePerk(serverPlayer, 1);
        } else if (id == REROLL_BUTTON) {
            changed = terminal.reroll(serverPlayer);
        } else {
            return false;
        }

        if (changed) {
            sendContentUpdates();
        }

        return changed;
    }

    /**
     * Проверка, что игрок всё ещё находится рядом с терминалом.
     */
    @Override
    public boolean canUse(PlayerEntity player) {
        // На клиенте terminal отсутствует.
        if (terminal == null) {
            return true;
        }

        if (terminal.getWorld() != player.getWorld()) {
            return false;
        }

        if (Objects.requireNonNull(terminal.getWorld()).getBlockEntity(terminal.getPos()) != terminal) {
            return false;
        }

        Vec3d terminalCenter = Vec3d.ofCenter(terminal.getPos());

        return player.squaredDistanceTo(terminalCenter) <= 64.0D;
    }

    /**
     * В терминале нет слотов инвентаря.
     */
    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    /**
         * PropertyDelegate для серверной стороны.
         */
        private record TerminalPropertyDelegate(TerminalBlockEntity terminal, UUID playerUuid) implements PropertyDelegate {

        @Override
            public int get(int index) {
                return switch (index) {
                    case 0, 1 -> terminal.getOfferIndex(playerUuid, index);
                    case 2 -> terminal.getRerollCharges(playerUuid);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Значения изменяются только сервером.
            }

            @Override
            public int size() {
                return PROPERTY_COUNT;
            }
        }
}