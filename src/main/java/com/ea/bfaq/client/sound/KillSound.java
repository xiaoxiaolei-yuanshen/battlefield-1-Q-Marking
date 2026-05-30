package com.ea.bfaq.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class KillSound
{
    private static final SoundEvent KILL = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.kill"));
    private static final SoundEvent HEADSHOT_KILL = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.headshot_kill"));

    public static void play(boolean isHeadshot)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }

        if (isHeadshot)
        {
            mc.level.playLocalSound(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                HEADSHOT_KILL,
                SoundSource.PLAYERS,
                1.0F, 1.0F, true
            );
        }
        else
        {
            mc.level.playLocalSound(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                KILL,
                SoundSource.PLAYERS,
                1.0F, 1.0F, true
            );
        }
    }
}