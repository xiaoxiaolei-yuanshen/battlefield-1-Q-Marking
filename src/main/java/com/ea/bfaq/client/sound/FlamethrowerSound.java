package com.ea.bfaq.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.concurrent.ThreadLocalRandom;

public class FlamethrowerSound
{
    private static final SoundEvent FLAMETHROWER_A = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.flamethrower_a"));
    private static final SoundEvent FLAMETHROWER_B = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.flamethrower_b"));
    private static final SoundEvent FLAMETHROWER_C = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.flamethrower_c"));

    public static void play()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }

        SoundEvent[] sounds = {FLAMETHROWER_A, FLAMETHROWER_B, FLAMETHROWER_C};
        SoundEvent sound = sounds[ThreadLocalRandom.current().nextInt(sounds.length)];
        mc.level.playLocalSound(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            sound,
            SoundSource.PLAYERS,
            1.0F, 1.0F, true
        );
    }
}