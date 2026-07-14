package div.wkp.client;

import div.wkp.PerkComponents;
import div.wkp.component.PerkComponent;
import div.wkp.perk.Perk;
import div.wkp.perk.PerkRegistry;
import div.wkp.terminal.TerminalScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class TerminalScreen extends HandledScreen<TerminalScreenHandler> {
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 260;

    private static final int CARD_WIDTH = 120;
    private static final int CARD_HEIGHT = 125;
    private static final int CARD_GAP = 20;

    private static final int COLOR_BACKGROUND = 0xFF2A1A12;
    private static final int COLOR_BORDER = 0xFFC8A040;
    private static final int COLOR_BORDER_DARK = 0xFF6B5220;
    private static final int COLOR_CARD = 0xFF3A2818;
    private static final int COLOR_CARD_SELECTED = 0xFFFF5540;
    private static final int COLOR_TEXT = 0xFFE8C868;
    private static final int COLOR_TEXT_SELECTED = 0xFFFF6050;
    private static final int COLOR_DESCRIPTION = 0xFFD0A968;

    private static final int TAB_BUY = 0;
    private static final int TAB_CURRENT = 1;

    private int selectedOffer = 0;
    private int selectedTab = TAB_BUY;

    private ButtonWidget buyTabButton;
    private ButtonWidget currentTabButton;
    private ButtonWidget purchaseButton;
    private ButtonWidget refreshButton;

    public TerminalScreen(
            TerminalScreenHandler handler,
            PlayerInventory inventory,
            Text title
    ) {
        super(handler, inventory, title);

        this.backgroundWidth = PANEL_WIDTH;
        this.backgroundHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        int panelX = this.x;
        int panelY = this.y;

        ButtonWidget exitButton = this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("X Exit"),
                        button -> this.close()
                ).dimensions(
                        panelX + 10,
                        panelY + 10,
                        65,
                        20
                ).build()
        );

        this.buyTabButton = this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Buy"),
                        button -> {
                            selectedTab = TAB_BUY;
                            selectedOffer = 0;
                        }
                ).dimensions(
                        panelX + 15,
                        panelY + 45,
                        55,
                        20
                ).build()
        );

        this.currentTabButton = this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Current"),
                        button -> {
                            selectedTab = TAB_CURRENT;
                            selectedOffer = 0;
                        }
                ).dimensions(
                        panelX + 15,
                        panelY + 70,
                        55,
                        20
                ).build()
        );

        this.purchaseButton = this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("PURCHASE"),
                        button -> purchaseSelected()
                ).dimensions(
                        panelX + 135,
                        panelY + 220,
                        130,
                        20
                ).build()
        );

        this.refreshButton = this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("REFRESH"),
                        button -> refreshOffers()
                ).dimensions(
                        panelX + 280,
                        panelY + 220,
                        110,
                        20
                ).build()
        );

        updateButtonStates();
    }

    private void updateButtonStates() {
        if (this.purchaseButton == null) {
            return;
        }

        boolean buyTab = selectedTab == TAB_BUY;
        Perk selectedPerk = getSelectedOffer();

        this.buyTabButton.active = !buyTab;
        this.currentTabButton.active = buyTab;

        this.purchaseButton.visible = buyTab;
        this.purchaseButton.active = buyTab && selectedPerk != null;

        this.refreshButton.visible = buyTab;
        this.refreshButton.active = buyTab && handler.getRerollCharges() > 0;
    }

    private Perk getSelectedOffer() {
        if (selectedOffer < 0 || selectedOffer > 1) {
            return null;
        }

        return handler.getOffer(selectedOffer);
    }

    private void purchaseSelected() {
        if (this.client == null || this.client.interactionManager == null) {
            return;
        }

        if (selectedTab != TAB_BUY) {
            return;
        }

        Perk perk = getSelectedOffer();

        if (perk == null) {
            return;
        }

        this.client.interactionManager.clickButton(
                handler.syncId,
                selectedOffer == 0
                        ? TerminalScreenHandler.BUY_OFFER_0
                        : TerminalScreenHandler.BUY_OFFER_1
        );
    }

    private void refreshOffers() {
        if (this.client == null || this.client.interactionManager == null) {
            return;
        }

        if (handler.getRerollCharges() <= 0) {
            return;
        }

        this.client.interactionManager.clickButton(
                handler.syncId,
                TerminalScreenHandler.REROLL_BUTTON
        );
    }

    @Override
    protected void drawBackground(
            DrawContext context,
            float delta,
            int mouseX,
            int mouseY
    ) {
        int panelX = this.x;
        int panelY = this.y;

        drawBorderedBox(
                context,
                panelX,
                panelY,
                PANEL_WIDTH,
                PANEL_HEIGHT,
                COLOR_BACKGROUND
        );

        context.drawText(
                this.textRenderer,
                Text.literal("QuietOS"),
                panelX + 95,
                panelY + 15,
                COLOR_TEXT,
                false
        );

        context.drawText(
                this.textRenderer,
                Text.literal("Perks & Upgrades"),
                panelX + 160,
                panelY + 15,
                COLOR_TEXT,
                false
        );

        context.fill(
                panelX + 80,
                panelY + 38,
                panelX + PANEL_WIDTH - 15,
                panelY + 40,
                COLOR_BORDER_DARK
        );

        if (selectedTab == TAB_BUY) {
            drawBuyTab(context, panelX, panelY);
        } else {
            drawCurrentTab(context, panelX, panelY);
        }

        context.drawText(
                this.textRenderer,
                Text.literal("REROLLS: " + handler.getRerollCharges()),
                panelX + 90,
                panelY + 245,
                COLOR_TEXT,
                false
        );

        context.drawText(
                this.textRenderer,
                Text.literal("COST: FREE"),
                panelX + 280,
                panelY + 245,
                COLOR_TEXT,
                false
        );
    }

    private void drawBuyTab(
            DrawContext context,
            int panelX,
            int panelY
    ) {
        for (int i = 0; i < 2; i++) {
            Perk perk = handler.getOffer(i);

            int cardX = panelX
                    + 88
                    + i * (CARD_WIDTH + CARD_GAP);

            int cardY = panelY + 58;

            drawPerkCard(
                    context,
                    perk,
                    cardX,
                    cardY,
                    i == selectedOffer
            );
        }
    }

    private void drawPerkCard(
            DrawContext context,
            Perk perk,
            int cardX,
            int cardY,
            boolean selected
    ) {
        context.fill(
                cardX,
                cardY,
                cardX + CARD_WIDTH,
                cardY + CARD_HEIGHT,
                COLOR_CARD
        );

        int borderColor = selected
                ? COLOR_CARD_SELECTED
                : COLOR_BORDER_DARK;

        drawRect(
                context,
                cardX,
                cardY,
                CARD_WIDTH,
                CARD_HEIGHT,
                borderColor
        );

        if (selected) {
            drawRect(
                    context,
                    cardX - 2,
                    cardY - 2,
                    CARD_WIDTH + 4,
                    CARD_HEIGHT + 4,
                    COLOR_CARD_SELECTED
            );
        }

        if (perk == null) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("EMPTY"),
                    cardX + CARD_WIDTH / 2,
                    cardY + CARD_HEIGHT / 2,
                    0xFF888888
            );
            return;
        }

        List<net.minecraft.text.OrderedText> nameLines =
                this.textRenderer.wrapLines(
                        perk.getName(),
                        CARD_WIDTH - 10
                );

        int nameY = cardY + 7;
        int nameColor = selected
                ? COLOR_TEXT_SELECTED
                : COLOR_TEXT;

        for (net.minecraft.text.OrderedText line : nameLines) {
            int lineWidth = this.textRenderer.getWidth(line);

            context.drawText(
                    this.textRenderer,
                    line,
                    cardX + (CARD_WIDTH - lineWidth) / 2,
                    nameY,
                    nameColor,
                    false
            );

            nameY += 10;
        }

        context.drawItem(
                perk.getIcon(),
                cardX + CARD_WIDTH / 2 - 16,
                cardY + 48
        );

        int level = 0;

        if (this.client != null && this.client.player != null) {
            PerkComponent component =
                    PerkComponents.PERK_COMPONENT.get(this.client.player);

            level = component.getPerkLevel(perk.getId());
        }

        Text levelText = Text.literal(
                "LV " + level + " / " + perk.getMaxLevel()
        );

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                levelText,
                cardX + CARD_WIDTH / 2,
                cardY + CARD_HEIGHT - 18,
                COLOR_DESCRIPTION
        );
    }

    private void drawCurrentTab(
            DrawContext context,
            int panelX,
            int panelY
    ) {
        if (this.client == null || this.client.player == null) {
            return;
        }

        PerkComponent component =
                PerkComponents.PERK_COMPONENT.get(this.client.player);

        List<Perk> ownedPerks = new ArrayList<>();

        for (String id : component.getPerks().keySet()) {
            Perk perk = PerkRegistry.get(id);

            if (perk != null && component.getPerkLevel(id) > 0) {
                ownedPerks.add(perk);
            }
        }

        if (ownedPerks.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("No perks installed."),
                    panelX + PANEL_WIDTH / 2,
                    panelY + 125,
                    0xFF999999
            );
            return;
        }

        int index = 0;

        for (Perk perk : ownedPerks) {
            int column = index % 2;
            int row = index / 2;

            int cardX = panelX + 90 + column * 140;
            int cardY = panelY + 55 + row * 48;

            int level = component.getPerkLevel(perk.getId());

            context.fill(
                    cardX,
                    cardY,
                    cardX + 120,
                    cardY + 38,
                    COLOR_CARD
            );

            drawRect(
                    context,
                    cardX,
                    cardY,
                    120,
                    38,
                    COLOR_BORDER_DARK
            );

            context.drawItem(
                    perk.getIcon(),
                    cardX + 5,
                    cardY + 5
            );

            context.drawText(
                    this.textRenderer,
                    perk.getName(),
                    cardX + 28,
                    cardY + 7,
                    COLOR_TEXT,
                    false
            );

            context.drawText(
                    this.textRenderer,
                    Text.literal(
                            "Lv. " + level + " / " + perk.getMaxLevel()
                    ),
                    cardX + 28,
                    cardY + 21,
                    COLOR_DESCRIPTION,
                    false
            );

            index++;
        }
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
        updateButtonStates();

        super.render(context, mouseX, mouseY, delta);

        if (selectedTab == TAB_BUY) {
            int hoveredOffer = getHoveredOffer(mouseX, mouseY);

            if (hoveredOffer >= 0) {
                Perk perk = handler.getOffer(hoveredOffer);

                if (perk != null) {
                    List<Text> tooltip = new ArrayList<>();

                    tooltip.add(perk.getName());
                    tooltip.add(perk.getDescription());

                    if (this.client != null && this.client.player != null) {
                        PerkComponent component =
                                PerkComponents.PERK_COMPONENT.get(
                                        this.client.player
                                );

                        tooltip.addAll(
                                perk.getExtraTooltip(
                                        component,
                                        component.getPerkLevel(perk.getId())
                                )
                        );
                    }

                    context.drawTooltip(
                            this.textRenderer,
                            tooltip,
                            mouseX,
                            mouseY
                    );
                }
            }
        }
    }

    private int getHoveredOffer(int mouseX, int mouseY) {
        int panelX = this.x;
        int panelY = this.y;

        for (int i = 0; i < 2; i++) {
            int cardX = panelX
                    + 88
                    + i * (CARD_WIDTH + CARD_GAP);

            int cardY = panelY + 58;

            if (mouseX >= cardX
                    && mouseX < cardX + CARD_WIDTH
                    && mouseY >= cardY
                    && mouseY < cardY + CARD_HEIGHT) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (selectedTab != TAB_BUY || button != 0) {
            return false;
        }

        int hoveredOffer = getHoveredOffer(
                (int) mouseX,
                (int) mouseY
        );

        if (hoveredOffer >= 0) {
            selectedOffer = hoveredOffer;
            return true;
        }

        return false;
    }

    private void drawRect(
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

    private void drawBorderedBox(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int fillColor
    ) {
        context.fill(
                x,
                y,
                x + width,
                y + height,
                fillColor
        );

        drawRect(
                context,
                x,
                y,
                width,
                height,
                COLOR_BORDER
        );

        drawRect(
                context,
                x + 3,
                y + 3,
                width - 6,
                height - 6,
                COLOR_BORDER_DARK
        );
    }
}