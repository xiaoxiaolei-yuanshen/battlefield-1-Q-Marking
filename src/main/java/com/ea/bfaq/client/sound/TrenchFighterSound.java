package com.ea.bfaq.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class TrenchFighterSound
{
    private static final SoundEvent TRENCH_FIGHTER = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.trench_fighter"));

    public static void play()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }

        mc.level.playLocalSound(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            TRENCH_FIGHTER,
            SoundSource.PLAYERS,
            1.0F, 1.0F, true
        );
    }
}