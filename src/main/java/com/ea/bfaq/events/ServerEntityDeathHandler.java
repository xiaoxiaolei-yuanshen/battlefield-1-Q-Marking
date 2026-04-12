package com.ea.bfaq.events;

import com.ea.bfaq.BF1Q;
import com.ea.bfaq.mark.MarkData;
import com.ea.bfaq.mark.MarkManager;
import com.ea.bfaq.network.NetworkHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BF1Q.MODID, value = Dist.DEDICATED_SERVER)
public class ServerEntityDeathHandler
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
        
        // 发送击杀音效包到客户端
        NetworkHandler.INSTANCE.send(
            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> (net.minecraft.server.level.ServerPlayer) player),
            new NetworkHandler.KillSoundPacket(mark.isFriendly())
        );
        
        MarkManager.getInstance().removeMark(deadEntity.getUUID());
    }
}