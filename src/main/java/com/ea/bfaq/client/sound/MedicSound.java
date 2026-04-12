package com.ea.bfaq.client.sound;

import com.ea.bfaq.SoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;

import java.util.Random;

public class MedicSound
{
    private static final Random RANDOM = new Random();
    private static final SoundEvent[] SOUNDS = {
        SoundEvents.MEDIC_A.get(),
        SoundEvents.MEDIC_B.get(),
        SoundEvents.MEDIC_C.get()
    };
    
    public static void play()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }
        
        // 普通音效只在本地播放
        SoundEvent sound = SOUNDS[RANDOM.nextInt(SOUNDS.length)];
        mc.level.playLocalSound(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            sound,
            net.minecraft.sounds.SoundSource.PLAYERS,
            10.0F, 1.0F, true
        );
    }
    
    public static void playEaster()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }
        
        // 播放彩蛋音效
        mc.level.playLocalSound(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            SoundEvents.MEDIC_EASTER.get(),
            net.minecraft.sounds.SoundSource.PLAYERS,
            10.0F, 1.0F, true
        );
    }
}
