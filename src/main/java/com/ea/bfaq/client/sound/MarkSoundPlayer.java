package com.ea.bfaq.client.sound;

import com.ea.bfaq.client.mark.MarkSoundHandler;
import com.ea.bfaq.mark.MarkData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.concurrent.ThreadLocalRandom;

public class MarkSoundPlayer
{
    public static void playMarkSound(Entity entity, MarkData.MarkType markType)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }

        MarkSoundHandler.playMarkSound(entity, markType);
    }
}