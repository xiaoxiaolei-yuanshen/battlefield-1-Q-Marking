package com.ea.bfaq.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBindings
{
    public static final String KEY_CATEGORY = "key.categories.bfq";
    public static final KeyMapping MARK_ENEMY = new KeyMapping(
            "key.bfq.mark_enemy",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            KEY_CATEGORY
    );
}
