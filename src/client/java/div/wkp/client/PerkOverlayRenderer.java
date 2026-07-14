package div.wkp.client;

import div.wkp.PerkComponents;
import div.wkp.client.mixin.HandledScreenAccessor;
import div.wkp.component.PerkComponent;
import div.wkp.perk.Perk;
import div.wkp.perk.PerkRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class PerkOverlayRenderer {
    private static final int PANEL_HEIGHT = 22;
    private static final int PANEL_PADDING = 4;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_GAP = 2;
    private static final int PANEL_MARGIN = 4;

    private static final int COLOR_PANEL = 0xEE2A1A12;
    private static final int COLOR_PANEL_BORDER = 0xFFC8A040;
    private static final int COLOR_PANEL_BORDER_DARK = 0xFF6B5220;
    private static final int COLOR_SLOT = 0xFF3A2818;
    private static final int COLOR_SLOT_BORDER = 0xFF6B5220;
    private static final int COLOR_SLOT_HOVER = 0xFFFF6050;
    private static final int COLOR_LEVEL = 0xFFE8C868;

    private PerkOverlayRenderer() {
    }

    public static void render(
            InventoryScreen screen,
            DrawContext context,
            int mouseX,
            int mouseY
    ) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) {
            return;
        }

        PerkComponent component =
                PerkComponents.PERK_COMPONENT.get(client.player);

        List<Perk> ownedPerks = new ArrayList<>();

        for (Perk perk : PerkRegistry.getAll()) {
            if (component.getPerkLevel(perk.getId()) > 0) {
                ownedPerks.add(perk);
            }
        }

        if (ownedPerks.isEmpty()) {
            return;
        }

        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        int inventoryX = accessor.wkperks$getX();
        int inventoryY = accessor.wkperks$getY();
        int inventoryWidth = accessor.wkperks$getBackgroundWidth();

        int contentWidth =
                ownedPerks.size() * SLOT_SIZE
                        + Math.max(0, ownedPerks.size() - 1) * SLOT_GAP;

        int panelWidth = Math.max(
                inventoryWidth,
                contentWidth + PANEL_PADDING * 2
        );

        int panelX = inventoryX + (inventoryWidth - panelWidth) / 2;
        int panelY = inventoryY - PANEL_HEIGHT - PANEL_MARGIN;
        int slotStartX = panelX + (panelWidth - contentWidth) / 2;
        int slotY = panelY + 2;

        drawPanel(context, panelX, panelY, panelWidth, PANEL_HEIGHT);

        Perk hoveredPerk = null;

        for (int i = 0; i < ownedPerks.size(); i++) {
            Perk perk = ownedPerks.get(i);
            int slotX = slotStartX + i * (SLOT_SIZE + SLOT_GAP);
            int level = component.getPerkLevel(perk.getId());

            boolean hovered = mouseX >= slotX
                    && mouseX < slotX + SLOT_SIZE
                    && mouseY >= slotY
                    && mouseY < slotY + SLOT_SIZE;

            drawSlot(context, slotX, slotY, hovered);
            context.drawItem(perk.getIcon(), slotX + 1, slotY + 1);

            if (perk.getMaxLevel() > 1) {
                String levelText = String.valueOf(level);
                context.drawText(
                        client.textRenderer,
                        levelText,
                        slotX + 10,
                        slotY + 10,
                        COLOR_LEVEL,
                        true
                );
            }

            if (hovered) {
                hoveredPerk = perk;
            }
        }

        if (hoveredPerk == null) {
            return;
        }

        List<Text> tooltip = new ArrayList<>();
        tooltip.add(hoveredPerk.getName());
        tooltip.add(hoveredPerk.getDescription());

        int hoveredLevel = component.getPerkLevel(hoveredPerk.getId());

        if (hoveredPerk.getMaxLevel() > 1) {
            tooltip.add(Text.literal(
                    "LV " + hoveredLevel + " / " + hoveredPerk.getMaxLevel()
            ));
        }

        tooltip.addAll(
                hoveredPerk.getExtraTooltip(client.player, component, hoveredLevel)
        );

        context.drawTooltip(
                client.textRenderer,
                tooltip,
                mouseX,
                mouseY
        );
    }

    private static void drawPanel(
            DrawContext context,
            int x,
            int y,
            int width,
            int height
    ) {
        context.fill(x, y, x + width, y + height, COLOR_PANEL);
        drawRect(context, x, y, width, height, COLOR_PANEL_BORDER);
        drawRect(context, x + 2, y + 2, width - 4, height - 4, COLOR_PANEL_BORDER_DARK);
    }

    private static void drawSlot(
            DrawContext context,
            int x,
            int y,
            boolean hovered
    ) {
        context.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, COLOR_SLOT);
        drawRect(
                context,
                x,
                y,
                SLOT_SIZE,
                SLOT_SIZE,
                hovered ? COLOR_SLOT_HOVER : COLOR_SLOT_BORDER
        );
    }

    private static void drawRect(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }
}
