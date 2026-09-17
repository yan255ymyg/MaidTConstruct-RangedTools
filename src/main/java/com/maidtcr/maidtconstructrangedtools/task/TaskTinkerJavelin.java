package com.maidtcr.maidtconstructrangedtools.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.maidtcr.maidtconstructrangedtools.MaidTCRConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeConfigSpec;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

/**
 * 标枪工作模式。
 *
 * <p>标枪在匠魂里既是近战武器也是投掷武器：这里按投掷处理（与匠魂
 * {@code ThrowingModule} 一致，投掷力度 = charge × velocity × 2）。</p>
 *
 * <p>匠魂原版对玩家是把标枪整个丢出去；为了让女仆能持续作战，
 * 默认保留标枪（可配置为消耗），代价是每次投掷损耗 1 点耐久。</p>
 */
public class TaskTinkerJavelin extends AbstractTinkerRangedAttackTask {

    public TaskTinkerJavelin() {
        super("javelin", "javelin");
    }

    @Override
    protected ForgeConfigSpec.IntValue rangeConfig() {
        return MaidTCRConfig.JAVELIN_RANGE;
    }

    /** 匠魂投掷至少需要蓄力 10 tick，再按拉弓速度换算。 */
    @Override
    public int getChargeTicks(EntityMaid maid) {
        if (!hasTool(maid)) {
            return 0;
        }
        ToolStack tool = ToolStack.from(maid.getMainHandItem());
        float drawSpeed = Math.max(0.05f,
                ConditionalStatModifierHook.getModifiedStat(tool, maid, ToolStats.DRAW_SPEED));
        if (tool.hasTag(TinkerTags.Items.MELEE_WEAPON)) {
            drawSpeed *= Math.max(0.05f, tool.getStats().get(ToolStats.ATTACK_SPEED));
        }
        return Math.max(10, (int) Math.ceil(20.0f / drawSpeed));
    }

    @Override
    public int getCooldownTicks(EntityMaid maid) {
        return 20;
    }

    @Override
    public void performRangedAttack(EntityMaid maid, LivingEntity target, float velocity) {
        TinkerRangedHelper.throwJavelin(maid, target, MaidTCRConfig.get(MaidTCRConfig.CONSUME_JAVELIN, false));
    }

    @Override
    public String getMaidActionSummary() {
        return "Throw Tinkers' Construct javelins at entities";
    }
}
