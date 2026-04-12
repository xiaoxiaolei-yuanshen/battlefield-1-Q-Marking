package com.ea.bfaq;

import com.ea.bfaq.mark.MarkData;
import com.ea.bfaq.mark.MarkManager;
import com.ea.bfaq.network.NetworkHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BF1Q.MODID, value = Dist.CLIENT)
public class EntityDeathHandler
{
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event)
    {
        LivingEntity deadEntity = event.getEntity();
        
        MarkData mark = MarkManager.getInstance().getMark(deadEntity.getUUID());
        if (mark == null)
        {
            return;
        }
        
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof Player player))
        {
            return;
        }
        
        // 检查是否安装了GD656击杀图标模组
        boolean hasGD656KillIconMod = net.minecraftforge.fml.ModList.get().isLoaded("gd656killicon");
        if (!hasGD656KillIconMod)
        {
            // 本地播放击杀音效
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null)
            {
                if (mark.isFriendly())
                {
                    mc.player.playSound(
                        com.ea.bfaq.SoundEvents.HEADSHOT_KILL.get(),
                        10.0F, 1.0F
                    );
                }
                else
                {
                    mc.player.playSound(
                        com.ea.bfaq.SoundEvents.KILL.get(),
                        10.0F, 1.0F
                    );
                }
            }
            
            // 发送击杀音效网络包
            NetworkHandler.INSTANCE.sendToServer(
                    new NetworkHandler.KillSoundPacket(mark.isFriendly())
            );
        }
        
        MarkManager.getInstance().removeMark(deadEntity.getUUID());
    }
}
