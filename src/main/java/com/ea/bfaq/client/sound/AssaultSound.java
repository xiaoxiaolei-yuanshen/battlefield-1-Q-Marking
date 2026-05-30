package com.ea.bfaq.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.concurrent.ThreadLocalRandom;

public class AssaultSound
{
    private static final SoundEvent ASSAULT_A = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.assault_a"));
    private static final SoundEvent ASSAULT_B = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.assault_b"));
    private static final SoundEvent ASSAULT_C = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.assault_c"));
    private static final SoundEvent ASSAULT_EASTER = SoundEvent.createVariableRangeEvent(new ResourceLocation("bfq", "mark.assault_easter"));

    public static void play()
    {
        System.out.println("[AssaultSound] ========== play() 被调用 ==========");
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            System.out.println("[AssaultSound] mc.player 或 mc.level 为 null，返回");
            return;
        }

        SoundEvent[] sounds = {ASSAULT_A, ASSAULT_B, ASSAULT_C};
        int randomIndex = ThreadLocalRandom.current().nextInt(sounds.length);
        SoundEvent sound = sounds[randomIndex];
        String name = sound.getLocation().toString();
        System.out.println("[AssaultSound] 随机选择索引 " + randomIndex + " -> " + name);
        System.out.println("[AssaultSound] 调用 playLocalSound...");
        mc.level.playLocalSound(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            sound,
            SoundSource.PLAYERS,
            1.0F, 1.0F, true
        );
        System.out.println("[AssaultSound] ========== play() 结束 ==========");
    }

    public static void playEaster()
    {
        System.out.println("[AssaultSound] ========== playEaster() 被调用 ==========");
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            System.out.println("[AssaultSound] mc.player 或 mc.level 为 null，返回");
            return;
        }

        System.out.println("[AssaultSound] 调用 playLocalSound 播放 ASSAULT_EASTER");
        mc.level.playLocalSound(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            ASSAULT_EASTER,
            SoundSource.PLAYERS,
            1.0F, 1.0F, true
        );
        System.out.println("[AssaultSound] ========== playEaster() 结束 ==========");
    }
}