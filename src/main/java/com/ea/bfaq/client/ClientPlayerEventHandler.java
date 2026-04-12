package com.ea.bfaq.client;

import com.ea.bfaq.BF1Q;
import com.ea.bfaq.mark.MarkManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BF1Q.MODID, value = Dist.CLIENT)
public class ClientPlayerEventHandler
{
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        // 玩家重生时清理所有标记数据，避免音效混乱
        MarkManager.getInstance().clearAll();
    }
    
    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        // 玩家传送（维度变化）时清理所有标记数据，避免音效混乱
        MarkManager.getInstance().clearAll();
    }
}