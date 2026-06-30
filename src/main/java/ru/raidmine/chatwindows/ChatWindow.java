package ru.raidmine.chatwindows;

import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatWindow {
    public String id = "window";
    public String name = "Новое окно";
    public int x = 260;
    public int y = 30;
    public int width = 260;
    public int height = 130;
    public boolean enabled = true;
    public boolean hideFromMain = true;
    public boolean matchCase = false;
    public boolean requireAllKeywords = false;
    public int maxMessages = 100;
    public List<String> keywords = new ArrayList<>();
    public transient List<ChatLine> messages = new ArrayList<>();

    public boolean matches(String message) {
        if (!enabled || keywords == null || keywords.isEmpty()) {
            return false;
        }

        String source = matchCase ? message : message.toLowerCase(Locale.ROOT);
        boolean hasUsableKeyword = false;

        if (requireAllKeywords) {
            for (String keyword : keywords) {
                String normalized = normalizeKeyword(keyword);
                if (normalized.isBlank()) {
                    continue;
                }
                hasUsableKeyword = true;
                if (!source.contains(normalized)) {
                    return false;
                }
            }
            return hasUsableKeyword;
        }

        for (String keyword : keywords) {
            String normalized = normalizeKeyword(keyword);
            if (!normalized.isBlank()) {
                hasUsableKeyword = true;
                if (source.contains(normalized)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void addMessage(Text message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(new ChatLine(message));
        while (messages.size() > Math.max(10, maxMessages)) {
            messages.removeFirst();
        }
    }

    public String keywordsAsText() {
        if (keywords == null || keywords.isEmpty()) {
            return "";
        }
        return String.join(", ", keywords);
    }

    public void setKeywordsFromText(String text) {
        keywords = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return;
        }
        for (String part : text.split(",")) {
            String cleaned = part.trim();
            if (!cleaned.isEmpty()) {
                keywords.add(cleaned);
            }
        }
    }

    public void clampSize() {
        width = Math.max(160, width);
        height = Math.max(70, height);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }
        String trimmed = keyword.trim();
        return matchCase ? trimmed : trimmed.toLowerCase(Locale.ROOT);
    }
}
