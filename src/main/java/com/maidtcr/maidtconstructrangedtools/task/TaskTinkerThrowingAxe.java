package com.maidtcr.maidtconstructrangedtools.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.maidtcr.maidtconstructrangedtools.MaidTCRConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 投斧工作模式。
 *
 * <p>与手里剑一样是消耗品，因此要求的是「背包里有」，有敌人时女仆会自动换到主手。
 * 匠魂里投斧与手里剑共用投掷实现。</p>
 */
public class TaskTinkerThrowingAxe extends AbstractTinkerRangedAttackTask {

    public TaskTinkerThrowingAxe() {
        super("throwing_axe", "throwing_axe");
    }

    @Override
    protected ForgeConfigSpec.IntValue rangeConfig() {
        return MaidTCRConfig.THROWING_AXE_RANGE;
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
        TinkerRangedHelper.throwThrowingAxe(maid, target, MaidTCRConfig.get(MaidTCRConfig.CONSUME_THROWING_AXE, true));
    }

    @Override
    public String getMaidActionSummary() {
        return "Throw Tinkers' Construct throwing axes at entities";
    }
}
