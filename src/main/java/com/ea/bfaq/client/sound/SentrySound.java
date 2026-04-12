package com.ea.bfaq.client.sound;

import com.ea.bfaq.SoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;

import java.util.Random;

public class SentrySound
{
    private static final Random RANDOM = new Random();
    private static final SoundEvent[] SOUNDS = {
        SoundEvents.SENTRY_A.get(),
        SoundEvents.SENTRY_B.get(),
        SoundEvents.SENTRY_C.get()
    };
    
    public static void play()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }
        
        SoundEvent sound = SOUNDS[RANDOM.nextInt(SOUNDS.length)];
        mc.level.playLocalSound(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            sound,
            net.minecraft.sounds.SoundSource.PLAYERS,
            10.0F, 1.0F, true
        );
    }
}
