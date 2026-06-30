package ru.raidmine.chatwindows;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class ChatWindowsClient implements ClientModInitializer {
    public static final ChatWindowManager MANAGER = new ChatWindowManager();

    private KeyBinding editorKey;

    @Override
    public void onInitializeClient() {
        MANAGER.load();

        editorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.chatwindows.editor",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                new KeyBinding.Category(Identifier.of("chatwindows", "main"))
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (editorKey.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new ChatWindowsScreen());
            }
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> MANAGER.renderHud(context));
    }
}
