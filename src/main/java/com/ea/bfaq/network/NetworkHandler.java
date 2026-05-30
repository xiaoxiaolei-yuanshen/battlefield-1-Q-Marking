package com.ea.bfaq.network;

import com.ea.bfaq.BF1Q;
import com.ea.bfaq.mark.MarkData;
import com.ea.bfaq.mark.MarkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class NetworkHandler
{
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            BF1Q.location("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register()
    {
        int id = 0;
        INSTANCE.registerMessage(id++, MarkPacket.class, MarkPacket::encode, MarkPacket::decode, MarkPacket::handle);
        INSTANCE.registerMessage(id++, TeammateSyncPacket.class, TeammateSyncPacket::encode, TeammateSyncPacket::decode, TeammateSyncPacket::handle);
        INSTANCE.registerMessage(id++, ClassSyncPacket.class, ClassSyncPacket::encode, ClassSyncPacket::decode, ClassSyncPacket::handle);
        INSTANCE.registerMessage(id++, ShowClassSelectionPacket.class, ShowClassSelectionPacket::encode, ShowClassSelectionPacket::decode, ShowClassSelectionPacket::handle);
        INSTANCE.registerMessage(id++, ClassSelectionResponsePacket.class, ClassSelectionResponsePacket::encode, ClassSelectionResponsePacket::decode, ClassSelectionResponsePacket::handle);
        INSTANCE.registerMessage(id++, KillSoundPacket.class, KillSoundPacket::encode, KillSoundPacket::decode, KillSoundPacket::handle);
        INSTANCE.registerMessage(id++, MarkSoundPacket.class, MarkSoundPacket::encode, MarkSoundPacket::decode, MarkSoundPacket::handle);
    }

    public static class MarkPacket
    {
        private final UUID targetUUID;
        private final int markTypeOrdinal;
        private final boolean isFriendly;

        public MarkPacket(UUID targetUUID, MarkData.MarkType markType, boolean isFriendly)
        {
            this.targetUUID = targetUUID;
            this.markTypeOrdinal = markType.ordinal();
            this.isFriendly = isFriendly;
        }

        public MarkPacket(FriendlyByteBuf buf)
        {
            this.targetUUID = buf.readUUID();
            this.markTypeOrdinal = buf.readInt();
            this.isFriendly = buf.readBoolean();
        }

        public void encode(FriendlyByteBuf buf)
        {
            buf.writeUUID(targetUUID);
            buf.writeInt(markTypeOrdinal);
            buf.writeBoolean(isFriendly);
        }

        public static MarkPacket decode(FriendlyByteBuf buf)
        {
            return new MarkPacket(buf);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx)
        {
            ctx.get().enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer sender = ctx.get().getSender();
                if (sender != null)
                {
                    MarkData.MarkType markType = MarkData.MarkType.values()[markTypeOrdinal];
                    
                    net.minecraft.world.scores.Team senderTeam = sender.getTeam();
                    if (senderTeam != null)
                    {
                        for (net.minecraft.server.level.ServerPlayer player : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers())
                        {
                            if (player.getTeam() != null && senderTeam.getName().equals(player.getTeam().getName()))
                            {
                                INSTANCE.send(
                                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                                    new MarkPacket(targetUUID, markType, isFriendly)
                                );
                            }
                        }
                    }
                    else
                    {
                        INSTANCE.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sender),
                            new MarkPacket(targetUUID, markType, isFriendly)
                        );
                    }
                }
                else
                {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                        MarkData.MarkType markType = MarkData.MarkType.values()[markTypeOrdinal];
                        
                        // 检查标记是否已经存在
                        if (!MarkManager.getInstance().hasActiveMark(targetUUID))
                        {
                            // 只添加标记，不触发UI显示，UI显示只在本地标记时触发
                            MarkManager.getInstance().addMark(targetUUID, markType, isFriendly);
                            
                            // 不播放音效，音效只在本地标记时播放
                            // 彩蛋音效通过MarkSoundPacket单独处理
                        }
                    });
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class TeammateSyncPacket
    {
        private final List<UUID> teammateUUIDs;

        public TeammateSyncPacket(List<UUID> teammateUUIDs)
        {
            this.teammateUUIDs = teammateUUIDs;
        }

        public TeammateSyncPacket(FriendlyByteBuf buf)
        {
            int count = buf.readInt();
            this.teammateUUIDs = new ArrayList<>();
            for (int i = 0; i < count; i++)
            {
                this.teammateUUIDs.add(buf.readUUID());
            }
        }

        public void encode(FriendlyByteBuf buf)
        {
            buf.writeInt(teammateUUIDs.size());
            for (UUID uuid : teammateUUIDs)
            {
                buf.writeUUID(uuid);
            }
        }

        public static TeammateSyncPacket decode(FriendlyByteBuf buf)
        {
            return new TeammateSyncPacket(buf);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx)
        {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    ClientTeammateManager.setTeammates(teammateUUIDs);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class ClientTeammateManager
    {
        private static final List<UUID> teammateUUIDs = new ArrayList<>();

        public static void setTeammates(List<UUID> newTeammates)
        {
            teammateUUIDs.clear();
            teammateUUIDs.addAll(newTeammates);
        }

        public static boolean isSelectedTeammate(UUID targetUUID)
        {
            return teammateUUIDs.contains(targetUUID);
        }

        public static boolean hasSelection()
        {
            return !teammateUUIDs.isEmpty();
        }

        public static List<UUID> getSelectedTeammates()
        {
            return new ArrayList<>(teammateUUIDs);
        }
    }

    public static class ClassSyncPacket
    {
        private final UUID playerUUID;
        private final String className;

        public ClassSyncPacket(UUID playerUUID, String className)
        {
            this.playerUUID = playerUUID;
            this.className = className;
        }

        public ClassSyncPacket(FriendlyByteBuf buf)
        {
            this.playerUUID = buf.readUUID();
            this.className = buf.readUtf();
        }

        public void encode(FriendlyByteBuf buf)
        {
            buf.writeUUID(playerUUID);
            buf.writeUtf(className);
        }

        public static ClassSyncPacket decode(FriendlyByteBuf buf)
        {
            return new ClassSyncPacket(buf);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx)
        {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    ClientClassManager.setPlayerClass(playerUUID, className);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class ClientClassManager
    {
        private static final Map<UUID, String> playerClasses = new ConcurrentHashMap<>();

        public static void setPlayerClass(UUID playerUUID, String className)
        {
            playerClasses.put(playerUUID, className);
        }

        public static String getPlayerClass(UUID playerUUID)
        {
            return playerClasses.get(playerUUID);
        }

        public static boolean hasClass(UUID playerUUID)
        {
            return playerClasses.containsKey(playerUUID);
        }

        public static void clearPlayerClass(UUID playerUUID)
        {
            playerClasses.remove(playerUUID);
        }
    }

    public static class ShowClassSelectionPacket
    {
        public ShowClassSelectionPacket()
        {
        }

        public ShowClassSelectionPacket(FriendlyByteBuf buf)
        {
        }

        public void encode(FriendlyByteBuf buf)
        {
        }

        public static ShowClassSelectionPacket decode(FriendlyByteBuf buf)
        {
            return new ShowClassSelectionPacket();
        }

        public void handle(Supplier<NetworkEvent.Context> ctx)
        {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    try {
                        Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
                        Object mcInstance = minecraftClass.getMethod("getInstance").invoke(null);
                        Class<?> screenClass = Class.forName("com.ea.bfaq.client.gui.ClassSelectionScreen");
                        Object screenInstance = screenClass.getConstructor().newInstance();
                        minecraftClass.getMethod("setScreen", Class.forName("net.minecraft.client.gui.screens.Screen")).invoke(mcInstance, screenInstance);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class ClassSelectionResponsePacket
    {
        private final String className;

        public ClassSelectionResponsePacket(String className)
        {
            this.className = className;
        }

        public ClassSelectionResponsePacket(FriendlyByteBuf buf)
        {
            this.className = buf.readUtf();
        }

        public void encode(FriendlyByteBuf buf)
        {
            buf.writeUtf(className);
        }

        public static ClassSelectionResponsePacket decode(FriendlyByteBuf buf)
        {
            return new ClassSelectionResponsePacket(buf);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx)
        {
            ctx.get().enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer player = ctx.get().getSender();
                if (player != null)
                {
                    com.ea.bfaq.events.PlayerEventHandler.setPlayerClassWithSave(player, className);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class KillSoundPacket
    {
        private final boolean isFriendly;

        public KillSoundPacket(boolean isFriendly)
        {
            this.isFriendly = isFriendly;
        }

        public KillSoundPacket(FriendlyByteBuf buf)
        {
            this.isFriendly = buf.readBoolean();
        }

        public void encode(FriendlyByteBuf buf)
        {
            buf.writeBoolean(isFriendly);
        }

        public static KillSoundPacket decode(FriendlyByteBuf buf)
        {
            return new KillSoundPacket(buf);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx)
        {
            ctx.get().enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer player = ctx.get().getSender();
                if (player != null)
                {
                    // 只发送给击杀者，不广播给其他玩家
                    INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new KillSoundPacket(isFriendly)
                    );
                }
                else
                {
                    // 客户端处理
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                        // 检查是否安装了GD656击杀图标模组
                        boolean hasGD656KillIconMod = net.minecraftforge.fml.ModList.get().isLoaded("gd656killicon");
                        if (!hasGD656KillIconMod)
                        {
                            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                            if (mc.player != null)
                            {
                                if (isFriendly)
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
                        }
                    });
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class MarkSoundPacket
    {
        private final String soundType;
        private final UUID senderUUID;

        public MarkSoundPacket(String soundType)
        {
            this.soundType = soundType;
            this.senderUUID = null;
        }

        public MarkSoundPacket(String soundType, UUID senderUUID)
        {
            this.soundType = soundType;
            this.senderUUID = senderUUID;
        }

        public MarkSoundPacket(FriendlyByteBuf buf)
        {
            this.soundType = buf.readUtf();
            if (buf.readBoolean())
            {
                this.senderUUID = buf.readUUID();
            }
            else
            {
                this.senderUUID = null;
            }
        }

        public void encode(FriendlyByteBuf buf)
        {
            buf.writeUtf(soundType);
            if (senderUUID != null)
            {
                buf.writeBoolean(true);
                buf.writeUUID(senderUUID);
            }
            else
            {
                buf.writeBoolean(false);
            }
        }

        public static MarkSoundPacket decode(FriendlyByteBuf buf)
        {
            return new MarkSoundPacket(buf);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx)
        {
            ctx.get().enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer player = ctx.get().getSender();
                if (player != null)
                {
                    UUID senderUUID = player.getUUID();
                    String finalSoundType = this.soundType;
                    net.minecraft.world.scores.Team senderTeam = player.getTeam();

                    for (net.minecraft.server.level.ServerPlayer serverPlayer : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers())
                    {
                        if (serverPlayer.getUUID().equals(senderUUID))
                        {
                            continue;
                        }

                        // 只转发给同小队的玩家
                        if (senderTeam == null || serverPlayer.getTeam() == null || !senderTeam.getName().equals(serverPlayer.getTeam().getName()))
                        {
                            continue;
                        }

                        // 只转发给标记者半径20格内的队友
                        double distSqr = serverPlayer.distanceToSqr(player);
                        if (distSqr > 20.0 * 20.0)
                        {
                            continue;
                        }

                        INSTANCE.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new MarkSoundPacket(finalSoundType, senderUUID)
                        );
                    }
                }
                else
                {
                    // 客户端处理 - 只处理从服务器转发的音效包（senderUUID不为null）
                    if (this.senderUUID != null)
                    {
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                            if (mc.player != null)
                            {
                                System.out.println("[BFQ-Network] 客户端收到转发音效包: soundType=" + soundType + ", senderUUID=" + senderUUID + ", playerUUID=" + mc.player.getUUID().toString());

                                if (this.senderUUID.equals(mc.player.getUUID()))
                                {
                                    System.out.println("[BFQ-Network] 跳过本地播放（自己发送的包）");
                                    return;
                                }

                                net.minecraft.sounds.SoundEvent soundEvent = null;
                                switch (soundType)
                                {
                                    case "mark.assault_easter":
                                        soundEvent = com.ea.bfaq.SoundEvents.ASSAULT_EASTER.get();
                                        break;
                                    case "mark.medic_easter":
                                        soundEvent = com.ea.bfaq.SoundEvents.MEDIC_EASTER.get();
                                        break;
                                    case "mark.support_easter":
                                        soundEvent = com.ea.bfaq.SoundEvents.SUPPORT_EASTER.get();
                                        break;
                                    case "mark.recon_easter":
                                        soundEvent = com.ea.bfaq.SoundEvents.RECON_EASTER.get();
                                        break;
                                    case "mark.bomber_easter":
                                        soundEvent = com.ea.bfaq.SoundEvents.BOMBER_EASTER.get();
                                        break;
                                    default:
                                        System.out.println("[BFQ-Network] 未知音效类型: " + soundType);
                                        break;
                                }

                                if (soundEvent != null)
                                {
                                    System.out.println("[BFQ-Network] 播放音效: " + soundEvent.getLocation().toString());
                                    mc.player.playSound(soundEvent, 1.0F, 1.0F);
                                }
                            }
                        });
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
