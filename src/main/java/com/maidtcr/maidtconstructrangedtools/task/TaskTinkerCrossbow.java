package com.maidtcr.maidtconstructrangedtools.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.maidtcr.maidtconstructrangedtools.MaidTCRConfig;
import com.maidtcr.maidtconstructrangedtools.util.TinkerToolLookup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * 弩工作模式。
 *
 * <p>匠魂的弩是“先装填、再发射”：第一次动作把弹药装到弩上（写入
 * {@code tconstruct:crossbow_ammo}），第二次动作发射。这里也照这个节奏走，
 * 装填耗时用 {@code draw_speed} 换算。</p>
 *
 * <p>与长弓一样<b>不要求背包里有箭</b>：弹药由匠魂的 {@code BowAmmoModifierHook} 提供，
 * 因此「晶簇」等无限箭强化可以正常工作（装填时按 4 点/支扣耐久）。</p>
 *
 * <p>匠魂的战镐（{@code tconstruct:war_pick}）是镐与弩的结合，同样是
 * {@code ModifiableCrossbowItem}，因此本工作模式也可以拿战镐远程攻击
 * （射程共用弩的这一项设置）。</p>
 */
public class TaskTinkerCrossbow extends AbstractTinkerRangedAttackTask {

    public static final String WAR_PICK = "war_pick";

    public TaskTinkerCrossbow() {
        super("crossbow", "crossbow");
    }

    @Override
    protected ForgeConfigSpec.IntValue rangeConfig() {
        return MaidTCRConfig.CROSSBOW_RANGE;
    }

    @Override
    public boolean isWeapon(EntityMaid maid, ItemStack stack) {
        return TinkerToolLookup.isTool(stack, getToolPath()) || TinkerToolLookup.isTool(stack, WAR_PICK);
    }

    @Override
    public int getChargeTicks(EntityMaid maid) {
        if (!hasTool(maid)) {
            return 0;
        }
        // 已经装填好了就立刻发射，否则先花时间装填
        if (TinkerRangedHelper.isCrossbowLoaded(maid)) {
            return 0;
        }
        return TinkerRangedHelper.crossbowLoadTicks(ToolStack.from(maid.getMainHandItem()), maid);
    }

    @Override
    public int getCooldownTicks(EntityMaid maid) {
        return 10;
    }

    @Override
    public void performRangedAttack(EntityMaid maid, LivingEntity target, float velocity) {
        TinkerRangedHelper.crossbowAttack(maid);
    }

    @Override
    public String getMaidActionSummary() {
        return "Load and fire a Tinkers' Construct crossbow or war pick at entities";
    }
}
