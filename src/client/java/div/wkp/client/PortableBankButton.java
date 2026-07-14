package div.wkp.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

public class PortableBankButton extends PressableWidget {
    private final ButtonWidget.PressAction onPress;
    private final ItemStack icon = new ItemStack(Items.ENDER_CHEST);

    public PortableBankButton(int x, int y, ButtonWidget.PressAction onPress) {
        super(x, y, 18, 18, Text.literal("Portable Bank"));
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        onPress.onPress(ButtonWidget.builder(Text.empty(), b -> {
        }).build());
    }

    @Override
    protected void renderWidget(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
        int bg = this.isHovered() ? 0xFF5A4A2A : 0xFF2A2A2A;

        context.fill(getX() - 1, getY() - 1, getX() + 19, getY() + 19, bg);
        context.fill(getX() - 1, getY() - 1, getX() + 19, getY(), 0xFF806020);
        context.fill(getX() - 1, getY() - 1, getX(), getY() + 19, 0xFF806020);

        context.drawItem(icon, getX() + 1, getY() + 1);

        if (this.isHovered()) {
            context.drawTooltip(
                    net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                    Text.literal("Portable Bank"),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }
}
