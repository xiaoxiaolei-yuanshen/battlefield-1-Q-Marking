package com.ea.bfaq.network;

import com.ea.bfaq.BF1Q;
import com.ea.bfaq.mark.MarkData;
import com.ea.bfaq.mark.MarkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
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
                    net.minecraft.client.Minecraft.getInstance().setScreen(new com.ea.bfaq.client.gui.ClassSelectionScreen());
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
                    // 广播给所有玩家
                    for (net.minecraft.server.level.ServerPlayer serverPlayer : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers())
                    {
                        INSTANCE.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new KillSoundPacket(isFriendly)
                        );
                    }
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

        public MarkSoundPacket(String soundType)
        {
            this.soundType = soundType;
        }

        public MarkSoundPacket(FriendlyByteBuf buf)
        {
            this.soundType = buf.readUtf();
        }

        public void encode(FriendlyByteBuf buf)
        {
            buf.writeUtf(soundType);
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
                    // 广播给所有玩家
                    for (net.minecraft.server.level.ServerPlayer serverPlayer : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers())
                    {
                        INSTANCE.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new MarkSoundPacket(soundType)
                        );
                    }
                }
                else
                {
                    // 客户端处理
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc.player != null)
                        {
                            switch (soundType)
                            {
                                case "assault":
                                    // 只播放普通音效，不触发随机逻辑
                                    net.minecraft.sounds.SoundEvent[] assaultSounds = {
                                        com.ea.bfaq.SoundEvents.ASSAULT_A.get(),
                                        com.ea.bfaq.SoundEvents.ASSAULT_B.get(),
                                        com.ea.bfaq.SoundEvents.ASSAULT_C.get()
                                    };
                                    net.minecraft.sounds.SoundEvent assaultSound = assaultSounds[new java.util.Random().nextInt(assaultSounds.length)];
                                    mc.player.playSound(assaultSound, 10.0F, 1.0F);
                                    break;
                                case "medic":
                                    // 只播放普通音效，不触发随机逻辑
                                    net.minecraft.sounds.SoundEvent[] medicSounds = {
                                        com.ea.bfaq.SoundEvents.MEDIC_A.get(),
                                        com.ea.bfaq.SoundEvents.MEDIC_B.get(),
                                        com.ea.bfaq.SoundEvents.MEDIC_C.get()
                                    };
                                    net.minecraft.sounds.SoundEvent medicSound = medicSounds[new java.util.Random().nextInt(medicSounds.length)];
                                    mc.player.playSound(medicSound, 10.0F, 1.0F);
                                    break;
                                case "support":
                                    // 只播放普通音效，不触发随机逻辑
                                    net.minecraft.sounds.SoundEvent[] supportSounds = {
                                        com.ea.bfaq.SoundEvents.SUPPORT_A.get(),
                                        com.ea.bfaq.SoundEvents.SUPPORT_B.get(),
                                        com.ea.bfaq.SoundEvents.SUPPORT_C.get()
                                    };
                                    net.minecraft.sounds.SoundEvent supportSound = supportSounds[new java.util.Random().nextInt(supportSounds.length)];
                                    mc.player.playSound(supportSound, 10.0F, 1.0F);
                                    break;
                                case "recon":
                                    // 只播放普通音效，不触发随机逻辑
                                    net.minecraft.sounds.SoundEvent[] reconSounds = {
                                        com.ea.bfaq.SoundEvents.RECON_A.get(),
                                        com.ea.bfaq.SoundEvents.RECON_B.get(),
                                        com.ea.bfaq.SoundEvents.RECON_C.get()
                                    };
                                    net.minecraft.sounds.SoundEvent reconSound = reconSounds[new java.util.Random().nextInt(reconSounds.length)];
                                    mc.player.playSound(reconSound, 10.0F, 1.0F);
                                    break;
                                case "raider":
                                    // 只播放普通音效，不触发随机逻辑
                                    net.minecraft.sounds.SoundEvent[] raiderSounds = {
                                        com.ea.bfaq.SoundEvents.RAIDER_A.get(),
                                        com.ea.bfaq.SoundEvents.RAIDER_B.get(),
                                        com.ea.bfaq.SoundEvents.RAIDER_C.get()
                                    };
                                    net.minecraft.sounds.SoundEvent raiderSound = raiderSounds[new java.util.Random().nextInt(raiderSounds.length)];
                                    mc.player.playSound(raiderSound, 10.0F, 1.0F);
                                    break;
                                case "flamethrower":
                                    // 只播放普通音效，不触发随机逻辑
                                    net.minecraft.sounds.SoundEvent[] flamethrowerSounds = {
                                        com.ea.bfaq.SoundEvents.FLAMETHROWER_A.get(),
                                        com.ea.bfaq.SoundEvents.FLAMETHROWER_B.get(),
                                        com.ea.bfaq.SoundEvents.FLAMETHROWER_C.get()
                                    };
                                    net.minecraft.sounds.SoundEvent flamethrowerSound = flamethrowerSounds[new java.util.Random().nextInt(flamethrowerSounds.length)];
                                    mc.player.playSound(flamethrowerSound, 10.0F, 1.0F);
                                    break;
                                case "sentry":
                                    // 只播放普通音效，不触发随机逻辑
                                    net.minecraft.sounds.SoundEvent[] sentrySounds = {
                                        com.ea.bfaq.SoundEvents.SENTRY_A.get(),
                                        com.ea.bfaq.SoundEvents.SENTRY_B.get(),
                                        com.ea.bfaq.SoundEvents.SENTRY_C.get()
                                    };
                                    net.minecraft.sounds.SoundEvent sentrySound = sentrySounds[new java.util.Random().nextInt(sentrySounds.length)];
                                    mc.player.playSound(sentrySound, 10.0F, 1.0F);
                                    break;
                                case "pilot":
                                    // 只播放普通音效，不触发随机逻辑
                                    net.minecraft.sounds.SoundEvent[] planeSounds = {
                                        com.ea.bfaq.SoundEvents.PLANE_A.get(),
                                        com.ea.bfaq.SoundEvents.PLANE_B.get(),
                                        com.ea.bfaq.SoundEvents.PLANE_C.get()
                                    };
                                    net.minecraft.sounds.SoundEvent planeSound = planeSounds[new java.util.Random().nextInt(planeSounds.length)];
                                    mc.player.playSound(planeSound, 10.0F, 1.0F);
                                    break;
                                case "bomber":
                                    // 只播放普通音效，不触发随机逻辑
                                    net.minecraft.sounds.SoundEvent[] bomberSounds = {
                                        com.ea.bfaq.SoundEvents.BOMBER_A.get(),
                                        com.ea.bfaq.SoundEvents.BOMBER_B.get(),
                                        com.ea.bfaq.SoundEvents.BOMBER_C.get()
                                    };
                                    net.minecraft.sounds.SoundEvent bomberSound = bomberSounds[new java.util.Random().nextInt(bomberSounds.length)];
                                    mc.player.playSound(bomberSound, 10.0F, 1.0F);
                                    break;
                                case "medic_easter":
                                    // 只播放彩蛋音效，不触发随机逻辑
                                    mc.player.playSound(
                                        com.ea.bfaq.SoundEvents.MEDIC_EASTER.get(),
                                        10.0F, 1.0F
                                    );
                                    break;
                                case "support_easter":
                                    // 只播放彩蛋音效，不触发随机逻辑
                                    mc.player.playSound(
                                        com.ea.bfaq.SoundEvents.SUPPORT_EASTER.get(),
                                        10.0F, 1.0F
                                    );
                                    break;
                                case "trench_fighter":
                                    // 只播放普通音效，不触发随机逻辑
                                    mc.player.playSound(
                                        com.ea.bfaq.SoundEvents.TRENCH_FIGHTER.get(),
                                        10.0F, 1.0F
                                    );
                                    break;
                                case "invader":
                                    // 只播放普通音效，不触发随机逻辑
                                    mc.player.playSound(
                                        com.ea.bfaq.SoundEvents.INVADER.get(),
                                        10.0F, 1.0F
                                    );
                                    break;
                            }
                        }
                    });
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    @Mod.EventBusSubscriber(modid = BF1Q.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Registration
    {
        @SubscribeEvent
        public static void onCommonSetup(FMLCommonSetupEvent event)
        {
            NetworkHandler.register();
        }
    }
}
