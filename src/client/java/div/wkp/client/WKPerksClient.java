package div.wkp.client;

import div.wkp.PerkComponents;
import div.wkp.PerkUtil;
import div.wkp.network.DoubleJumpPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.util.math.Vec3d;

public final class WKPerksClient implements ClientModInitializer {
    private static int permanentJumpsUsed = 0;
    private static int tempJumpsUsed = 0;
    private static boolean wasOnGround = true;
    private static boolean jumpKeyWasDown = false;

    // Размеры GUI инвентаря
    private static final int INV_WIDTH = 176;
    private static final int INV_HEIGHT = 166;

    @Override
    public void onInitializeClient() {
        registerDoubleJump();
        registerInventoryButton();
    }

    private void registerDoubleJump() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var player = client.player;
            if (player == null || client.world == null) {
                permanentJumpsUsed = 0; tempJumpsUsed = 0;
                wasOnGround = true; jumpKeyWasDown = false;
                return;
            }

            // Блокируем в креативе/спектаторе
            if (!PerkUtil.arePerksEnabled(player)) return;

            boolean onGround = player.isOnGround();
            boolean jumpKeyDown = client.options.jumpKey.isPressed();

            if (onGround) {
                permanentJumpsUsed = 0; tempJumpsUsed = 0;
                wasOnGround = true; jumpKeyWasDown = jumpKeyDown;
                return;
            }

            var comp = PerkComponents.PERK_COMPONENT.get(player);
            int maxPermanent = comp.getPerkLevel("double_jump");
            int availableTemp = comp.getTempJumps() - tempJumpsUsed;

            boolean freshPress = jumpKeyDown && !jumpKeyWasDown;
            jumpKeyWasDown = jumpKeyDown;

            if (wasOnGround) { wasOnGround = false; return; }

            if (freshPress) {
                boolean useTempJump;
                if (permanentJumpsUsed < maxPermanent) {
                    permanentJumpsUsed++; useTempJump = false;
                } else if (availableTemp > 0) {
                    tempJumpsUsed++; useTempJump = true;
                } else return;

                Vec3d look = player.getRotationVector();
                Vec3d horizontal = new Vec3d(look.x, 0.0, look.z).normalize();
                player.setVelocity(horizontal.x * 0.3, 0.5, horizontal.z * 0.3);
                ClientPlayNetworking.send(new DoubleJumpPayload(useTempJump));
            }
        });
    }

    private void registerInventoryButton() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof InventoryScreen inventoryScreen) {
                // Позиция GUI инвентаря
                int guiLeft = (scaledWidth - INV_WIDTH) / 2;
                int guiTop = (scaledHeight - INV_HEIGHT) / 2;

                // Значок-кнопка справа от модели персонажа (как Curios)
                // Модель игрока находится примерно в левой части GUI (x: 26-75)
                int buttonX = guiLeft + 63; // правее модели
                int buttonY = guiTop + 8;

                Screens.getButtons(screen).add(
                        new PerkIconButton(buttonX, buttonY, button -> {
                            client.setScreen(new PerkListScreen(inventoryScreen));
                        })
                );
            }
        });
    }
}