package com.maidtcr.maidtconstructrangedtools.entity;

import com.maidtcr.maidtconstructrangedtools.MaidTConstructRangedTools;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 本模组注册的实体。 */
public final class InitEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MaidTConstructRangedTools.MOD_ID);

    /**
     * 女仆用的匠魂鱼钩。
     *
     * <p>注册成独立实体类型的原因：客户端需要按自己的类反序列化（这样才能走
     * {@link MaidCombatFishingHook#getPlayerOwner()} 的回退逻辑，否则鱼钩在客户端会被原版逻辑丢弃）。</p>
     */
    public static final RegistryObject<EntityType<MaidCombatFishingHook>> MAID_FISHING_HOOK =
            ENTITY_TYPES.register("maid_fishing_hook", () -> EntityType.Builder
                    .<MaidCombatFishingHook>of(MaidCombatFishingHook::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(5)
                    .build(MaidTConstructRangedTools.id("maid_fishing_hook").toString()));

    private InitEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}
