package div.wkp.client.config;

import div.wkp.config.WKPerksConfig;
import div.wkp.config.WKPerksServerConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Objects;

public class WKPerksConfigScreen extends Screen {
    private final Screen parent;

    private TextFieldWidget consumptiveThresholdField;
    private ButtonWidget overeatingButton;
    private ButtonWidget recallModeButton;
    private ButtonWidget saveButton;

    private boolean consumptiveOvereat;
    private WKPerksServerConfig.SpearRecallMode spearRecallMode;
    private String validationMessage = "";

    public WKPerksConfigScreen(Screen parent) {
        super(Text.literal("WKPerks Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        WKPerksConfig clientConfig = WKPerksConfig.get();
        WKPerksServerConfig serverConfig = WKPerksServerConfig.get();

        this.consumptiveOvereat = clientConfig.consumptiveReflexOvereat;
        this.spearRecallMode = serverConfig.spearRecallMode;

        int centerX = this.width / 2;
        int startY = this.height / 2 - 70;

        this.consumptiveThresholdField = new TextFieldWidget(
                this.textRenderer,
                centerX - 90,
                startY + 18,
                180,
                20,
                Text.literal("Eat threshold")
        );
        this.consumptiveThresholdField.setText(String.valueOf(clientConfig.consumptiveReflexEatBelowFoodLevel));
        this.addDrawableChild(this.consumptiveThresholdField);

        this.overeatingButton = this.addDrawableChild(ButtonWidget.builder(
                getOvereatButtonText(),
                button -> {
                    consumptiveOvereat = !consumptiveOvereat;
                    button.setMessage(getOvereatButtonText());
                }
        ).dimensions(centerX - 90, startY + 58, 180, 20).build());

        this.recallModeButton = this.addDrawableChild(ButtonWidget.builder(
                getRecallModeButtonText(),
                button -> {
                    spearRecallMode = spearRecallMode == WKPerksServerConfig.SpearRecallMode.MARKER_CLICK
                            ? WKPerksServerConfig.SpearRecallMode.SIMPLE_RMB
                            : WKPerksServerConfig.SpearRecallMode.MARKER_CLICK;
                    button.setMessage(getRecallModeButtonText());
                }
        ).dimensions(centerX - 90, startY + 118, 180, 20).build());

        this.saveButton = this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Save").formatted(Formatting.GREEN),
                button -> saveAndClose()
        ).dimensions(centerX - 90, startY + 170, 85, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Cancel").formatted(Formatting.RED),
                button -> close()
        ).dimensions(centerX + 5, startY + 170, 85, 20).build());

        this.setInitialFocus(this.consumptiveThresholdField);
    }

    private Text getOvereatButtonText() {
        return Text.literal("Overeat: " + (consumptiveOvereat ? "ON" : "OFF"));
    }

    private Text getRecallModeButtonText() {
        String label = spearRecallMode == WKPerksServerConfig.SpearRecallMode.MARKER_CLICK
                ? "MARKER_CLICK"
                : "SIMPLE_RMB";
        return Text.literal("Spear Recall: " + label);
    }

    private void saveAndClose() {
        int threshold;

        try {
            threshold = Integer.parseInt(this.consumptiveThresholdField.getText().trim());
        } catch (NumberFormatException e) {
            this.validationMessage = "Порог должен быть числом от 1 до 20.";
            return;
        }

        if (threshold < 1 || threshold > 20) {
            this.validationMessage = "Порог должен быть в диапазоне 1..20.";
            return;
        }

        WKPerksConfig clientConfig = WKPerksConfig.get();
        clientConfig.consumptiveReflexEatBelowFoodLevel = threshold;
        clientConfig.consumptiveReflexOvereat = consumptiveOvereat;
        WKPerksConfig.save();

        WKPerksServerConfig serverConfig = WKPerksServerConfig.get();
        serverConfig.spearRecallMode = spearRecallMode;
        WKPerksServerConfig.save();

        this.close();
    }

    @Override
    public void close() {
        Objects.requireNonNull(this.client).setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 70;

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                centerX,
                startY - 18,
                0xFFE8C868
        );

        context.drawText(
                this.textRenderer,
                Text.literal("Client config").formatted(Formatting.GOLD),
                centerX - 90,
                startY,
                0xFFFFFFFF,
                false
        );

        context.drawText(
                this.textRenderer,
                Text.literal("Consumptive Reflex food threshold"),
                centerX - 90,
                startY + 6,
                0xFFCCCCCC,
                false
        );

        context.drawText(
                this.textRenderer,
                Text.literal("Server config").formatted(Formatting.GOLD),
                centerX - 90,
                startY + 96,
                0xFFFFFFFF,
                false
        );

        context.drawText(
                this.textRenderer,
                Text.literal("Spear recall mode (local/integrated use)"),
                centerX - 90,
                startY + 106,
                0xFFCCCCCC,
                false
        );

        if (!validationMessage.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal(validationMessage).formatted(Formatting.RED),
                    centerX,
                    startY + 146,
                    0xFFFF5555
            );
        }

        this.consumptiveThresholdField.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }
}