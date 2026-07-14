package div.wkp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import div.wkp.WKPerks;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WKPerksConfig {
    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("wkperks.json");

    private static WKPerksConfig INSTANCE;

    // ==============================================================
    // Consumptive Reflex
    // ==============================================================

    /**
     * Автоеда включается, когда голод СТРОГО МЕНЬШЕ этого значения.
     * <p>
     * Пример:
     *   14 = есть при 13 и ниже (поведение по умолчанию)
     *    7 = есть при 6 и ниже  (режим "только при голоде")
     */
    public int consumptiveReflexEatBelowFoodLevel = 14;

    /**
     * Если true — перк доедает выше порога до полной сытости (20),
     * то есть с "перерасходом" еды.
     * <p>
     * Если false — перк ест ровно до тех пор, пока голод не станет
     * равен или выше порога, и останавливается.
     */
    public boolean consumptiveReflexOvereat = false;

    // ==============================================================
    // Загрузка / сохранение
    // ==============================================================

    public static WKPerksConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        WKPerksConfig loaded = null;

        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                loaded = GSON.fromJson(json, WKPerksConfig.class);
            } catch (IOException | RuntimeException e) {
                WKPerks.LOGGER.error(
                        "Не удалось прочитать wkperks.json, использую значения по умолчанию.",
                        e
                );
            }
        }

        if (loaded == null) {
            loaded = new WKPerksConfig();
        }

        loaded.sanitize();

        INSTANCE = loaded;

        save();
    }

    public static void save() {
        if (INSTANCE == null) {
            return;
        }

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(
                    CONFIG_PATH,
                    GSON.toJson(INSTANCE)
            );
        } catch (IOException e) {
            WKPerks.LOGGER.error(
                    "Не удалось сохранить wkperks.json",
                    e
            );
        }
    }

    /**
     * Приводит значения в допустимый диапазон,
     * чтобы кривой конфиг не ломал игру.
     */
    private void sanitize() {
        if (consumptiveReflexEatBelowFoodLevel < 1) {
            consumptiveReflexEatBelowFoodLevel = 1;
        }

        if (consumptiveReflexEatBelowFoodLevel > 20) {
            consumptiveReflexEatBelowFoodLevel = 20;
        }
    }
}