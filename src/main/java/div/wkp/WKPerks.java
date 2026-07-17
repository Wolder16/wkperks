
package div.wkp;


import div.wkp.artifact.ArtifactUtil;
import div.wkp.block.ModBlocks;
import div.wkp.block.ModBlockEntities;
import div.wkp.config.WKPerksConfig;
import div.wkp.entity.ModEntities;
import div.wkp.item.ModItems;
import div.wkp.altar.AltarRegistry;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
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
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class WKPerks implements ModInitializer {
    public static final String MOD_ID = "wkperks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final SuggestionProvider<ServerCommandSource> PERK_SUGGESTIONS =
            (context, builder) -> {
                for (Perk perk : PerkRegistry.getAll()) {
                    builder.suggest(perk.getId());
                }
                return builder.buildFuture();
            };

    private static final SuggestionProvider<ServerCommandSource> ADDABLE_PERK_SUGGESTIONS =
            (context, builder) -> suggestPerksForTargets(context, builder, true);

    private static final SuggestionProvider<ServerCommandSource> REMOVABLE_PERK_SUGGESTIONS =
            (context, builder) -> suggestPerksForTargets(context, builder, false);

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
                    ArtifactUtil.startUsingArtifact(player, payload.hand());
                } else if (payload.action() == ArtifactUsePayload.Action.RELEASE) {
                    ArtifactUtil.releaseUsingArtifact(player, payload.hand());
                } else if (payload.action() == ArtifactUsePayload.Action.RECALL) {
                    ArtifactUtil.recallActiveSpear(player, payload.hand());
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

    private static CompletableFuture<Suggestions> suggestPerksForTargets(
            CommandContext<ServerCommandSource> context,
            SuggestionsBuilder builder,
            boolean forAdd
    ) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(context, "target");

        for (Perk perk : PerkRegistry.getAll()) {
            boolean shouldSuggest = false;

            for (ServerPlayerEntity target : targets) {
                int level = PerkComponents.PERK_COMPONENT.get(target).getPerkLevel(perk.getId());

                if (forAdd) {
                    if (level < perk.getMaxLevel()) {
                        shouldSuggest = true;
                        break;
                    }
                } else {
                    if (level > 0) {
                        shouldSuggest = true;
                        break;
                    }
                }
            }

            if (shouldSuggest) {
                builder.suggest(perk.getId());
            }
        }

        return builder.buildFuture();
    }

    private int changePerkLevels(Collection<ServerPlayerEntity> targets, String id, int delta) {
        int changed = 0;

        for (ServerPlayerEntity target : targets) {
            int oldLevel = PerkComponents.PERK_COMPONENT.get(target).getPerkLevel(id);
            PerkComponents.PERK_COMPONENT.get(target).changePerkLevel(id, delta);
            int newLevel = PerkComponents.PERK_COMPONENT.get(target).getPerkLevel(id);

            if (newLevel != oldLevel) {
                changed++;
            }
        }

        return changed;
    }

    private int clearPerkForTargets(Collection<ServerPlayerEntity> targets, String id) {
        int changed = 0;

        for (ServerPlayerEntity target : targets) {
            int oldLevel = PerkComponents.PERK_COMPONENT.get(target).getPerkLevel(id);
            PerkComponents.PERK_COMPONENT.get(target).clearPerk(id);

            if (oldLevel > 0) {
                changed++;
            }
        }

        return changed;
    }

    private int clearAllPerksForTargets(Collection<ServerPlayerEntity> targets) {
        int changed = 0;

        for (ServerPlayerEntity target : targets) {
            var comp = PerkComponents.PERK_COMPONENT.get(target);
            var perkIds = new ArrayList<>(comp.getPerks().keySet());

            if (!perkIds.isEmpty() || comp.getTempJumps() > 0 || comp.getAnomalousBondsCharges() > 0) {
                changed++;
            }

            for (String perkId : perkIds) {
                comp.clearPerk(perkId);
            }

            comp.clearTempJumps();
            comp.setAnomalousBondsCharges(0);
        }

        return changed;
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var addCommand = CommandManager.literal("add")
                    .then(CommandManager.argument("target", EntityArgumentType.players())
                            .then(CommandManager.argument("id", StringArgumentType.string())
                                    .suggests(ADDABLE_PERK_SUGGESTIONS)
                                    .then(CommandManager.argument("level", IntegerArgumentType.integer(1))
                                            .executes(ctx -> {
                                                String id = StringArgumentType.getString(ctx, "id");
                                                int delta = IntegerArgumentType.getInteger(ctx, "level");
                                                Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "target");

                                                if (!PerkRegistry.exists(id)) {
                                                    ctx.getSource().sendError(Text.literal("Перк не найден: " + id));
                                                    return 0;
                                                }

                                                int changed = changePerkLevels(targets, id, delta);
                                                ctx.getSource().sendFeedback(() -> Text.literal(
                                                        "Добавлено уровней перка " + id + ": " + delta + ", изменено игроков: " + changed
                                                ), false);
                                                return changed;
                                            }))));

            var removeCommand = CommandManager.literal("remove")
                    .then(CommandManager.argument("target", EntityArgumentType.players())
                            .then(CommandManager.argument("id", StringArgumentType.string())
                                    .suggests(REMOVABLE_PERK_SUGGESTIONS)
                                    .then(CommandManager.argument("level", IntegerArgumentType.integer(1))
                                            .executes(ctx -> {
                                                String id = StringArgumentType.getString(ctx, "id");
                                                int delta = IntegerArgumentType.getInteger(ctx, "level");
                                                Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "target");

                                                if (!PerkRegistry.exists(id)) {
                                                    ctx.getSource().sendError(Text.literal("Перк не найден: " + id));
                                                    return 0;
                                                }

                                                int changed = changePerkLevels(targets, id, -delta);
                                                ctx.getSource().sendFeedback(() -> Text.literal(
                                                        "Убрано уровней перка " + id + ": " + delta + ", изменено игроков: " + changed
                                                ), false);
                                                return changed;
                                            }))));

            var clearCommand = CommandManager.literal("clear")
                    .then(CommandManager.argument("target", EntityArgumentType.players())
                            .then(CommandManager.argument("id", StringArgumentType.string())
                                    .suggests(REMOVABLE_PERK_SUGGESTIONS)
                                    .executes(ctx -> {
                                        String id = StringArgumentType.getString(ctx, "id");
                                        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "target");

                                        if (!PerkRegistry.exists(id)) {
                                            ctx.getSource().sendError(Text.literal("Перк не найден: " + id));
                                            return 0;
                                        }

                                        int changed = clearPerkForTargets(targets, id);
                                        ctx.getSource().sendFeedback(() -> Text.literal(
                                                "Перк сброшен: " + id + ", изменено игроков: " + changed
                                        ), false);
                                        return changed;
                                    })));

            var clearAllCommand = CommandManager.literal("clearall")
                    .then(CommandManager.argument("target", EntityArgumentType.players())
                            .executes(ctx -> {
                                Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "target");
                                int changed = clearAllPerksForTargets(targets);
                                ctx.getSource().sendFeedback(() -> Text.literal(
                                        "Все перки удалены. Изменено игроков: " + changed
                                ), false);
                                return changed;
                            }));

            var listCommand = CommandManager.literal("list")
                    .then(CommandManager.argument("target", EntityArgumentType.player())
                            .executes(ctx -> {
                                ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "target");
                                var comp = PerkComponents.PERK_COMPONENT.get(player);
                                var perks = comp.getPerks();

                                if (perks.isEmpty()) {
                                    ctx.getSource().sendFeedback(() -> Text.literal("У игрока нет перков."), false);
                                } else {
                                    StringBuilder sb = new StringBuilder(player.getName().getString()).append(": ");
                                    perks.forEach((pid, lvl) -> sb.append(pid).append(" (").append(lvl).append("), "));
                                    if (comp.getTempJumps() > 0) {
                                        sb.append("| Temp: ").append(comp.getTempJumps());
                                    }
                                    ctx.getSource().sendFeedback(() -> Text.literal(sb.toString()), false);
                                }

                                return 1;
                            }));

            dispatcher.register(CommandManager.literal("perk")
                    .then(addCommand)
                    .then(removeCommand)
                    .then(clearCommand)
                    .then(clearAllCommand)
                    .then(listCommand)
            );
        });
    }
}

