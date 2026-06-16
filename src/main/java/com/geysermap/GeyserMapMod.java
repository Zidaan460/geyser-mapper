package com.geysermap;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;

@Mod("geysermap")
public class GeyserMapMod {

    public static final Logger LOGGER = LogUtils.getLogger();

    public GeyserMapMod(IEventBus modEventBus) {
        LOGGER.info("GeyserMap loaded!");
    }

    @EventBusSubscriber(modid = "geysermap", bus = EventBusSubscriber.Bus.GAME)
    public static class GameEvents {
        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            GeyserMapCommand.register(event.getDispatcher());
        }
    }
}
