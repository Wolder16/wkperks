package div.wkp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import div.wkp.WKPerks;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WKPerksServerConfig {
    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("wkperks-server.json");

    private static WKPerksServerConfig INSTANCE;

    public SpearRecallMode spearRecallMode = SpearRecallMode.MARKER_CLICK;

    public enum SpearRecallMode {
        SIMPLE_RMB,
        MARKER_CLICK
    }

    public static WKPerksServerConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        WKPerksServerConfig loaded = null;

        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                loaded = GSON.fromJson(json, WKPerksServerConfig.class);
            } catch (IOException | RuntimeException e) {
                WKPerks.LOGGER.error("Не удалось прочитать wkperks-server.json, использую значения по умолчанию.", e);
            }
        }

        if (loaded == null) {
            loaded = new WKPerksServerConfig();
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
            Files.writeString(CONFIG_PATH, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            WKPerks.LOGGER.error("Не удалось сохранить wkperks-server.json", e);
        }
    }

    private void sanitize() {
        if (spearRecallMode == null) {
            spearRecallMode = SpearRecallMode.MARKER_CLICK;
        }
    }
}
