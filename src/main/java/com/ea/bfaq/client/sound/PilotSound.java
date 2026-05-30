package com.ea.bfaq.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.concurrent.ThreadLocalRandom;

public class PilotSound
{
    private static final SoundEvent BOMBER_A = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.bomber_a"));
    private static final SoundEvent BOMBER_B = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.bomber_b"));
    private static final SoundEvent BOMBER_C = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.bomber_c"));
    private static final SoundEvent BOMBER_EASTER = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.bomber_easter"));
    private static final SoundEvent PLANE_A = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.plane_a"));
    private static final SoundEvent PLANE_B = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.plane_b"));
    private static final SoundEvent PLANE_C = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.plane_c"));

    public static void play(boolean isBomber)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }

        SoundEvent[] sounds;
        if (isBomber)
        {
            sounds = new SoundEvent[]{BOMBER_A, BOMBER_B, BOMBER_C};
        }
        else
        {
            sounds = new SoundEvent[]{PLANE_A, PLANE_B, PLANE_C};
        }
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
            BOMBER_EASTER,
            SoundSource.PLAYERS,
            1.0F, 1.0F, true
        );
    }
}