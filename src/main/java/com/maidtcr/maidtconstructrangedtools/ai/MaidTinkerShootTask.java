package com.maidtcr.maidtconstructrangedtools.ai;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.maidtcr.maidtconstructrangedtools.task.AbstractTinkerRangedAttackTask;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * 通用“举械—蓄力—放”的远程攻击 AI。
 *
 * <p>女仆模组自带的 {@code MaidShootTargetTask} 依赖 {@code startUsingItem}
 * 与物品的 {@code getUseDuration}，对匠魂工具（持续时间由修饰符决定、且要求 Player 上下文）
 * 并不可靠，因此这里自己实现一套纯计时的射击节奏：</p>
 * <ul>
 *   <li>冷却结束且能看见目标 → 开始蓄力（{@code chargeTicks} 为 0 则立即出手）</li>
 *   <li>蓄力期间持续面向目标；目标丢失过久则中断</li>
 *   <li>蓄力满 → 调用 {@code maid.performRangedAttack}，由当前工作模式的
 *       {@code performRangedAttack} 真正发射弹射物</li>
 * </ul>
 */
public class MaidTinkerShootTask extends Behavior<EntityMaid> {

    private static final int MAX_SEE_TIME = 20;
    private static final int MAX_LOST_TIME = 20;

    private final AbstractTinkerRangedAttackTask task;
    private final Predicate<ItemStack> weaponTest;

    private int attackTime = -1;
    private int seeTime;
    private int chargeTime = -1;

    public MaidTinkerShootTask(AbstractTinkerRangedAttackTask task, Predicate<ItemStack> weaponTest) {
        super(ImmutableMap.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
                MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT), 1200);
        this.task = task;
        this.weaponTest = weaponTest;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        Optional<LivingEntity> target = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        return target.isPresent()
                && maid.isHolding(weaponTest)
                && maid.canSee(target.get());
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        return maid.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                && checkExtraStartConditions(level, maid);
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        this.attackTime = 0;
        this.chargeTime = -1;
        maid.setSwingingArms(true);
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        Optional<LivingEntity> memory = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (memory.isEmpty()) {
            return;
        }
        LivingEntity target = memory.get();
        maid.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ());

        boolean canSee = maid.canSee(target);
        if (canSee) {
            this.seeTime = Math.min(MAX_SEE_TIME, this.seeTime + 1);
        } else {
            this.seeTime = Math.max(-MAX_LOST_TIME, this.seeTime - 1);
        }

        int chargeTicks = Math.max(0, task.getChargeTicks(maid));
        if (this.chargeTime >= 0) {
            // 正在蓄力
            if (!canSee && this.seeTime <= -MAX_LOST_TIME) {
                this.chargeTime = -1;
                return;
            }
            this.chargeTime++;
            if (canSee && this.chargeTime >= chargeTicks) {
                fire(maid, target);
            }
            return;
        }

        if (this.attackTime > 0) {
            this.attackTime--;
            return;
        }
        if (this.seeTime < 0) {
            return;
        }
        if (chargeTicks <= 0) {
            fire(maid, target);
        } else {
            this.chargeTime = 0;
        }
    }

    private void fire(EntityMaid maid, LivingEntity target) {
        // 直接走工作模式的 performRangedAttack：
        // EntityMaid 的远程攻击入口在编译期映射下不可见（女仆类里那个方法被混淆成了 SRG 名，
        // 且它并不实现 RangedAttackMob），这里直接调用任务实现，行为一致且更稳定。
        task.performRangedAttack(maid, target, 1.0f);
        this.attackTime = Math.max(1, task.getCooldownTicks(maid));
        this.chargeTime = -1;
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long gameTime) {
        this.seeTime = 0;
        this.attackTime = -1;
        this.chargeTime = -1;
        maid.setSwingingArms(false);
    }
}
