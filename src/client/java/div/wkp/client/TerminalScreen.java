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
    private static final int COLOR_PANEL_DARK = 0xFF1A100B;
    private static final int COLOR_BORDER = 0xFFC8A040;
    private static final int COLOR_BORDER_DARK = 0xFF6B5220;

    private static final int COLOR_CARD = 0xFF3A2818;
    private static final int COLOR_SELECTED = 0xFFFF5540;

    private static final int COLOR_TEXT = 0xFFE8C868;
    private static final int COLOR_TEXT_SELECTED = 0xFFFF6050;
    private static final int COLOR_DESCRIPTION = 0xFFD0A968;

    private static final int COLOR_PURCHASED_BG = 0xFF17351B;
    private static final int COLOR_PURCHASED_BORDER = 0xFF55AA55;
    private static final int COLOR_PURCHASED_TEXT = 0xFF7DFF7D;

    private static final int COLOR_LOCKED_BG = 0xFF171411;
    private static final int COLOR_LOCKED_BORDER = 0xFF4A4540;
    private static final int COLOR_LOCKED_TEXT = 0xFF77716B;

    private static final int TAB_BUY = 0;
    private static final int TAB_CURRENT = 1;

    private int selectedTab = TAB_BUY;
    private int selectedOffer = 0;

    private ButtonWidget buyTabButton;
    private ButtonWidget currentTabButton;
    private ButtonWidget purchaseButton;
    private ButtonWidget refreshButton;

    private enum OfferVisualState {
        AVAILABLE,
        PURCHASED,
        LOCKED
    }

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

        this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("⏻ Exit"),
                        button -> this.close()
                ).dimensions(
                        panelX + 10,
                        panelY + 10,
                        70,
                        20
                ).build()
        );

        this.buyTabButton = this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Buy"),
                        button -> {
                            selectedTab = TAB_BUY;
                            normalizeSelectedOffer();
                            updateButtonStates();
                        }
                ).dimensions(
                        panelX + 15,
                        panelY + 55,
                        60,
                        20
                ).build()
        );

        this.currentTabButton = this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("Current"),
                        button -> {
                            selectedTab = TAB_CURRENT;
                            updateButtonStates();
                        }
                ).dimensions(
                        panelX + 15,
                        panelY + 80,
                        60,
                        20
                ).build()
        );

        this.purchaseButton = this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("PURCHASE SELECTED"),
                        button -> purchaseSelected()
                ).dimensions(
                        panelX + 140,
                        panelY + 205,
                        145,
                        20
                ).build()
        );

        this.refreshButton = this.addDrawableChild(
                ButtonWidget.builder(
                        Text.literal("REFRESH"),
                        button -> refreshOffers()
                ).dimensions(
                        panelX + 300,
                        panelY + 205,
                        90,
                        20
                ).build()
        );

        normalizeSelectedOffer();
        updateButtonStates();
    }

    private void normalizeSelectedOffer() {
        int purchasedSlot = handler.getPurchasedSlot();

        if (purchasedSlot == 0 || purchasedSlot == 1) {
            selectedOffer = purchasedSlot;
            return;
        }

        if (selectedOffer >= 0
                && selectedOffer <= 1
                && handler.getOffer(selectedOffer) != null) {
            return;
        }

        if (handler.getOffer(0) != null) {
            selectedOffer = 0;
        } else if (handler.getOffer(1) != null) {
            selectedOffer = 1;
        } else {
            selectedOffer = 0;
        }
    }

    private void updateButtonStates() {
        if (buyTabButton == null
                || currentTabButton == null
                || purchaseButton == null
                || refreshButton == null) {
            return;
        }

        normalizeSelectedOffer();

        boolean buyTab = selectedTab == TAB_BUY;
        boolean purchased = handler.getPurchasedSlot() != -1;

        buyTabButton.active = !buyTab;
        currentTabButton.active = buyTab;

        purchaseButton.visible = buyTab;
        purchaseButton.active =
                buyTab
                        && !purchased
                        && getSelectedOffer() != null;

        purchaseButton.setMessage(
                purchased
                        ? Text.literal("PURCHASED")
                        : Text.literal("PURCHASE SELECTED")
        );

        refreshButton.visible = buyTab;
        refreshButton.active =
                buyTab
                        && handler.getRerollCharges() > 0;
    }

    private Perk getSelectedOffer() {
        if (selectedOffer < 0 || selectedOffer > 1) {
            return null;
        }

        return handler.getOffer(selectedOffer);
    }

    private void purchaseSelected() {
        if (client == null || client.interactionManager == null) {
            return;
        }

        if (selectedTab != TAB_BUY) {
            return;
        }

        if (handler.getPurchasedSlot() != -1) {
            return;
        }

        if (getSelectedOffer() == null) {
            return;
        }

        int buttonId = selectedOffer == 0
                ? TerminalScreenHandler.BUY_OFFER_0
                : TerminalScreenHandler.BUY_OFFER_1;

        client.interactionManager.clickButton(
                handler.syncId,
                buttonId
        );
    }

    private void refreshOffers() {
        if (client == null || client.interactionManager == null) {
            return;
        }

        if (selectedTab != TAB_BUY) {
            return;
        }

        if (handler.getRerollCharges() <= 0) {
            return;
        }

        client.interactionManager.clickButton(
                handler.syncId,
                TerminalScreenHandler.REROLL_BUTTON
        );

        selectedOffer = 0;
        updateButtonStates();
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
                textRenderer,
                Text.literal("✹  " + handler.getRerollCharges()),
                panelX + 150,
                panelY + 15,
                COLOR_TEXT,
                false
        );

        context.drawText(
                textRenderer,
                Text.literal("QuietOS ♜"),
                panelX + 305,
                panelY + 15,
                COLOR_TEXT,
                false
        );

        context.fill(
                panelX + 10,
                panelY + 36,
                panelX + PANEL_WIDTH - 10,
                panelY + 38,
                COLOR_BORDER_DARK
        );

        context.fill(
                panelX + 85,
                panelY + 48,
                panelX + PANEL_WIDTH - 25,
                panelY + 235,
                COLOR_PANEL_DARK
        );

        drawRect(
                context,
                panelX + 85,
                panelY + 48,
                PANEL_WIDTH - 110,
                187,
                COLOR_BORDER_DARK
        );

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Perks & Upgrades"),
                panelX + 240,
                panelY + 55,
                COLOR_TEXT
        );

        if (selectedTab == TAB_BUY) {
            drawBuyTab(context, panelX, panelY);
        } else {
            drawCurrentTab(context, panelX, panelY);
        }
    }

    @Override
    protected void drawForeground(
            DrawContext context,
            int mouseX,
            int mouseY
    ) {
        // Отключаем стандартные подписи HandledScreen.
    }

    private void drawBuyTab(
            DrawContext context,
            int panelX,
            int panelY
    ) {
        int purchasedSlot = handler.getPurchasedSlot();

        for (int i = 0; i < 2; i++) {
            Perk perk = handler.getOffer(i);

            int cardX = panelX + 120 + i * (CARD_WIDTH + CARD_GAP);
            int cardY = panelY + 78;

            OfferVisualState state;

            if (purchasedSlot == -1) {
                state = OfferVisualState.AVAILABLE;
            } else if (purchasedSlot == i) {
                state = OfferVisualState.PURCHASED;
            } else {
                state = OfferVisualState.LOCKED;
            }

            drawPerkCard(
                    context,
                    perk,
                    cardX,
                    cardY,
                    i == selectedOffer,
                    state
            );
        }
    }

    private void drawPerkCard(
            DrawContext context,
            Perk perk,
            int cardX,
            int cardY,
            boolean selected,
            OfferVisualState state
    ) {
        int background = switch (state) {
            case AVAILABLE -> COLOR_CARD;
            case PURCHASED -> COLOR_PURCHASED_BG;
            case LOCKED -> COLOR_LOCKED_BG;
        };

        int border = switch (state) {
            case AVAILABLE -> selected ? COLOR_SELECTED : COLOR_BORDER_DARK;
            case PURCHASED -> COLOR_PURCHASED_BORDER;
            case LOCKED -> COLOR_LOCKED_BORDER;
        };

        context.fill(
                cardX,
                cardY,
                cardX + CARD_WIDTH,
                cardY + CARD_HEIGHT,
                background
        );

        drawRect(
                context,
                cardX,
                cardY,
                CARD_WIDTH,
                CARD_HEIGHT,
                border
        );

        if (selected && state == OfferVisualState.AVAILABLE) {
            drawRect(
                    context,
                    cardX - 2,
                    cardY - 2,
                    CARD_WIDTH + 4,
                    CARD_HEIGHT + 4,
                    COLOR_SELECTED
            );
        }

        if (state == OfferVisualState.PURCHASED) {
            drawRect(
                    context,
                    cardX - 2,
                    cardY - 2,
                    CARD_WIDTH + 4,
                    CARD_HEIGHT + 4,
                    COLOR_PURCHASED_BORDER
            );
        }

        if (perk == null) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal("EMPTY"),
                    cardX + CARD_WIDTH / 2,
                    cardY + CARD_HEIGHT / 2,
                    COLOR_LOCKED_TEXT
            );

            return;
        }

        int textColor = switch (state) {
            case AVAILABLE -> selected ? COLOR_TEXT_SELECTED : COLOR_TEXT;
            case PURCHASED -> COLOR_PURCHASED_TEXT;
            case LOCKED -> COLOR_LOCKED_TEXT;
        };

        List<net.minecraft.text.OrderedText> lines =
                textRenderer.wrapLines(
                        perk.getName(),
                        CARD_WIDTH - 10
                );

        int textY = cardY + 8;

        for (net.minecraft.text.OrderedText line : lines) {
            int lineWidth = textRenderer.getWidth(line);

            context.drawText(
                    textRenderer,
                    line,
                    cardX + (CARD_WIDTH - lineWidth) / 2,
                    textY,
                    textColor,
                    false
            );

            textY += 10;
        }

        int iconX = cardX + CARD_WIDTH / 2 - 8;
        int iconY = cardY + 50;

        context.drawItem(
                perk.getIcon(),
                iconX,
                iconY
        );

        if (state == OfferVisualState.LOCKED) {
            context.fill(
                    iconX - 2,
                    iconY - 2,
                    iconX + 18,
                    iconY + 18,
                    0x99000000
            );
        }

        Text bottomText;
        int bottomColor;

        if (state == OfferVisualState.PURCHASED) {
            bottomText = Text.literal("PURCHASED");
            bottomColor = COLOR_PURCHASED_TEXT;
        } else if (state == OfferVisualState.LOCKED) {
            bottomText = Text.literal("LOCKED");
            bottomColor = COLOR_LOCKED_TEXT;
        } else {
            int level = getPlayerPerkLevel(perk);

            bottomText = Text.literal(
                    "LV " + level + " / " + perk.getMaxLevel()
            );

            bottomColor = COLOR_DESCRIPTION;
        }

        context.drawCenteredTextWithShadow(
                textRenderer,
                bottomText,
                cardX + CARD_WIDTH / 2,
                cardY + CARD_HEIGHT - 18,
                bottomColor
        );
    }

    private int getPlayerPerkLevel(Perk perk) {
        if (client == null || client.player == null) {
            return 0;
        }

        PerkComponent component =
                PerkComponents.PERK_COMPONENT.get(client.player);

        return component.getPerkLevel(perk.getId());
    }

    private void drawCurrentTab(
            DrawContext context,
            int panelX,
            int panelY
    ) {
        if (client == null || client.player == null) {
            return;
        }

        PerkComponent component =
                PerkComponents.PERK_COMPONENT.get(client.player);

        List<Perk> owned = new ArrayList<>();

        for (Perk perk : PerkRegistry.getAll()) {
            if (component.getPerkLevel(perk.getId()) > 0) {
                owned.add(perk);
            }
        }

        if (owned.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal("No perks installed."),
                    panelX + 240,
                    panelY + 135,
                    0xFF999999
            );

            return;
        }

        for (int i = 0; i < owned.size(); i++) {
            Perk perk = owned.get(i);

            int column = i % 2;
            int row = i / 2;

            int cardX = panelX + 105 + column * 145;
            int cardY = panelY + 78 + row * 42;

            int level = component.getPerkLevel(perk.getId());

            context.fill(
                    cardX,
                    cardY,
                    cardX + 135,
                    cardY + 34,
                    COLOR_CARD
            );

            drawRect(
                    context,
                    cardX,
                    cardY,
                    135,
                    34,
                    COLOR_BORDER_DARK
            );

            context.drawItem(
                    perk.getIcon(),
                    cardX + 6,
                    cardY + 8
            );

            context.drawText(
                    textRenderer,
                    perk.getName(),
                    cardX + 28,
                    cardY + 6,
                    COLOR_TEXT,
                    false
            );

            context.drawText(
                    textRenderer,
                    Text.literal("Lv. " + level + " / " + perk.getMaxLevel()),
                    cardX + 28,
                    cardY + 19,
                    COLOR_DESCRIPTION,
                    false
            );
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

        super.render(
                context,
                mouseX,
                mouseY,
                delta
        );

        if (selectedTab != TAB_BUY) {
            return;
        }

        int hoveredOffer = getHoveredOffer(mouseX, mouseY);

        if (hoveredOffer < 0) {
            return;
        }

        Perk perk = handler.getOffer(hoveredOffer);

        if (perk == null) {
            return;
        }

        List<Text> tooltip = new ArrayList<>();

        tooltip.add(perk.getName());
        tooltip.add(perk.getDescription());

        int purchasedSlot = handler.getPurchasedSlot();

        if (purchasedSlot == hoveredOffer) {
            tooltip.add(Text.literal("PURCHASED"));
        } else if (purchasedSlot != -1) {
            tooltip.add(Text.literal("LOCKED"));
        }

        if (client != null && client.player != null) {
            PerkComponent component =
                    PerkComponents.PERK_COMPONENT.get(client.player);

            tooltip.addAll(
                    perk.getExtraTooltip(
                            client.player,
                            component,
                            component.getPerkLevel(perk.getId())
                    )
            );
        }

        context.drawTooltip(
                textRenderer,
                tooltip,
                mouseX,
                mouseY
        );
    }

    private int getHoveredOffer(int mouseX, int mouseY) {
        int panelX = this.x;
        int panelY = this.y;

        for (int i = 0; i < 2; i++) {
            int cardX = panelX + 120 + i * (CARD_WIDTH + CARD_GAP);
            int cardY = panelY + 78;

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
        /*
         * ВАЖНО:
         * Карточки обрабатываем ДО super.mouseClicked.
         * Иначе HandledScreen может съесть клик раньше нас.
         */
        if (selectedTab == TAB_BUY && button == 0) {
            int hoveredOffer = getHoveredOffer(
                    (int) mouseX,
                    (int) mouseY
            );

            if (hoveredOffer >= 0
                    && handler.getPurchasedSlot() == -1
                    && handler.getOffer(hoveredOffer) != null) {
                selectedOffer = hoveredOffer;
                updateButtonStates();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
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
            int fill
    ) {
        context.fill(
                x,
                y,
                x + width,
                y + height,
                fill
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