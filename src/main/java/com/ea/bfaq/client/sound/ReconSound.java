package com.ea.bfaq.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.concurrent.ThreadLocalRandom;

public class ReconSound
{
    private static final SoundEvent RECON_A = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.recon_a"));
    private static final SoundEvent RECON_B = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.recon_b"));
    private static final SoundEvent RECON_C = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.recon_c"));
    private static final SoundEvent RECON_EASTER = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.recon_easter"));

    public static void play()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }

        SoundEvent[] sounds = {RECON_A, RECON_B, RECON_C};
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
            RECON_EASTER,
            SoundSource.PLAYERS,
            1.0F, 1.0F, true
        );
    }
}