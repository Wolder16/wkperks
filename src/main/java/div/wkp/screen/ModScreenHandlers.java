package div.wkp.screen;

import div.wkp.WKPerks;
import div.wkp.terminal.TerminalScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.resource.featuretoggle.FeatureFlags;

public final class ModScreenHandlers {
    private ModScreenHandlers() {
    }

    public static final ScreenHandlerType<TerminalScreenHandler> TERMINAL =
            Registry.register(
                    Registries.SCREEN_HANDLER,
                    Identifier.of(WKPerks.MOD_ID, "terminal"),
                    new ScreenHandlerType<>(
                            TerminalScreenHandler::new,
                            FeatureFlags.VANILLA_FEATURES
                    )
            );

    public static void initialize() {
        WKPerks.LOGGER.info("Registering WKPerks screen handlers");
    }
}