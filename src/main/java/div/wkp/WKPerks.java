
package div.wkp;


import div.wkp.artifact.ArtifactUtil;
import div.wkp.block.ModBlocks;
import div.wkp.block.ModBlockEntities;
import div.wkp.config.WKPerksConfig;
import div.wkp.entity.ModEntities;
import div.wkp.item.ModItems;
import div.wkp.altar.AltarRegistry;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import div.wkp.screen.ModScreenHandlers;
import div.wkp.network.ArtifactUsePayload;
import div.wkp.network.DoubleJumpPayload;
import div.wkp.network.OpenPortableBankPayload;
import div.wkp.perk.Perk;
import div.wkp.perk.PerkRegistry;
import div.wkp.perk.perks.ConsumptiveReflexPerk;
import div.wkp.perk.perks.PortableBankPerk;
import div.wkp.perk.perks.ProfitMotivePerk;
import div.wkp.perk.perks.TissueRestorativesPerk;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.Hand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WKPerks implements ModInitializer {
    public static final String MOD_ID = "wkperks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final SuggestionProvider<ServerCommandSource> PERK_SUGGESTIONS =
            (context, builder) -> {
                for (Perk perk : PerkRegistry.getAll()) builder.suggest(perk.getId());
                return builder.buildFuture();
            };

    private static final Set<UUID> FALL_DAMAGE_IMMUNITY = new HashSet<>();
    // Запоминаем последний режим каждого игрока, чтобы ловить смену
    private static final Map<UUID, GameMode> LAST_GAMEMODE = new HashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("WKPerks: инициализация...");
        WKPerksConfig.load();
        ModItems.initialize();
        ModBlocks.initialize();
        ModScreenHandlers.initialize();
        ModBlockEntities.initialize();
        ModEntities.initialize();
        PerkRegistry.init();
        AltarRegistry.init();
        PayloadTypeRegistry.playC2S().register(DoubleJumpPayload.ID, DoubleJumpPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(OpenPortableBankPayload.ID, OpenPortableBankPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ArtifactUsePayload.ID, ArtifactUsePayload.CODEC);

        // Приём двойного прыжка
        ServerPlayNetworking.registerGlobalReceiver(DoubleJumpPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();

                // Блокируем прыжок в креативе/спектаторе
                if (!PerkUtil.arePerksEnabled(player)) return;

                var comp = PerkComponents.PERK_COMPONENT.get(player);
                boolean allowed = false;

                if (payload.useTempJump()) {
                    if (comp.getTempJumps() > 0) {
                        comp.useTempJump();
                        allowed = true;
                    }
                } else {
                    if (comp.hasPerk("double_jump")) allowed = true;
                }

                if (allowed) {
                    Vec3d look = player.getRotationVector();
                    Vec3d horizontal = new Vec3d(look.x, 0.0, look.z).normalize();
                    double jumpVelocity = PerkUtil.getScaledDoubleJumpVerticalVelocity(player);
                    player.setVelocity(horizontal.x * 0.3, jumpVelocity, horizontal.z * 0.3);
                    player.velocityModified = true;
                    player.fallDistance = 0.0F;
                    FALL_DAMAGE_IMMUNITY.add(player.getUuid());
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(OpenPortableBankPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();

                if (!PerkUtil.arePerksEnabled(player)) {
                    return;
                }

                if (!PerkComponents.PERK_COMPONENT
                        .get(player)
                        .hasPerk(PortableBankPerk.ID)) {
                    return;
                }

                player.openHandledScreen(
                        new SimpleNamedScreenHandlerFactory(
                                (syncId, inventory, openedPlayer) ->
                                        GenericContainerScreenHandler.createGeneric9x3(
                                                syncId,
                                                inventory,
                                                openedPlayer.getEnderChestInventory()
                                        ),
                                Text.translatable("container.enderchest")
                        )
                );
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(ArtifactUsePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();

                if (payload.action() == ArtifactUsePayload.Action.START) {
                    if (ArtifactUtil.hasActiveSpear(player)
                            && !ArtifactUtil.isHoldingArtifact(player, Hand.MAIN_HAND)
                            && !ArtifactUtil.isHoldingArtifact(player, Hand.OFF_HAND)) {
                        ArtifactUtil.recallActiveSpear(player, payload.hand());
                    } else {
                        ArtifactUtil.startUsingArtifact(player, payload.hand());
                    }
                } else if (payload.action() == ArtifactUsePayload.Action.RELEASE) {
                    ArtifactUtil.releaseUsingArtifact(player, payload.hand());
                }
            });
        });

        // Серверный тик: защита от падения + отслеживание смены режима
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Защита от урона от падения
            var it = FALL_DAMAGE_IMMUNITY.iterator();
            while (it.hasNext()) {
                UUID uuid = it.next();
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                if (player == null) { it.remove(); continue; }
                player.fallDistance = 0.0F;
                if (player.isOnGround()) it.remove();
            }

            // Отслеживание смены режима игры
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID uuid = player.getUuid();

                GameMode currentMode =
                        player.interactionManager.getGameMode();

                GameMode previousMode = LAST_GAMEMODE.put(uuid, currentMode);

                if (previousMode != currentMode) {
                    PerkUtil.refreshPlayer(player);
                }

                ArtifactUtil.tickUsingArtifact(player);
                ConsumptiveReflexPerk.tick(player);
                ProfitMotivePerk.tick(player);
                TissueRestorativesPerk.tick(player);
            }

        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerUuid = handler.player.getUuid();
            LAST_GAMEMODE.remove(playerUuid);
            ArtifactUtil.cancelUsingArtifact(handler.player);
            ConsumptiveReflexPerk.clearTracking(playerUuid);
            ProfitMotivePerk.clearTracking(playerUuid);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            ArtifactUtil.restoreSoulboundArtifacts(newPlayer);
        });
        registerCommands();
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("perk")
                    .then(CommandManager.literal("add")
                            .then(CommandManager.argument("id", StringArgumentType.string())
                                    .suggests(PERK_SUGGESTIONS)
                                    .executes(ctx -> {
                                        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                        String id = StringArgumentType.getString(ctx, "id");
                                        if (!PerkRegistry.exists(id)) {
                                            ctx.getSource().sendError(Text.literal("Перк не найден: " + id));
                                            return 0;
                                        }
                                        PerkComponents.PERK_COMPONENT.get(player).addPerk(id);
                                        ctx.getSource().sendFeedback(() -> Text.literal("Перк добавлен: " + id), false);
                                        return 1;
                                    })))
                    .then(CommandManager.literal("remove")
                            .then(CommandManager.argument("id", StringArgumentType.string())
                                    .suggests(PERK_SUGGESTIONS)
                                    .executes(ctx -> {
                                        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                        String id = StringArgumentType.getString(ctx, "id");
                                        PerkComponents.PERK_COMPONENT.get(player).removePerk(id);
                                        ctx.getSource().sendFeedback(() -> Text.literal("Перк удалён: " + id), false);
                                        return 1;
                                    })))
                    .then(CommandManager.literal("clear")
                            .then(CommandManager.argument("id", StringArgumentType.string())
                                    .suggests(PERK_SUGGESTIONS)
                                    .executes(ctx -> {
                                        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                        String id = StringArgumentType.getString(ctx, "id");
                                        PerkComponents.PERK_COMPONENT.get(player).clearPerk(id);
                                        ctx.getSource().sendFeedback(() -> Text.literal("Сброшено: " + id), false);
                                        return 1;
                                    })))
                    .then(CommandManager.literal("clearall")
                            .executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                var comp = PerkComponents.PERK_COMPONENT.get(player);

                                var perkIds = new ArrayList<>(comp.getPerks().keySet());
                                for (String perkId : perkIds) {
                                    comp.clearPerk(perkId);
                                }

                                comp.clearTempJumps();
                                comp.setAnomalousBondsCharges(0);

                                ctx.getSource().sendFeedback(() -> Text.literal("Все перки удалены."), false);
                                return 1;
                            }))
                    .then(CommandManager.literal("list")
                            .executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                var comp = PerkComponents.PERK_COMPONENT.get(player);
                                var perks = comp.getPerks();
                                if (perks.isEmpty()) {
                                    ctx.getSource().sendFeedback(() -> Text.literal("У вас нет перков."), false);
                                } else {
                                    StringBuilder sb = new StringBuilder("Перки: ");
                                    perks.forEach((id, lvl) -> sb.append(id).append(" (").append(lvl).append("), "));
                                    if (comp.getTempJumps() > 0) sb.append("| Temp: ").append(comp.getTempJumps());
                                    ctx.getSource().sendFeedback(() -> Text.literal(sb.toString()), false);
                                }
                                return 1;
                            }))
            );
        });
    }
}

