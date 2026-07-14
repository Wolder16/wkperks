package div.wkp;


import div.wkp.block.ModBlocks;
import div.wkp.block.ModBlockEntities;
import div.wkp.item.ModItems;
import div.wkp.altar.AltarRegistry;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import div.wkp.screen.ModScreenHandlers;
import div.wkp.network.DoubleJumpPayload;
import div.wkp.perk.Perk;
import div.wkp.perk.PerkRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

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
        ModItems.initialize();
        ModBlocks.initialize();
        ModScreenHandlers.initialize();
        ModBlockEntities.initialize();
        PerkRegistry.init();
        AltarRegistry.init();
        PayloadTypeRegistry.playC2S().register(DoubleJumpPayload.ID, DoubleJumpPayload.CODEC);

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
                    player.setVelocity(horizontal.x * 0.3, 0.5, horizontal.z * 0.3);
                    player.velocityModified = true;
                    player.fallDistance = 0.0F;
                    FALL_DAMAGE_IMMUNITY.add(player.getUuid());
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
            }

        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            LAST_GAMEMODE.remove(handler.player.getUuid());
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