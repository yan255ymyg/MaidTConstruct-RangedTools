package com.maidtcr.maidtconstructrangedtools;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 通用配置。
 *
 * <p>射程项与「女仆三叉戟范围」等一样，会出现在女仆全局设置界面的
 * 「女仆匠魂：远程工具」分类里（见 {@code MaidClothConfig}）。</p>
 */
public final class MaidTCRConfig {

    public static final ForgeConfigSpec SPEC;

    /** 射程滑条的取值范围（与女仆模组自带远程范围的 8~192 保持一致）。 */
    public static final int RANGE_MIN = 8;
    public static final int RANGE_MAX = 192;

    /* 弹药消耗 */

    public static final ForgeConfigSpec.BooleanValue CONSUME_ARROW;
    public static final ForgeConfigSpec.BooleanValue CONSUME_SHURIKEN;
    public static final ForgeConfigSpec.BooleanValue CONSUME_THROWING_AXE;
    public static final ForgeConfigSpec.BooleanValue CONSUME_JAVELIN;

    /* 各工作模式射程 */

    public static final ForgeConfigSpec.IntValue CROSSBOW_RANGE;
    public static final ForgeConfigSpec.IntValue FISHING_ROD_RANGE;
    public static final ForgeConfigSpec.IntValue JAVELIN_RANGE;
    public static final ForgeConfigSpec.IntValue LONGBOW_RANGE;
    public static final ForgeConfigSpec.IntValue SHURIKEN_RANGE;
    public static final ForgeConfigSpec.IntValue THROWING_AXE_RANGE;
    public static final ForgeConfigSpec.IntValue SWASHER_RANGE;

    /* 喷流刃 */

    public static final ForgeConfigSpec.BooleanValue SWASHER_AUTO_REFILL;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Maid TConstruct: Ranged Tools / 女仆匠魂：远程工具").push("ranged_tools");

        builder.comment("Ammo consumption / 弹药消耗").push("consume");
        CONSUME_ARROW = builder
                .comment("长弓/弩射击时是否消耗女仆背包内的箭。",
                        "关闭后女仆会无限借箭（不影响匠魂「晶簇」等强化：那些强化由匠魂自己按 4 点/支扣耐久）。")
                .define("consume_arrow", true);
        CONSUME_SHURIKEN = builder
                .comment("手里剑工作模式投掷时是否消耗手里剑（每次消耗 1 个）。")
                .define("consume_shuriken", true);
        CONSUME_THROWING_AXE = builder
                .comment("投斧工作模式投掷时是否消耗投斧（每次消耗 1 个）。")
                .define("consume_throwing_axe", true);
        CONSUME_JAVELIN = builder
                .comment("标枪工作模式投掷时是否消耗标枪。默认关闭，",
                        "否则女仆每投一次就没武器可用（匠魂原版对玩家是消耗的）；",
                        "无论是否消耗，每次投掷都会损耗 1 点耐久。")
                .define("consume_javelin", false);
        builder.pop();

        builder.comment("Attack range of each work mode / 各工作模式射程").push("range");
        CROSSBOW_RANGE = range(builder, "crossbow", 64);
        FISHING_ROD_RANGE = range(builder, "fishing_rod", 48);
        JAVELIN_RANGE = range(builder, "javelin", 48);
        LONGBOW_RANGE = range(builder, "longbow", 48);
        SHURIKEN_RANGE = range(builder, "shuriken", 48);
        // 投斧的弹射物初速很低（匠魂基础 velocity 只有 0.75），抬到极限也只能打到十几格，
        // 所以默认射程给得比其它投掷武器短，免得女仆对着打不到的目标一直扔。
        THROWING_AXE_RANGE = range(builder, "throwing_axe", 16);
        SWASHER_RANGE = range(builder, "swasher", 48);
        builder.pop();

        builder.comment("Fluid swasher / 喷流刃").push("swasher");
        SWASHER_AUTO_REFILL = builder
                .comment("喷流刃内没有流体时，女仆是否自动用背包里的流体容器（匠魂的罐、灯笼、量器等）给它补充流体。")
                .define("auto_refill", true);
        builder.pop();

        builder.pop();
        SPEC = builder.build();
    }

    private static ForgeConfigSpec.IntValue range(ForgeConfigSpec.Builder builder, String name, int defaultValue) {
        return builder
                .comment("Range of the " + name + " work mode")
                .defineInRange(name + "_range", defaultValue, RANGE_MIN, RANGE_MAX);
    }

    private MaidTCRConfig() {
    }

    /** 配置在极端情况下（例如尚未加载）读取失败时回退到默认值，避免 AI tick 抛异常。 */
    public static boolean get(ForgeConfigSpec.BooleanValue value, boolean fallback) {
        try {
            return value.get();
        } catch (Exception e) {
            return fallback;
        }
    }

    /** 同上，用于射程。 */
    public static float getRange(ForgeConfigSpec.IntValue value, float fallback) {
        try {
            return value.get();
        } catch (Exception e) {
            return fallback;
        }
    }
}
