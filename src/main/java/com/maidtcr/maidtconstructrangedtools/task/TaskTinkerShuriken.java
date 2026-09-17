package com.maidtcr.maidtconstructrangedtools.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.maidtcr.maidtconstructrangedtools.MaidTCRConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 手里剑工作模式。
 *
 * <p>手里剑是消耗品，所以<b>不要求主手拿着</b>，只要求女仆背包里有：
 * 有敌人时她会自动把背包里的手里剑换到主手（与农场模式的思路一致），
 * 之后每次投掷消耗 1 个。</p>
 *
 * <p>投掷角度会依据距离略微抬高，用来抵消弹射物下坠、提高命中率。</p>
 */
public class TaskTinkerShuriken extends AbstractTinkerRangedAttackTask {

    public TaskTinkerShuriken() {
        super("shuriken", "shuriken");
    }

    @Override
    protected ForgeConfigSpec.IntValue rangeConfig() {
        return MaidTCRConfig.SHURIKEN_RANGE;
    }

    @Override
    protected boolean enableBackpackEquip() {
        return true;
    }

    @Override
    protected String conditionKey() {
        return "has_tool_in_backpack";
    }

    @Override
    protected boolean conditionPredicate(EntityMaid maid) {
        return hasWeaponInBackpack(maid);
    }

    @Override
    public int getCooldownTicks(EntityMaid maid) {
        return 10;
    }

    @Override
    public void performRangedAttack(EntityMaid maid, LivingEntity target, float velocity) {
        TinkerRangedHelper.throwShuriken(maid, target, MaidTCRConfig.get(MaidTCRConfig.CONSUME_SHURIKEN, true));
    }

    @Override
    public String getMaidActionSummary() {
        return "Throw Tinkers' Construct shurikens at entities";
    }
}
