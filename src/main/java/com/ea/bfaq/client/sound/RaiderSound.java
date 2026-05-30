package com.ea.bfaq.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.concurrent.ThreadLocalRandom;

public class RaiderSound
{
    private static final SoundEvent RAIDER_A = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.raider_a"));
    private static final SoundEvent RAIDER_B = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.raider_b"));
    private static final SoundEvent RAIDER_C = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.raider_c"));

    public static void play()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }

        SoundEvent[] sounds = {RAIDER_A, RAIDER_B, RAIDER_C};
        SoundEvent sound = sounds[ThreadLocalRandom.current().nextInt(sounds.length)];
        mc.level.playLocalSound(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            sound,
            SoundSource.PLAYERS,
            1.0F, 1.0F, true
        );
    }
}