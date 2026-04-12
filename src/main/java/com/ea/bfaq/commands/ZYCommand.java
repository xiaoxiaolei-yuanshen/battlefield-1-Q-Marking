package com.ea.bfaq.commands;

import com.ea.bfaq.BF1Q;
import com.ea.bfaq.events.PlayerEventHandler;
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
import net.minecraftforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ZYCommand
{
    private static final List<String> CLASS_NAMES = Arrays.asList(
        "assault", "medic", "recon", "support"
    );
    
    private static final Map<UUID, String> playerClasses = new ConcurrentHashMap<>();
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
            Commands.literal("zy")
                .requires(source -> source.hasPermission(0))
                .then(Commands.argument("className", StringArgumentType.word())
                    .suggests(ZYCommand::suggestClasses)
                    .executes(ZYCommand::setClass)
                )
        );
    }
    
    private static CompletableFuture<Suggestions> suggestClasses(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder)
    {
        return SharedSuggestionProvider.suggest(CLASS_NAMES, builder);
    }
    
    private static int setClass(CommandContext<CommandSourceStack> context)
    {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player))
        {
            source.sendFailure(Component.literal("只有玩家可以使用此命令"));
            return 1;
        }
        
        String className = StringArgumentType.getString(context, "className");
        
        if (!CLASS_NAMES.contains(className))
        {
            source.sendFailure(Component.literal("无效的职业：" + className));
            return 1;
        }
        
        PlayerEventHandler.setPlayerClassWithSave(player, className);
        source.sendSuccess(() -> Component.literal("已设置职业：" + getClassDisplayName(className)), false);
        
        return 1;
    }
    
    private static String getClassDisplayName(String className)
    {
        return switch (className) {
            case "assault" -> "突击兵";
            case "medic" -> "医疗兵";
            case "recon" -> "侦察兵";
            case "support" -> "支援兵";
            default -> className;
        };
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
    
    public static String getPlayerClass(UUID playerUUID)
    {
        return playerClasses.get(playerUUID);
    }
    
    public static boolean hasClass(UUID playerUUID)
    {
        return playerClasses.containsKey(playerUUID);
    }
    
    public static Map<UUID, String> getAllPlayerClasses()
    {
        return new HashMap<>(playerClasses);
    }
    
    public static void setPlayerClass(UUID playerUUID, String className)
    {
        playerClasses.put(playerUUID, className);
    }
}
