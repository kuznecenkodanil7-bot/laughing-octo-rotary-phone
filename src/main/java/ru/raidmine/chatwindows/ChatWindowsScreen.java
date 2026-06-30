package ru.raidmine.chatwindows;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

public class ChatWindowsScreen extends Screen {
    private static final int PANEL_WIDTH = 240;

    private ChatWindow selected;
    private TextFieldWidget nameField;
    private TextFieldWidget keywordsField;
    private ChatWindow dragging;
    private ChatWindow resizing;
    private double dragOffsetX;
    private double dragOffsetY;

    public ChatWindowsScreen() {
        super(Text.literal("Chat Windows"));
    }

    @Override
    protected void init() {
        if (selected == null && !ChatWindowsClient.MANAGER.windows().isEmpty()) {
            selected = ChatWindowsClient.MANAGER.windows().getFirst();
        }
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        clearChildren();

        addDrawableChild(ButtonWidget.builder(Text.literal("+ окно"), button -> {
            storeFields();
            selected = ChatWindowsClient.MANAGER.addWindow();
            rebuildWidgets();
        }).dimensions(10, 14, 70, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("сохранить"), button -> {
            storeFields();
            ChatWindowsClient.MANAGER.save();
        }).dimensions(86, 14, 86, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("закрыть"), button -> close()).dimensions(178, 14, 55, 20).build());

        int y = 52;
        for (ChatWindow window : ChatWindowsClient.MANAGER.windows()) {
            ButtonWidget button = ButtonWidget.builder(Text.literal(window.name), clicked -> {
                storeFields();
                selected = window;
                rebuildWidgets();
            }).dimensions(10, y, 223, 20).build();
            addDrawableChild(button);
            y += 23;
            if (y > Math.min(this.height - 190, 220)) {
                break;
            }
        }

        int formY = Math.max(250, y + 12);
        nameField = new TextFieldWidget(textRenderer, 10, formY + 22, 223, 20, Text.literal("Название окна"));
        keywordsField = new TextFieldWidget(textRenderer, 10, formY + 66, 223, 20, Text.literal("Ключевые слова"));
        keywordsField.setMaxLength(4096);

        if (selected != null) {
            nameField.setText(selected.name);
            keywordsField.setText(selected.keywordsAsText());
        }
        addDrawableChild(nameField);
        addDrawableChild(keywordsField);

        addDrawableChild(ButtonWidget.builder(Text.literal(toggleHideText()), button -> {
            if (selected != null) {
                storeFields();
                selected.hideFromMain = !selected.hideFromMain;
                ChatWindowsClient.MANAGER.save();
                rebuildWidgets();
            }
        }).dimensions(10, formY + 96, 223, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(toggleCaseText()), button -> {
            if (selected != null) {
                storeFields();
                selected.matchCase = !selected.matchCase;
                ChatWindowsClient.MANAGER.save();
                rebuildWidgets();
            }
        }).dimensions(10, formY + 119, 223, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal(toggleModeText()), button -> {
            if (selected != null) {
                storeFields();
                selected.requireAllKeywords = !selected.requireAllKeywords;
                ChatWindowsClient.MANAGER.save();
                rebuildWidgets();
            }
        }).dimensions(10, formY + 142, 223, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("удалить окно"), button -> {
            if (selected != null) {
                ChatWindow toRemove = selected;
                selected = null;
                ChatWindowsClient.MANAGER.removeWindow(toRemove);
                if (!ChatWindowsClient.MANAGER.windows().isEmpty()) {
                    selected = ChatWindowsClient.MANAGER.windows().getFirst();
                }
                rebuildWidgets();
            }
        }).dimensions(10, formY + 165, 105, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("сброс"), button -> {
            selected = null;
            ChatWindowsClient.MANAGER.resetDefaults();
            if (!ChatWindowsClient.MANAGER.windows().isEmpty()) {
                selected = ChatWindowsClient.MANAGER.windows().getFirst();
            }
            rebuildWidgets();
        }).dimensions(128, formY + 165, 105, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, PANEL_WIDTH, height, 0xDD060606);
        context.fill(PANEL_WIDTH, 0, width, height, 0x66000000);

        context.drawTextWithShadow(textRenderer, "Chat Windows", 10, 4, 0xFFFFAA3D);
        context.drawTextWithShadow(textRenderer, "ПКМ/ЛКМ по окну — выбрать", 10, 38, 0xFFBDBDBD);

        int formY = getFormY();
        context.drawTextWithShadow(textRenderer, "Название", 10, formY + 10, 0xFFECECEC);
        context.drawTextWithShadow(textRenderer, "Ключевые слова через запятую", 10, formY + 54, 0xFFECECEC);

        if (selected != null) {
            context.drawTextWithShadow(textRenderer, "Выбрано: " + selected.name, 10, formY - 14, 0xFFFFD36A);
        }

        List<ChatWindow> windows = ChatWindowsClient.MANAGER.windows();
        for (ChatWindow window : windows) {
            ChatWindowsClient.MANAGER.renderWindow(context, window, true, window == selected, mouseX, mouseY);
        }

        context.drawTextWithShadow(textRenderer,
                "Зажми заголовок окна и двигай. Потяни нижний правый угол, чтобы изменить размер.",
                PANEL_WIDTH + 12, height - 18, 0xFFECECEC);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }

        double mouseX = click.x();
        double mouseY = click.y();

        List<ChatWindow> windows = ChatWindowsClient.MANAGER.windows();
        for (int i = windows.size() - 1; i >= 0; i--) {
            ChatWindow window = windows.get(i);
            if (ChatWindowsClient.MANAGER.isOverResizeHandle(window, mouseX, mouseY)) {
                storeFields();
                selected = window;
                resizing = window;
                rebuildWidgets();
                return true;
            }
            if (ChatWindowsClient.MANAGER.isOverHeader(window, mouseX, mouseY)) {
                storeFields();
                selected = window;
                dragging = window;
                dragOffsetX = mouseX - window.x;
                dragOffsetY = mouseY - window.y;
                rebuildWidgets();
                return true;
            }
            if (ChatWindowsClient.MANAGER.isInsideWindow(window, mouseX, mouseY)) {
                storeFields();
                selected = window;
                rebuildWidgets();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        double mouseX = click.x();
        double mouseY = click.y();

        if (dragging != null) {
            dragging.x = clamp((int) (mouseX - dragOffsetX), PANEL_WIDTH + 2, width - 30);
            dragging.y = clamp((int) (mouseY - dragOffsetY), 0, height - 30);
            return true;
        }
        if (resizing != null) {
            resizing.width = Math.max(160, (int) mouseX - resizing.x);
            resizing.height = Math.max(70, (int) mouseY - resizing.y);
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null || resizing != null) {
            dragging = null;
            resizing = null;
            ChatWindowsClient.MANAGER.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void removed() {
        storeFields();
        ChatWindowsClient.MANAGER.save();
        super.removed();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void storeFields() {
        if (selected == null || nameField == null || keywordsField == null) {
            return;
        }
        String name = nameField.getText().trim();
        selected.name = name.isEmpty() ? "Окно" : name;
        selected.setKeywordsFromText(keywordsField.getText());
    }

    private int getFormY() {
        int y = 52;
        for (int i = 0; i < ChatWindowsClient.MANAGER.windows().size(); i++) {
            y += 23;
            if (y > Math.min(this.height - 190, 220)) {
                break;
            }
        }
        return Math.max(250, y + 12);
    }

    private String toggleHideText() {
        if (selected == null) {
            return "скрывать из основного: -";
        }
        return selected.hideFromMain ? "скрывать из основного: да" : "скрывать из основного: нет";
    }

    private String toggleCaseText() {
        if (selected == null) {
            return "учитывать регистр: -";
        }
        return selected.matchCase ? "учитывать регистр: да" : "учитывать регистр: нет";
    }

    private String toggleModeText() {
        if (selected == null) {
            return "режим фильтра: -";
        }
        return selected.requireAllKeywords ? "режим фильтра: все слова" : "режим фильтра: любое слово";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
