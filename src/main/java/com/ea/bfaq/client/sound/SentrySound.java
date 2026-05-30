package com.ea.bfaq.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.concurrent.ThreadLocalRandom;

public class SentrySound
{
    private static final SoundEvent SENTRY_A = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.sentry_a"));
    private static final SoundEvent SENTRY_B = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.sentry_b"));
    private static final SoundEvent SENTRY_C = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.sentry_c"));

    public static void play()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }

        SoundEvent[] sounds = {SENTRY_A, SENTRY_B, SENTRY_C};
        SoundEvent sound = sounds[ThreadLocalRandom.current().nextInt(sounds.length)];
        mc.level.playLocalSound(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            sound,
            SoundSource.PLAYERS,
            1.0F, 1.0F, true
        );
    }
}