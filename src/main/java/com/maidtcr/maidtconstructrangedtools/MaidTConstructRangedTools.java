package com.maidtcr.maidtconstructrangedtools;

import com.maidtcr.maidtconstructrangedtools.client.MaidClothConfig;
import com.maidtcr.maidtconstructrangedtools.compat.TinkerFishingType;
import com.maidtcr.maidtconstructrangedtools.entity.InitEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Maid TConstruct: Ranged Tools
 *
 * <p>为车万女仆（Touhou Little Maid）添加匠魂 3（Tinkers' Construct 3）远程工具的工作模式：
 * 弩、钓鱼竿（当前禁用）、标枪、长弓、手里剑、投斧、喷流刃。
 * 同时把匠魂钓鱼竿接入女仆原有的「钓鱼」工作模式。</p>
 */
@Mod(MaidTConstructRangedTools.MOD_ID)
public class MaidTConstructRangedTools {

    public static final String MOD_ID = "maidtconstructrangedtools";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MaidTConstructRangedTools() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        InitEntities.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MaidTCRConfig.SPEC);

        // 女仆全局设置（Cloth Config）里追加「女仆匠魂：远程工具」分类。
        // Cloth Config 是可选依赖，因此只在客户端且装了它时才注册。
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (ModList.get().isLoaded("cloth_config")) {
                MaidClothConfig.init();
            }
        });
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(TinkerFishingType::register);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
