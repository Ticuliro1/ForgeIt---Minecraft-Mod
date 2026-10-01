package com.ticuliro.forgeit.client;

import com.ticuliro.forgeit.ForgeIt;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = ForgeIt.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ForgeItClient {
    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(ForgeIt.REFORGING_MENU.get(), ReforgingScreen::new);
    }
}
