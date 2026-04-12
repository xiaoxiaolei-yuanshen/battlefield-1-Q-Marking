package com.ea.bfaq.events;

import com.ea.bfaq.BF1Q;
import com.ea.bfaq.client.gui.ClassSelectionScreen;
import com.ea.bfaq.commands.ZYCommand;
import com.ea.bfaq.mark.MarkManager;
import com.ea.bfaq.network.NetworkHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = BF1Q.MODID)
public class PlayerEventHandler
{
    private static final String CLASS_TAG = "bfq_class";
    private static final String HAS_CHOSEN_CLASS_TAG = "bfq_has_chosen_class";
    
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
        {
            return;
        }
        
        CompoundTag data = player.getPersistentData();
        
        if (data.contains(CLASS_TAG))
        {
            String className = data.getString(CLASS_TAG);
            ZYCommand.setPlayerClass(player.getUUID(), className);
            syncClassToAllClients(player.getUUID(), className);
        }
        else
        {
            if (!data.getBoolean(HAS_CHOSEN_CLASS_TAG))
            {
                String[] classes = {"assault", "medic", "recon", "support"};
                String randomClass = classes[new java.util.Random().nextInt(classes.length)];
                setPlayerClassWithSave(player, randomClass);
            }
        }
        
        syncAllExistingClassesToNewPlayer(player);
    }
    
    private static void syncAllExistingClassesToNewPlayer(ServerPlayer newPlayer)
    {
        java.util.Map<UUID, String> allClasses = ZYCommand.getAllPlayerClasses();
        for (java.util.Map.Entry<UUID, String> entry : allClasses.entrySet())
        {
            if (!entry.getKey().equals(newPlayer.getUUID()))
            {
                NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> newPlayer),
                    new NetworkHandler.ClassSyncPacket(entry.getKey(), entry.getValue())
                );
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event)
    {
        if (!event.isWasDeath())
        {
            return;
        }
        
        if (event.getOriginal() instanceof ServerPlayer originalPlayer && event.getEntity() instanceof ServerPlayer newPlayer)
        {
            CompoundTag originalData = originalPlayer.getPersistentData();
            CompoundTag newData = newPlayer.getPersistentData();
            
            if (originalData.contains(CLASS_TAG))
            {
                newData.putString(CLASS_TAG, originalData.getString(CLASS_TAG));
            }
            
            if (originalData.contains(HAS_CHOSEN_CLASS_TAG))
            {
                newData.putBoolean(HAS_CHOSEN_CLASS_TAG, originalData.getBoolean(HAS_CHOSEN_CLASS_TAG));
            }
        }
    }
    
    public static void setPlayerClassWithSave(ServerPlayer player, String className)
    {
        CompoundTag data = player.getPersistentData();
        data.putString(CLASS_TAG, className);
        data.putBoolean(HAS_CHOSEN_CLASS_TAG, true);
        
        ZYCommand.setPlayerClass(player.getUUID(), className);
        syncClassToAllClients(player.getUUID(), className);
    }
    
    private static void syncClassToAllClients(UUID playerUUID, String className)
    {
        for (ServerPlayer player : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers())
        {
            NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new NetworkHandler.ClassSyncPacket(playerUUID, className)
            );
        }
    }
}