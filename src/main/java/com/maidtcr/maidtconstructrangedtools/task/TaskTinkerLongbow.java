package com.maidtcr.maidtconstructrangedtools.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.maidtcr.maidtconstructrangedtools.MaidTCRConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeConfigSpec;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * 长弓工作模式。
 *
 * <p>蓄力时间由长弓的 {@code draw_speed} 决定，伤害/初速由匠魂工具属性决定。</p>
 *
 * <p><b>不要求背包里有箭</b>：弹药交给匠魂自己的 {@code BowAmmoModifierHook} 解析，
 * 所以装了能力强化「晶簇」等无限箭强化时，女仆会在没有箭的情况下消耗 4 点耐久射出水晶箭；
 * 只有在既没有强化也没有实体箭时才无法射击。</p>
 */
public class TaskTinkerLongbow extends AbstractTinkerRangedAttackTask {

    public TaskTinkerLongbow() {
        super("longbow", "longbow");
    }

    @Override
    protected ForgeConfigSpec.IntValue rangeConfig() {
        return MaidTCRConfig.LONGBOW_RANGE;
    }

    @Override
    public int getChargeTicks(EntityMaid maid) {
        if (!hasTool(maid)) {
            return 0;
        }
        return TinkerRangedHelper.longbowChargeTicks(ToolStack.from(maid.getMainHandItem()), maid);
    }

    @Override
    public int getCooldownTicks(EntityMaid maid) {
        return 5;
    }

    @Override
    public void performRangedAttack(EntityMaid maid, LivingEntity target, float velocity) {
        TinkerRangedHelper.fireLongbow(maid);
    }

    @Override
    public String getMaidActionSummary() {
        return "Use a Tinkers' Construct longbow to shoot arrows at entities";
    }
}
