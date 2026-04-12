package com.ea.bfaq.client.sound;

import com.ea.bfaq.SoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;

import java.util.Random;

public class PilotSound
{
    private static final Random RANDOM = new Random();
    private static final SoundEvent[] PLANE_SOUNDS = {
        SoundEvents.PLANE_A.get(),
        SoundEvents.PLANE_B.get(),
        SoundEvents.PLANE_C.get()
    };
    private static final SoundEvent[] BOMBER_SOUNDS = {
        SoundEvents.BOMBER_A.get(),
        SoundEvents.BOMBER_B.get(),
        SoundEvents.BOMBER_C.get()
    };
    
    public static void play(boolean isBomber)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }
        
        SoundEvent[] sounds = isBomber ? BOMBER_SOUNDS : PLANE_SOUNDS;
        SoundEvent sound = sounds[RANDOM.nextInt(sounds.length)];
        mc.level.playLocalSound(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            sound,
            net.minecraft.sounds.SoundSource.PLAYERS,
            10.0F, 1.0F, true
        );
    }
}
