package com.ea.bfaq.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.concurrent.ThreadLocalRandom;

public class SupportSound
{
    private static final SoundEvent SUPPORT_A = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.support_a"));
    private static final SoundEvent SUPPORT_B = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.support_b"));
    private static final SoundEvent SUPPORT_C = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.support_c"));
    private static final SoundEvent SUPPORT_EASTER = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.support_easter"));

    public static void play()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }

        SoundEvent[] sounds = {SUPPORT_A, SUPPORT_B, SUPPORT_C};
        SoundEvent sound = sounds[ThreadLocalRandom.current().nextInt(sounds.length)];
        mc.level.playLocalSound(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            sound,
            SoundSource.PLAYERS,
            1.0F, 1.0F, true
        );
    }

    public static void playEaster()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }

        mc.level.playLocalSound(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            SUPPORT_EASTER,
            SoundSource.PLAYERS,
            1.0F, 1.0F, true
        );
    }
}