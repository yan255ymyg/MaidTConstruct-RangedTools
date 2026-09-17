package com.maidtcr.maidtconstructrangedtools.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.maidtcr.maidtconstructrangedtools.MaidTCRConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeConfigSpec;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * 喷流刃工作模式。
 *
 * <p>匠魂的喷流刃是「近战武器 + 手持流体枪」，本工作模式只做<b>远程</b>部分：
 * 女仆蓄力后用喷流刃喷出流体弹射物（{@code FluidEffectProjectile}）。</p>
 *
 * <ul>
 *   <li>喷射直接交给匠魂 {@code SpittingModule} 执行，喷洒等级、多重射击、水阻力、
 *       {@code projectile_launch} 修饰符钩子、流体消耗与耐久损耗都与本体一致；</li>
 *   <li>流体弹射物有明显下坠（每刻 -0.06 重力、水平速度 0.99 衰减），
 *       所以开火时会按距离临时抬高俯仰角补偿；</li>
 *   <li>罐子里没有流体、而背包里有装着流体的容器（匠魂的铜罐、灯笼、量器、储罐等，
 *       或任何带流体容器能力的物品）时，会自动补满；可在设置里关闭。</li>
 * </ul>
 *
 * <p>注意：只有「有效果」的流体才能喷出弹射物（岩浆、牛奶、蜂蜜、史莱姆、
 * 熔融金属等）；水没有效果，喷不出来。</p>
 */
public class TaskTinkerSwasher extends AbstractTinkerRangedAttackTask {

    public TaskTinkerSwasher() {
        super("swasher", "swasher");
    }

    @Override
    protected ForgeConfigSpec.IntValue rangeConfig() {
        return MaidTCRConfig.SWASHER_RANGE;
    }

    @Override
    public int getChargeTicks(EntityMaid maid) {
        if (!hasTool(maid)) {
            return 0;
        }
        return TinkerRangedHelper.swasherChargeTicks(ToolStack.from(maid.getMainHandItem()), maid);
    }

    @Override
    public int getCooldownTicks(EntityMaid maid) {
        return 10;
    }

    @Override
    protected boolean hasAmmo(EntityMaid maid) {
        return TinkerRangedHelper.canSwasherSpit(maid);
    }

    @Override
    protected String conditionKey() {
        return "has_fluid";
    }

    @Override
    protected boolean conditionPredicate(EntityMaid maid) {
        return TinkerRangedHelper.canSwasherSpit(maid);
    }

    @Override
    public void performRangedAttack(EntityMaid maid, LivingEntity target, float velocity) {
        TinkerRangedHelper.spitSwasher(maid, target);
    }

    @Override
    public String getMaidActionSummary() {
        return "Spray fluid at entities with a Tinkers' Construct swasher";
    }
}
