package com.ea.bfaq.client.sound;

import com.ea.bfaq.SoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;

public class InvaderSound
{
    public static void play()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }
        
        SoundEvent sound = SoundEvents.INVADER.get();
        mc.level.playLocalSound(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            sound,
            net.minecraft.sounds.SoundSource.PLAYERS,
            10.0F, 1.0F, true
        );
    }
}