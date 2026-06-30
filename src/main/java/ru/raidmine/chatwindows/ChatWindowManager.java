package ru.raidmine.chatwindows;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ChatWindowManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int BACKGROUND = 0xAA090909;
    private static final int HEADER = 0xDD1C1C1C;
    private static final int BORDER = 0xAAFF8A22;
    private static final int SELECTED_BORDER = 0xFFFFAA3D;
    private static final int TEXT = 0xFFECECEC;
    private static final int DIM_TEXT = 0xFFB8B8B8;

    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("chatwindows.json");
    private final List<ChatWindow> windows = new ArrayList<>();

    public List<ChatWindow> windows() {
        return windows;
    }

    public void load() {
        windows.clear();
        if (!Files.exists(configPath)) {
            createDefaults();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            ChatWindowsConfig config = GSON.fromJson(reader, ChatWindowsConfig.class);
            if (config != null && config.windows != null) {
                windows.addAll(config.windows);
            }
        } catch (Exception ignored) {
            createDefaults();
        }

        if (windows.isEmpty()) {
            createDefaults();
        }
        for (ChatWindow window : windows) {
            prepareWindow(window);
        }
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            ChatWindowsConfig config = new ChatWindowsConfig();
            config.windows = windows;
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException ignored) {
        }
    }

    public ChatWindow addWindow() {
        ChatWindow window = new ChatWindow();
        window.id = UUID.randomUUID().toString();
        window.name = "Новое окно " + (windows.size() + 1);
        window.x = 260 + windows.size() * 18;
        window.y = 35 + windows.size() * 18;
        window.keywords = new ArrayList<>(List.of("ключевое_слово"));
        prepareWindow(window);
        windows.add(window);
        save();
        return window;
    }

    public void removeWindow(ChatWindow window) {
        windows.remove(window);
        save();
    }

    public void resetDefaults() {
        windows.clear();
        createDefaults();
        save();
    }

    public boolean routeMessage(Text message) {
        if (message == null) {
            return false;
        }

        String plain = message.getString();
        boolean matched = false;
        boolean hideFromMain = false;

        for (ChatWindow window : windows) {
            prepareWindow(window);
            if (window.matches(plain)) {
                window.addMessage(message);
                matched = true;
                hideFromMain |= window.hideFromMain;
            }
        }

        return matched && hideFromMain;
    }

    public void renderHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options.hudHidden || client.currentScreen instanceof ChatWindowsScreen) {
            return;
        }

        for (ChatWindow window : windows) {
            if (window.enabled) {
                renderWindow(context, window, false, false, 0, 0);
            }
        }
    }

    public void renderWindow(DrawContext context, ChatWindow window, boolean editMode, boolean selected, int mouseX, int mouseY) {
        prepareWindow(window);
        window.clampSize();

        int x1 = window.x;
        int y1 = window.y;
        int x2 = window.x + window.width;
        int y2 = window.y + window.height;

        context.fill(x1, y1, x2, y2, BACKGROUND);
        context.fill(x1, y1, x2, y1 + 16, HEADER);
        drawBorder(context, x1, y1, x2, y2, selected ? SELECTED_BORDER : BORDER);

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer renderer = client.textRenderer;
        String title = window.name + "  •  " + window.messages.size();
        context.drawTextWithShadow(renderer, title, x1 + 6, y1 + 4, TEXT);

        int statusColor = window.hideFromMain ? 0xFF72E86A : 0xFFFFD36A;
        String status = window.hideFromMain ? "из основного скрывается" : "копия в основной чат";
        int statusWidth = renderer.getWidth(status);
        if (statusWidth + 12 < window.width) {
            context.drawTextWithShadow(renderer, status, x2 - statusWidth - 6, y1 + 4, statusColor);
        }

        int contentX = x1 + 6;
        int contentY = y1 + 21;
        int contentWidth = Math.max(20, window.width - 12);
        int contentHeight = Math.max(18, window.height - 27);

        context.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);
        renderMessages(context, renderer, window, contentX, contentY, contentWidth, contentHeight);
        context.disableScissor();

        if (editMode) {
            drawResizeCorner(context, x2, y2, selected);
            if (isInside(mouseX, mouseY, x1, y1, x2, y2)) {
                context.drawTextWithShadow(renderer, "перетащи заголовок / угол", x1 + 6, y2 - 13, 0xFFDDDDDD);
            }
        }
    }

    public boolean isOverResizeHandle(ChatWindow window, double mouseX, double mouseY) {
        int x2 = window.x + window.width;
        int y2 = window.y + window.height;
        return mouseX >= x2 - 12 && mouseX <= x2 + 2 && mouseY >= y2 - 12 && mouseY <= y2 + 2;
    }

    public boolean isOverHeader(ChatWindow window, double mouseX, double mouseY) {
        return mouseX >= window.x && mouseX <= window.x + window.width && mouseY >= window.y && mouseY <= window.y + 16;
    }

    public boolean isInsideWindow(ChatWindow window, double mouseX, double mouseY) {
        return mouseX >= window.x && mouseX <= window.x + window.width && mouseY >= window.y && mouseY <= window.y + window.height;
    }

    private void renderMessages(DrawContext context, TextRenderer renderer, ChatWindow window, int x, int y, int width, int height) {
        if (window.messages.isEmpty()) {
            context.drawTextWithShadow(renderer, "Сообщений пока нет", x, y, DIM_TEXT);
            context.drawTextWithShadow(renderer, "Добавь ключевые слова в редакторе", x, y + 11, DIM_TEXT);
            return;
        }

        List<String> lines = new ArrayList<>();
        for (int i = Math.max(0, window.messages.size() - 40); i < window.messages.size(); i++) {
            lines.addAll(wrap(renderer, window.messages.get(i).displayText(), width));
        }

        int lineHeight = 10;
        int maxLines = Math.max(1, height / lineHeight);
        int from = Math.max(0, lines.size() - maxLines);
        int yy = y;
        for (int i = from; i < lines.size(); i++) {
            context.drawTextWithShadow(renderer, lines.get(i), x, yy, TEXT);
            yy += lineHeight;
        }
    }

    private List<String> wrap(TextRenderer renderer, String text, int maxWidth) {
        if (renderer.getWidth(text) <= maxWidth) {
            return List.of(text);
        }

        List<String> result = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (renderer.getWidth(candidate) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
            } else {
                if (!line.isEmpty()) {
                    result.add(line.toString());
                    line.setLength(0);
                }
                splitLongWord(renderer, word, maxWidth, result, line);
            }
        }
        if (!line.isEmpty()) {
            result.add(line.toString());
        }
        return result;
    }

    private void splitLongWord(TextRenderer renderer, String word, int maxWidth, List<String> result, StringBuilder currentLine) {
        StringBuilder part = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            String candidate = part.toString() + word.charAt(i);
            if (renderer.getWidth(candidate) > maxWidth && !part.isEmpty()) {
                result.add(part.toString());
                part.setLength(0);
            }
            part.append(word.charAt(i));
        }
        currentLine.append(part);
    }

    private void drawBorder(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        context.fill(x1, y1, x2, y1 + 1, color);
        context.fill(x1, y2 - 1, x2, y2, color);
        context.fill(x1, y1, x1 + 1, y2, color);
        context.fill(x2 - 1, y1, x2, y2, color);
    }

    private void drawResizeCorner(DrawContext context, int x2, int y2, boolean selected) {
        int color = selected ? SELECTED_BORDER : BORDER;
        context.fill(x2 - 10, y2 - 2, x2 - 2, y2 - 1, color);
        context.fill(x2 - 6, y2 - 6, x2 - 2, y2 - 5, color);
        context.fill(x2 - 2, y2 - 10, x2 - 1, y2 - 2, color);
    }

    private boolean isInside(int mouseX, int mouseY, int x1, int y1, int x2, int y2) {
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
    }

    private void prepareWindow(ChatWindow window) {
        if (window.id == null || window.id.isBlank()) {
            window.id = UUID.randomUUID().toString();
        }
        if (window.name == null || window.name.isBlank()) {
            window.name = "Окно";
        }
        if (window.keywords == null) {
            window.keywords = new ArrayList<>();
        }
        if (window.messages == null) {
            window.messages = new ArrayList<>();
        }
        window.clampSize();
    }

    private void createDefaults() {
        ChatWindow staff = new ChatWindow();
        staff.id = "staff-chat";
        staff.name = "Чат персонала";
        staff.x = 260;
        staff.y = 30;
        staff.width = 330;
        staff.height = 130;
        staff.keywords = new ArrayList<>(Arrays.asList("[A]", "[Staff]", "[Персонал]", "админ-чат"));
        staff.hideFromMain = true;

        ChatWindow reports = new ChatWindow();
        reports.id = "reports";
        reports.name = "Жалобы / репорты";
        reports.x = 260;
        reports.y = 175;
        reports.width = 330;
        reports.height = 130;
        reports.keywords = new ArrayList<>(Arrays.asList("жалоба", "репорт", "report", "/report"));
        reports.hideFromMain = true;

        ChatWindow warns = new ChatWindow();
        warns.id = "alerts";
        warns.name = "Подозрительные слова";
        warns.x = 605;
        warns.y = 30;
        warns.width = 300;
        warns.height = 130;
        warns.keywords = new ArrayList<>(Arrays.asList("чит", "xray", "killaura", "kill aura", "fly"));
        warns.hideFromMain = false;

        windows.add(staff);
        windows.add(reports);
        windows.add(warns);
        for (ChatWindow window : windows) {
            prepareWindow(window);
        }
    }
}
