package com.ea.bfaq;

// 关注b站UID:545778318谢谢喵
// 关注b站UID:1157669161谢谢喵

import com.ea.bfaq.client.KeyBindings;
import com.ea.bfaq.commands.XDCommand;
import com.ea.bfaq.commands.ZYCommand;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(BF1Q.MODID)
public class BF1Q
{
    public static final String MODID = "bfq";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation location(String path)
    {
        return new ResourceLocation(MODID, path);
    }

    public BF1Q()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        SoundEvents.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("Battlefield 1 Q mod loaded");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Battlefield 1 Q mod server starting");
    }
    
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event)
    {
        XDCommand.register(event.getDispatcher());
        ZYCommand.register(event.getDispatcher());
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            LOGGER.info("Battlefield 1 Q client setup");
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
        {
            event.register(KeyBindings.MARK_ENEMY);
        }
    }
}
