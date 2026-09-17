package com.maidtcr.maidtconstructrangedtools.client;

import com.github.tartaricacid.touhoulittlemaid.api.event.client.AddClothConfigEvent;
import com.maidtcr.maidtconstructrangedtools.MaidTCRConfig;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 在女仆的全局设置界面（Cloth Config）里追加一个「女仆匠魂：远程工具」分类。
 *
 * <p>女仆模组会在 {@code Compat.cloth.MenuIntegration} 里向 {@code MinecraftForge.EVENT_BUS}
 * 投递 {@link AddClothConfigEvent}，本模组接住它追加自己的分类。</p>
 *
 * <p>注意：Cloth Config 是可选依赖，只有在它已加载时才会注册本监听器；
 * 另外所有 Cloth Config 相关的类型都只出现在方法体/私有方法里，
 * 不出现在公开方法签名上，避免 Forge 事件总线反射扫描时触发类加载失败。</p>
 */
public final class MaidClothConfig {

    private static final String KEY = "config.maidtconstructrangedtools.ranged_tools.";

    private MaidClothConfig() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(MaidClothConfig.class);
    }

    @SubscribeEvent
    public static void onAddClothConfig(AddClothConfigEvent event) {
        Object builder = event.getRoot();
        Object entries = event.getEntryBuilder();
        addCategory(builder, entries);
    }

    /** 只有私有方法才允许出现 Cloth Config 类型（见类注释）。 */
    private static void addCategory(Object rootObject, Object entriesObject) {
        me.shedaniel.clothconfig2.api.ConfigBuilder root =
                (me.shedaniel.clothconfig2.api.ConfigBuilder) rootObject;
        me.shedaniel.clothconfig2.api.ConfigEntryBuilder entries =
                (me.shedaniel.clothconfig2.api.ConfigEntryBuilder) entriesObject;

        me.shedaniel.clothconfig2.api.ConfigCategory category =
                root.getOrCreateCategory(Component.translatable(KEY + "title"));

        addRange(entries, category, "crossbow", MaidTCRConfig.CROSSBOW_RANGE, 64);
        addRange(entries, category, "fishing_rod", MaidTCRConfig.FISHING_ROD_RANGE, 48);
        addRange(entries, category, "javelin", MaidTCRConfig.JAVELIN_RANGE, 48);
        addRange(entries, category, "longbow", MaidTCRConfig.LONGBOW_RANGE, 48);
        addRange(entries, category, "shuriken", MaidTCRConfig.SHURIKEN_RANGE, 48);
        addRange(entries, category, "throwing_axe", MaidTCRConfig.THROWING_AXE_RANGE, 16);
        addRange(entries, category, "swasher", MaidTCRConfig.SWASHER_RANGE, 48);

        category.addEntry(entries
                .startBooleanToggle(Component.translatable(KEY + "swasher_auto_refill"),
                        MaidTCRConfig.SWASHER_AUTO_REFILL.get())
                .setDefaultValue(true)
                .setTooltip(Component.translatable(KEY + "swasher_auto_refill.tooltip"))
                .setSaveConsumer(value -> save(MaidTCRConfig.SWASHER_AUTO_REFILL, value))
                .build());
    }

    private static void addRange(me.shedaniel.clothconfig2.api.ConfigEntryBuilder entries,
                                 me.shedaniel.clothconfig2.api.ConfigCategory category,
                                 String name, ForgeConfigSpec.IntValue value, int defaultValue) {
        category.addEntry(entries
                .startIntSlider(Component.translatable(KEY + name + "_range"),
                        value.get(), MaidTCRConfig.RANGE_MIN, MaidTCRConfig.RANGE_MAX)
                .setDefaultValue(defaultValue)
                .setTooltip(Component.translatable(KEY + name + "_range.tooltip"))
                .setSaveConsumer(newValue -> save(value, newValue))
                .build());
    }

    private static void save(ForgeConfigSpec.ConfigValue<?> value, Object newValue) {
        setValue(value, newValue);
        MaidTCRConfig.SPEC.save();
    }

    @SuppressWarnings("unchecked")
    private static <T> void setValue(ForgeConfigSpec.ConfigValue<?> value, Object newValue) {
        ((ForgeConfigSpec.ConfigValue<T>) value).set((T) newValue);
    }
}
