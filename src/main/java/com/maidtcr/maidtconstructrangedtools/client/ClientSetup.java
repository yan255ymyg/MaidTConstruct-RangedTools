package com.maidtcr.maidtconstructrangedtools.client;

import com.maidtcr.maidtconstructrangedtools.MaidTConstructRangedTools;
import com.maidtcr.maidtconstructrangedtools.entity.InitEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import slimeknights.tconstruct.tools.client.material.CombatFishingHookRenderer;

/** 客户端注册：女仆鱼钩直接复用匠魂的鱼钩渲染器（含材质贴图）。 */
@Mod.EventBusSubscriber(modid = MaidTConstructRangedTools.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(InitEntities.MAID_FISHING_HOOK.get(), CombatFishingHookRenderer::new);
    }
}
