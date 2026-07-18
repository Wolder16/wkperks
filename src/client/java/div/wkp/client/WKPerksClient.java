package div.wkp.client;

import div.wkp.ArtifactComponents;
import div.wkp.PerkComponents;
import div.wkp.PerkUtil;
import div.wkp.artifact.ArtifactUtil;
import div.wkp.artifact.TranslocatorItem;
import div.wkp.block.ModBlockEntities;

import div.wkp.client.mixin.HandledScreenAccessor;
import div.wkp.client.renderer.entity.SpearProjectileRenderer;
import div.wkp.client.renderer.item.ArtifactSpearItemRenderer;
import div.wkp.entity.ModEntities;
import div.wkp.item.ModItems;
import div.wkp.network.ArtifactUsePayload;
import div.wkp.network.DoubleJumpPayload;
import div.wkp.network.OpenPortableBankPayload;
import div.wkp.perk.perks.PortableBankPerk;
import div.wkp.screen.ModScreenHandlers;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.util.math.Vec3d;

public final class WKPerksClient implements ClientModInitializer {
    private static int permanentJumpsUsed = 0;
    private static int tempJumpsUsed = 0;
    private static boolean wasOnGround = true;
    private static boolean jumpKeyWasDown = false;

    private static boolean artifactUseKeyWasDown = false;
    private static net.minecraft.util.Hand activeArtifactHand = null;

    @Override
    public void onInitializeClient() {
        registerDoubleJump();
        registerArtifactInput();
        registerRecallHud();
        registerInventoryButtons();
        registerGeoItemRenderers();
        BlockEntityRendererFactories.register(ModBlockEntities.RHO_ALTAR, div.wkp.client.block.AltarPedestalRenderer::new);
        EntityRendererRegistry.register(ModEntities.SPEAR_PROJECTILE, SpearProjectileRenderer::new);
        HandledScreens.register(
                ModScreenHandlers.TERMINAL,
                TerminalScreen::new
        );
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
                double jumpVelocity = PerkUtil.getScaledDoubleJumpVerticalVelocity(player);
                player.setVelocity(horizontal.x * 0.3, jumpVelocity, horizontal.z * 0.3);
                ClientPlayNetworking.send(new DoubleJumpPayload(useTempJump));
            }
        });
    }

    private void registerArtifactInput() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var player = client.player;
            if (player == null || client.world == null) {
                artifactUseKeyWasDown = false;
                activeArtifactHand = null;
                return;
            }

            boolean useKeyDown = client.options.useKey.isPressed();

            if (activeArtifactHand != null
                    && !ArtifactUtil.isHoldingArtifact(player, activeArtifactHand)) {
                ClientPlayNetworking.send(new ArtifactUsePayload(
                        activeArtifactHand,
                        ArtifactUsePayload.Action.RELEASE,
                        false
                ));
                activeArtifactHand = null;
            }

            if (useKeyDown && !artifactUseKeyWasDown) {
                net.minecraft.util.Hand hand = findArtifactHand(player);

                if (hand != null) {
                    activeArtifactHand = hand;
                    ClientPlayNetworking.send(new ArtifactUsePayload(
                            hand,
                            ArtifactUsePayload.Action.START,
                            false
                    ));
                } else {
                    net.minecraft.util.Hand recallHand = SpearRecallHudOverlay.getRecallHand();
                    if (recallHand != null) {
                        ClientPlayNetworking.send(new ArtifactUsePayload(
                                recallHand,
                                ArtifactUsePayload.Action.RECALL,
                                SpearRecallHudOverlay.isMarkerTargeted()
                        ));
                    }
                }
            }

            if (!useKeyDown && artifactUseKeyWasDown && activeArtifactHand != null) {
                ClientPlayNetworking.send(new ArtifactUsePayload(
                        activeArtifactHand,
                        ArtifactUsePayload.Action.RELEASE,
                        false
                ));
                activeArtifactHand = null;
            }

            if (useKeyDown && activeArtifactHand != null) {
                var stack = player.getStackInHand(activeArtifactHand);
                if (stack.getItem() instanceof TranslocatorItem) {
                    TranslocatorItem.spawnClientPreview(player, player.age);
                }
            }

            artifactUseKeyWasDown = useKeyDown;
        });
    }

    private static net.minecraft.util.Hand findArtifactHand(net.minecraft.entity.player.PlayerEntity player) {
        if (ArtifactUtil.isHoldingArtifact(player, net.minecraft.util.Hand.MAIN_HAND)
                && !player.getItemCooldownManager().isCoolingDown(
                player.getMainHandStack().getItem())) {
            return net.minecraft.util.Hand.MAIN_HAND;
        }

        if (ArtifactUtil.isHoldingArtifact(player, net.minecraft.util.Hand.OFF_HAND)
                && !player.getItemCooldownManager().isCoolingDown(
                player.getOffHandStack().getItem())) {
            return net.minecraft.util.Hand.OFF_HAND;
        }

        return null;
    }

    private void registerRecallHud() {
        HudRenderCallback.EVENT.register(SpearRecallHudOverlay::render);
    }

    private void registerGeoItemRenderers() {
        ArtifactSpearItemRenderer spearRenderer = new ArtifactSpearItemRenderer();

        BuiltinItemRendererRegistry.INSTANCE.register(
                ModItems.ARTIFACT_SPEAR,
                (stack, mode, matrices, vertexConsumers, light, overlay) ->
                        spearRenderer.render(stack, mode, matrices, vertexConsumers, light, overlay)
        );
    }

    private void registerInventoryButtons() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof InventoryScreen inventoryScreen)) {
                return;
            }

            HandledScreenAccessor accessor =
                    (HandledScreenAccessor) inventoryScreen;

            PortableBankButton bankButton = new PortableBankButton(
                    accessor.wkperks$getX() + 128,
                    accessor.wkperks$getY() + 61,
                    button -> ClientPlayNetworking.send(
                            new OpenPortableBankPayload()
                    )
            );

            boolean hasPortableBank = client.player != null
                    && PerkComponents.PERK_COMPONENT
                    .get(client.player)
                    .hasPerk(PortableBankPerk.ID);

            bankButton.visible = hasPortableBank;
            bankButton.active = hasPortableBank;

            Screens.getButtons(screen).add(bankButton);

            ScreenEvents.afterRender(screen).register((currentScreen, context, mouseX, mouseY, tickDelta) -> {
                if (client.player == null) {
                    return;
                }

                HandledScreenAccessor currentAccessor =
                        (HandledScreenAccessor) currentScreen;

                bankButton.setX(currentAccessor.wkperks$getX() + 128);
                bankButton.setY(currentAccessor.wkperks$getY() + 61);

                boolean hasPortableBankNow = PerkComponents.PERK_COMPONENT
                        .get(client.player)
                        .hasPerk(PortableBankPerk.ID);

                bankButton.visible = hasPortableBankNow;
                bankButton.active = hasPortableBankNow;

                PerkOverlayRenderer.render(
                        inventoryScreen,
                        context,
                        mouseX,
                        mouseY
                );
            });
        });
    }
}
