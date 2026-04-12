package com.ea.bfaq.commands;

import com.ea.bfaq.BF1Q;
import com.ea.bfaq.network.NetworkHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class XDCommand
{
    private static final List<String> TEAM_NAMES = Arrays.asList(
        "pg", "ny", "cl", "df", "ed", 
        "fd", "gq", "hl", "mq", "gw", "ld"
    );
    
    private static final Map<String, String> TEAM_DISPLAY_NAMES = new HashMap<>() {{
        put("pg", "苹果");
        put("ny", "奶油");
        put("cl", "查理");
        put("df", "达夫");
        put("ed", "爱德华");
        put("fd", "弗莱迪");
        put("gq", "乔治");
        put("hl", "哈利");
        put("mq", "墨水强尼");
        put("gw", "国王");
        put("ld", "伦敦");
    }};
    
    private static final int MAX_TEAM_SIZE = 5;
    
    private static String getTeamDisplayName(String teamName)
    {
        return TEAM_DISPLAY_NAMES.getOrDefault(teamName, teamName);
    }
    
    private static final Map<String, Map<String, TeamData>> scoreboardTeamToXDTeams = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerToXDTeam = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerToScoreboardTeam = new ConcurrentHashMap<>();
    
    private static class TeamData
    {
        private final String name;
        private final String scoreboardTeam;
        private final List<UUID> members = new ArrayList<>();
        private final UUID creator;
        
        public TeamData(String name, String scoreboardTeam, UUID creator)
        {
            this.name = name;
            this.scoreboardTeam = scoreboardTeam;
            this.creator = creator;
            this.members.add(creator);
        }
        
        public String getName()
        {
            return name;
        }
        
        public String getScoreboardTeam()
        {
            return scoreboardTeam;
        }
        
        public List<UUID> getMembers()
        {
            return members;
        }
        
        public UUID getCreator()
        {
            return creator;
        }
        
        public boolean isFull()
        {
            return members.size() >= MAX_TEAM_SIZE;
        }
        
        public boolean addMember(UUID playerUUID)
        {
            if (isFull() || members.contains(playerUUID))
            {
                return false;
            }
            members.add(playerUUID);
            return true;
        }
        
        public boolean removeMember(UUID playerUUID)
        {
            return members.remove(playerUUID);
        }
        
        public int size()
        {
            return members.size();
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
            Commands.literal("xd")
                .requires(source -> source.hasPermission(0))
                .then(Commands.literal("add")
                    .executes(XDCommand::addTeam)
                )
                .then(Commands.literal("join")
                    .then(Commands.argument("teamName", StringArgumentType.word())
                        .suggests(XDCommand::suggestAvailableTeams)
                        .executes(XDCommand::joinTeam)
                    )
                )
                .then(Commands.literal("leave")
                    .executes(XDCommand::leaveTeam)
                )
                .then(Commands.literal("list")
                    .executes(XDCommand::listTeams)
                )
        );
    }
    
    private static String getScoreboardTeamName(ServerPlayer player)
    {
        Team team = player.getTeam();
        return team != null ? team.getName() : null;
    }
    
    private static boolean isInScoreboardTeam(ServerPlayer player)
    {
        return getScoreboardTeamName(player) != null;
    }
    
    private static CompletableFuture<Suggestions> suggestAvailableTeams(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder)
    {
        List<String> availableTeams = new ArrayList<>();
        if (!(context.getSource().getEntity() instanceof ServerPlayer player))
        {
            return SharedSuggestionProvider.suggest(availableTeams, builder);
        }
        
        String scoreboardTeamName = getScoreboardTeamName(player);
        if (scoreboardTeamName == null)
        {
            return SharedSuggestionProvider.suggest(availableTeams, builder);
        }
        
        Map<String, TeamData> xdTeams = scoreboardTeamToXDTeams.get(scoreboardTeamName);
        if (xdTeams == null)
        {
            return SharedSuggestionProvider.suggest(availableTeams, builder);
        }
        
        for (Map.Entry<String, TeamData> entry : xdTeams.entrySet())
        {
            if (!entry.getValue().isFull())
            {
                availableTeams.add(entry.getValue().getName());
            }
        }
        
        return SharedSuggestionProvider.suggest(availableTeams, builder);
    }

    private static int addTeam(CommandContext<CommandSourceStack> context)
    {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player))
        {
            source.sendFailure(Component.literal("只有玩家可以使用此命令"));
            return 1;
        }
        
        String scoreboardTeamName = getScoreboardTeamName(player);
        if (scoreboardTeamName == null)
        {
            source.sendFailure(Component.literal("你需要先加入一个Minecraft队伍才能使用此命令"));
            return 1;
        }
        
        if (playerToXDTeam.containsKey(player.getUUID()))
        {
            source.sendFailure(Component.literal("你已经在一个小队中了"));
            return 1;
        }
        
        Map<String, TeamData> xdTeams = scoreboardTeamToXDTeams.computeIfAbsent(scoreboardTeamName, k -> new ConcurrentHashMap<>());
        
        for (String teamName : TEAM_NAMES)
        {
            if (!xdTeams.containsKey(teamName))
            {
                TeamData team = new TeamData(teamName, scoreboardTeamName, player.getUUID());
                xdTeams.put(teamName, team);
                playerToXDTeam.put(player.getUUID(), teamName);
                playerToScoreboardTeam.put(player.getUUID(), scoreboardTeamName);
                syncToClient(player);
                source.sendSuccess(() -> Component.literal("已创建小队：" + getTeamDisplayName(teamName) + " (队伍：" + scoreboardTeamName + ")"), false);
                return 1;
            }
        }
        
        source.sendFailure(Component.literal("所有小队都已被创建"));
        return 1;
    }

    private static int joinTeam(CommandContext<CommandSourceStack> context)
    {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player))
        {
            source.sendFailure(Component.literal("只有玩家可以使用此命令"));
            return 1;
        }
        
        String scoreboardTeamName = getScoreboardTeamName(player);
        if (scoreboardTeamName == null)
        {
            source.sendFailure(Component.literal("你需要先加入一个Minecraft队伍才能使用此命令"));
            return 1;
        }
        
        if (playerToXDTeam.containsKey(player.getUUID()))
        {
            source.sendFailure(Component.literal("你已经在一个小队中了"));
            return 1;
        }
        
        String teamName = StringArgumentType.getString(context, "teamName");
        Map<String, TeamData> xdTeams = scoreboardTeamToXDTeams.get(scoreboardTeamName);
        
        if (xdTeams == null)
        {
            source.sendFailure(Component.literal("该队伍没有可加入的小队"));
            return 1;
        }
        
        TeamData team = xdTeams.get(teamName);
        if (team == null)
        {
            source.sendFailure(Component.literal("小队不存在：" + getTeamDisplayName(teamName)));
            return 1;
        }
        
        if (team.isFull())
        {
            source.sendFailure(Component.literal("小队已满：" + getTeamDisplayName(teamName)));
            return 1;
        }
        
        if (team.addMember(player.getUUID()))
        {
            playerToXDTeam.put(player.getUUID(), teamName);
            playerToScoreboardTeam.put(player.getUUID(), scoreboardTeamName);
            syncToClient(player);
            source.sendSuccess(() -> Component.literal("已加入小队：" + getTeamDisplayName(teamName) + " (" + team.size() + "/" + MAX_TEAM_SIZE + ")"), false);
        }
        else
        {
            source.sendFailure(Component.literal("加入小队失败"));
        }
        return 1;
    }

    private static int leaveTeam(CommandContext<CommandSourceStack> context)
    {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player))
        {
            source.sendFailure(Component.literal("只有玩家可以使用此命令"));
            return 1;
        }
        
        String teamName = playerToXDTeam.get(player.getUUID());
        String displayName = getTeamDisplayName(teamName);
        if (teamName == null)
        {
            source.sendFailure(Component.literal("你不在任何小队中"));
            return 1;
        }
        
        String scoreboardTeamName = playerToScoreboardTeam.get(player.getUUID());
        Map<String, TeamData> xdTeams = scoreboardTeamToXDTeams.get(scoreboardTeamName);
        TeamData team = null;
        
        if (xdTeams != null)
        {
            team = xdTeams.get(teamName);
            if (team != null)
            {
                team.removeMember(player.getUUID());
                if (team.size() == 0)
                {
                    xdTeams.remove(teamName);
                    if (xdTeams.isEmpty())
                    {
                        scoreboardTeamToXDTeams.remove(scoreboardTeamName);
                    }
                }
            }
        }
        
        playerToXDTeam.remove(player.getUUID());
        playerToScoreboardTeam.remove(player.getUUID());
        
        List<UUID> emptyTeammates = new ArrayList<>();
        NetworkHandler.INSTANCE.send(
            PacketDistributor.PLAYER.with(() -> player),
            new NetworkHandler.TeammateSyncPacket(emptyTeammates)
        );
        
        if (team != null && team.size() > 0)
        {
            syncToAllTeamMembers(team);
        }
        
        source.sendSuccess(() -> Component.literal("已离开小队：" + displayName), false);
        return 1;
    }

    private static int listTeams(CommandContext<CommandSourceStack> context)
    {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player))
        {
            source.sendFailure(Component.literal("只有玩家可以使用此命令"));
            return 1;
        }
        
        String scoreboardTeamName = getScoreboardTeamName(player);
        if (scoreboardTeamName == null)
        {
            source.sendFailure(Component.literal("你需要先加入一个Minecraft队伍才能使用此命令"));
            return 1;
        }
        
        Map<String, TeamData> xdTeams = scoreboardTeamToXDTeams.get(scoreboardTeamName);
        
        if (xdTeams == null || xdTeams.isEmpty())
        {
            source.sendSuccess(() -> Component.literal("当前没有小队"), false);
            return 1;
        }
        
        source.sendSuccess(() -> Component.literal("当前小队列表（队伍：" + scoreboardTeamName + "）："), false);
        for (Map.Entry<String, TeamData> entry : xdTeams.entrySet())
        {
            TeamData team = entry.getValue();
            String status = team.isFull() ? "(已满)" : "(" + team.size() + "/" + MAX_TEAM_SIZE + ")";
            source.sendSuccess(() -> Component.literal("- " + getTeamDisplayName(team.getName()) + " " + status), false);
        }
        return 1;
    }
    
    private static void syncToClient(ServerPlayer player)
    {
        String scoreboardTeamName = playerToScoreboardTeam.get(player.getUUID());
        String xdTeamName = playerToXDTeam.get(player.getUUID());
        
        if (scoreboardTeamName != null && xdTeamName != null)
        {
            Map<String, TeamData> xdTeams = scoreboardTeamToXDTeams.get(scoreboardTeamName);
            if (xdTeams != null)
            {
                TeamData team = xdTeams.get(xdTeamName);
                if (team != null)
                {
                    syncToAllTeamMembers(team);
                }
            }
        }
        else
        {
            List<UUID> teammates = new ArrayList<>();
            NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new NetworkHandler.TeammateSyncPacket(teammates)
            );
        }
    }
    
    private static void syncToAllTeamMembers(TeamData team)
    {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        
        for (UUID memberUUID : team.getMembers())
        {
            ServerPlayer member = server.getPlayerList().getPlayer(memberUUID);
            if (member != null)
            {
                List<UUID> teammates = new ArrayList<>();
                for (UUID otherMember : team.getMembers())
                {
                    if (!otherMember.equals(memberUUID))
                    {
                        teammates.add(otherMember);
                    }
                }
                NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> member),
                    new NetworkHandler.TeammateSyncPacket(teammates)
                );
            }
        }
    }

    public static boolean isSelectedTeammate(UUID playerUUID, UUID targetUUID)
    {
        String playerScoreboardTeam = playerToScoreboardTeam.get(playerUUID);
        String targetScoreboardTeam = playerToScoreboardTeam.get(targetUUID);
        
        if (playerScoreboardTeam == null || targetScoreboardTeam == null)
        {
            return false;
        }
        
        if (!playerScoreboardTeam.equals(targetScoreboardTeam))
        {
            return false;
        }
        
        String xdTeamName = playerToXDTeam.get(playerUUID);
        if (xdTeamName == null)
        {
            return false;
        }
        
        Map<String, TeamData> xdTeams = scoreboardTeamToXDTeams.get(playerScoreboardTeam);
        if (xdTeams == null)
        {
            return false;
        }
        
        TeamData team = xdTeams.get(xdTeamName);
        if (team == null)
        {
            return false;
        }
        
        return team.getMembers().contains(targetUUID);
    }

    public static List<UUID> getSelectedTeammates(UUID playerUUID)
    {
        String scoreboardTeamName = playerToScoreboardTeam.get(playerUUID);
        String xdTeamName = playerToXDTeam.get(playerUUID);
        
        if (scoreboardTeamName == null || xdTeamName == null)
        {
            return new ArrayList<>();
        }
        
        Map<String, TeamData> xdTeams = scoreboardTeamToXDTeams.get(scoreboardTeamName);
        if (xdTeams == null)
        {
            return new ArrayList<>();
        }
        
        TeamData team = xdTeams.get(xdTeamName);
        if (team == null)
        {
            return new ArrayList<>();
        }
        
        List<UUID> teammates = new ArrayList<>();
        for (UUID member : team.getMembers())
        {
            if (!member.equals(playerUUID))
            {
                teammates.add(member);
            }
        }
        return teammates;
    }

    public static boolean hasSelection(UUID playerUUID)
    {
        return playerToXDTeam.containsKey(playerUUID);
    }
}
