package com.ea.bfaq.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.concurrent.ThreadLocalRandom;

public class NeedAmmoSound
{
    private static final SoundEvent NEED_AMMO_A = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.need_ammo_a"));
    private static final SoundEvent NEED_AMMO_B = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.need_ammo_b"));
    private static final SoundEvent NEED_AMMO_C = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.need_ammo_c"));
    private static final SoundEvent NEED_AMMO_D = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.need_ammo_d"));
    private static final SoundEvent NEED_AMMO_E = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.need_ammo_e"));

    public static void play()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }

        SoundEvent[] sounds = {NEED_AMMO_A, NEED_AMMO_B, NEED_AMMO_C, NEED_AMMO_D, NEED_AMMO_E};
        SoundEvent sound = sounds[ThreadLocalRandom.current().nextInt(sounds.length)];
        mc.level.playLocalSound(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            sound,
            SoundSource.PLAYERS,
            1.0F, 1.0F, true
        );
    }
}