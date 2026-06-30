package ru.raidmine.chatwindows;

import net.minecraft.text.Text;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatLine {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public transient Text text;
    public String plain;
    public String time;

    public ChatLine() {
    }

    public ChatLine(Text text) {
        this.text = text.copy();
        this.plain = text.getString();
        this.time = LocalTime.now().format(TIME_FORMAT);
    }

    public String displayText() {
        return "[" + time + "] " + plain;
    }
}
