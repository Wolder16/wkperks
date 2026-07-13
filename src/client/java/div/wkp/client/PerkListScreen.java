package div.wkp.client;

import div.wkp.PerkComponents;
import div.wkp.component.PerkComponent;
import div.wkp.perk.Perk;
import div.wkp.perk.PerkRegistry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PerkListScreen extends Screen {
    private final Screen parent;

    // === Размеры терминала ===
    private static final int PANEL_W = 420;
    private static final int PANEL_H = 260;

    // Карточки перков
    private static final int CARD_W = 76;
    private static final int CARD_H = 110;
    private static final int CARD_GAP = 12;

    // === Цвета в стиле референса ===
    private static final int COL_BG        = 0xFF2A1A12; // тёмно-коричневый фон панели
    private static final int COL_BORDER    = 0xFFC8A040; // золотая рамка
    private static final int COL_BORDER_D  = 0xFF6B5220; // тёмное золото
    private static final int COL_CARD      = 0xFF3A2818; // фон карточки
    private static final int COL_CARD_SEL  = 0xFFFF5540; // красная рамка выбранного
    private static final int COL_TEXT      = 0xFFE8C868; // золотой текст
    private static final int COL_TEXT_SEL  = 0xFFFF6050; // красный текст

    private int selectedIndex = 0;
    private final List<Perk> ownedPerks = new ArrayList<>();

    public PerkListScreen(Screen parent) {
        super(Text.literal("Perks & Upgrades"));
        this.parent = parent;
    }

    private int panelX() { return (this.width - PANEL_W) / 2; }
    private int panelY() { return (this.height - PANEL_H) / 2; }

    @Override
    protected void init() {
        // Собираем список перков игрока
        ownedPerks.clear();
        PerkComponent comp = PerkComponents.PERK_COMPONENT.get(this.client.player);
        for (String id : comp.getPerks().keySet()) {
            Perk perk = PerkRegistry.get(id);
            if (perk != null) ownedPerks.add(perk);
        }

        // Кнопка "Exit" внизу
        int px = panelX();
        int py = panelY();
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("✕ Exit").formatted(Formatting.GOLD),
                b -> this.client.setScreen(parent)
        ).dimensions(px + 20, py + PANEL_H - 30, 70, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Затемнение всего экрана
        context.fill(0, 0, this.width, this.height, 0xC0000000);

        int px = panelX();
        int py = panelY();

        // === Основная панель ===
        drawBorderedBox(context, px, py, PANEL_W, PANEL_H, COL_BG);

        // === Заголовок ===
        context.drawText(this.textRenderer,
                Text.literal("✕").formatted(Formatting.GOLD),
                px + 16, py + 14, COL_TEXT, false);
        context.drawCenteredTextWithShadow(this.textRenderer,
                this.title, px + PANEL_W / 2, py + 14, COL_TEXT);

        // Линия под заголовком
        context.fill(px + 12, py + 30, px + PANEL_W - 12, py + 31, COL_BORDER_D);

        PerkComponent comp = PerkComponents.PERK_COMPONENT.get(this.client.player);

        if (ownedPerks.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("No perks installed.").formatted(Formatting.DARK_GRAY),
                    px + PANEL_W / 2, py + PANEL_H / 2, 0xFF888888);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        // === Ряд карточек (центрирован) ===
        int totalCards = ownedPerks.size();
        int rowWidth = totalCards * CARD_W + (totalCards - 1) * CARD_GAP;
        int startX = px + (PANEL_W - rowWidth) / 2;
        int cardY = py + 45;

        Perk hovered = null;

        for (int i = 0; i < totalCards; i++) {
            Perk perk = ownedPerks.get(i);
            int cardX = startX + i * (CARD_W + CARD_GAP);

            boolean isSelected = (i == selectedIndex);
            boolean isHovered = mouseX >= cardX && mouseX < cardX + CARD_W
                    && mouseY >= cardY && mouseY < cardY + CARD_H;

            if (isHovered) hovered = perk;

            // Фон карточки
            context.fill(cardX, cardY, cardX + CARD_W, cardY + CARD_H, COL_CARD);

            // Рамка карточки
            int borderColor = isSelected ? COL_CARD_SEL : COL_BORDER_D;
            drawRect(context, cardX, cardY, CARD_W, CARD_H, borderColor);
            if (isSelected) {
                // двойная рамка для выделения
                drawRect(context, cardX - 2, cardY - 2, CARD_W + 4, CARD_H + 4, COL_CARD_SEL);
            }

            // Название перка (сверху карточки, в 2 строки если надо)
            int nameColor = isSelected ? COL_TEXT_SEL : COL_TEXT;
            List<net.minecraft.text.OrderedText> wrapped =
                    this.textRenderer.wrapLines(perk.getName(), CARD_W - 8);
            int ty = cardY + 6;
            for (var line : wrapped) {
                int lineW = this.textRenderer.getWidth(line);
                context.drawText(this.textRenderer, line,
                        cardX + (CARD_W - lineW) / 2, ty, nameColor, false);
                ty += 10;
            }

            // Иконка перка (крупно по центру карточки)
            int iconX = cardX + (CARD_W - 16) / 2;
            int iconY = cardY + CARD_H / 2 - 8;
            context.getMatrices().push();
            context.getMatrices().translate(iconX + 8, iconY + 8, 0);
            context.getMatrices().scale(2.0f, 2.0f, 1.0f);
            context.getMatrices().translate(-8, -8, 0);
            context.drawItem(perk.getIcon(), 0, 0);
            context.getMatrices().pop();

            // Уровень (если стакается) — в углу
            int level = comp.getPerkLevel(perk.getId());
            if (perk.getMaxLevel() > 1) {
                context.drawText(this.textRenderer, "Lv." + level,
                        cardX + 6, cardY + CARD_H - 14, COL_TEXT, false);
            }
        }

        // === Область описания (снизу, как в референсе) ===
        int descY = cardY + CARD_H + 12;
        context.fill(px + 20, descY, px + PANEL_W - 20, descY + 40, 0x40000000);
        context.fill(px + 20, descY, px + PANEL_W - 20, descY + 1, COL_BORDER_D);

        // Показываем описание выбранного (или наведённого) перка
        Perk display = hovered != null ? hovered : ownedPerks.get(selectedIndex);
        if (display != null) {
            context.drawText(this.textRenderer, display.getName(),
                    px + 28, descY + 6, COL_TEXT_SEL, false);
            context.drawText(this.textRenderer, display.getDescription(),
                    px + 28, descY + 18, 0xFFAAAAAA, false);

            // Доп. строки (например, запас прыжков)
            List<Text> extra = display.getExtraTooltip(comp, comp.getPerkLevel(display.getId()));
            int ey = descY + 30;
            for (Text line : extra) {
                context.drawText(this.textRenderer, line, px + 28, ey, 0xFFFFFFFF, false);
                ey += 10;
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Выбор карточки кликом
        int totalCards = ownedPerks.size();
        if (totalCards > 0) {
            int rowWidth = totalCards * CARD_W + (totalCards - 1) * CARD_GAP;
            int startX = panelX() + (PANEL_W - rowWidth) / 2;
            int cardY = panelY() + 45;
            for (int i = 0; i < totalCards; i++) {
                int cardX = startX + i * (CARD_W + CARD_GAP);
                if (mouseX >= cardX && mouseX < cardX + CARD_W
                        && mouseY >= cardY && mouseY < cardY + CARD_H) {
                    selectedIndex = i;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // === Вспомогательные методы отрисовки рамок ===

    /** Прямоугольная рамка толщиной 1px */
    private void drawRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** Панель с двойной золотой рамкой в стиле терминала */
    private void drawBorderedBox(DrawContext ctx, int x, int y, int w, int h, int fill) {
        ctx.fill(x, y, x + w, y + h, fill);
        // Внешняя золотая рамка
        drawRect(ctx, x, y, w, h, COL_BORDER);
        // Внутренняя тёмная рамка (отступ 3px)
        drawRect(ctx, x + 3, y + 3, w - 6, h - 6, COL_BORDER_D);
    }
}