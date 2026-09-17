package com.maidtcr.maidtconstructrangedtools.task;

import com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidAttackStrafingAnyItemTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidRangedWalkToTarget;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.github.tartaricacid.touhoulittlemaid.util.SoundUtil;
import com.google.common.collect.Lists;
import com.maidtcr.maidtconstructrangedtools.MaidTConstructRangedTools;
import com.maidtcr.maidtconstructrangedtools.ai.MaidTinkerEquipTask;
import com.maidtcr.maidtconstructrangedtools.ai.MaidTinkerShootTask;
import com.maidtcr.maidtconstructrangedtools.util.TinkerToolLookup;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeConfigSpec;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

/**
 * 匠魂远程工具工作模式的公共实现。
 *
 * <p>AI 结构刻意与女仆模组自带的弓箭工作模式保持一致（优先级全部为 5）：
 * 选取目标 → 目标无效则放弃 → 靠近到射程 → 侧移 → 射击。
 * 若任务声明需要「从背包换武器」（{@link #enableBackpackEquip()}），
 * 会额外插入一个优先级 4 的换装行为。</p>
 */
public abstract class AbstractTinkerRangedAttackTask implements IRangedAttackTask {

    private static final int ATTACK_PRIORITY = 5;
    private static final int EQUIP_PRIORITY = 4;

    private static final float WALK_SPEED = 0.6f;
    private static final float STRAFE_SPEED = 0.6f;

    private final ResourceLocation uid;
    private final String toolPath;
    private ItemStack icon = ItemStack.EMPTY;

    protected AbstractTinkerRangedAttackTask(String taskPath, String toolPath) {
        this.uid = MaidTConstructRangedTools.id(taskPath);
        this.toolPath = toolPath;
    }

    /* ------------------------------------------------------------------ 基础信息 */

    @Override
    public ResourceLocation getUid() {
        return uid;
    }

    /** 工作模式图标：对应工具的骑士史莱姆材质版本（个别任务会覆写）。 */
    @Override
    public ItemStack getIcon() {
        if (icon.isEmpty()) {
            icon = TinkerToolLookup.icon(toolPath);
        }
        return icon.copy();
    }

    @Override
    @Nullable
    public SoundEvent getAmbientSound(EntityMaid maid) {
        return SoundUtil.attackSound(maid, InitSounds.MAID_RANGE_ATTACK.get(), 0.5f);
    }

    public String getToolPath() {
        return toolPath;
    }

    /* ------------------------------------------------------------------ 武器判定 */

    @Override
    public boolean isWeapon(EntityMaid maid, ItemStack stack) {
        return TinkerToolLookup.isTool(stack, toolPath);
    }

    /** 主手是否拿着本工作模式要用的工具。 */
    protected boolean hasTool(EntityMaid maid) {
        return isWeapon(maid, maid.getMainHandItem());
    }

    /**
     * 是否允许「背包里有工具就行」，由女仆自己在开打前换到主手。
     * 手里剑、投斧这类消耗性工具会覆写为 true。
     */
    protected boolean enableBackpackEquip() {
        return false;
    }

    protected boolean hasWeaponInBackpack(EntityMaid maid) {
        return ItemsUtil.findStackSlot(maid.getAvailableBackpackInv(),
                stack -> isWeapon(maid, stack)) >= 0;
    }

    /** 手上或背包里能否拿到武器。 */
    protected boolean canUseWeapon(EntityMaid maid) {
        if (hasTool(maid)) {
            return true;
        }
        return enableBackpackEquip() && hasWeaponInBackpack(maid);
    }

    /** 是否有可用的弹药/投掷物；默认不限制（长弓、弩由匠魂的强化自行解决弹药）。 */
    protected boolean hasAmmo(EntityMaid maid) {
        return true;
    }

    /* ------------------------------------------------------------------ 节奏参数 */

    /** 出手前需要蓄力的 tick 数，0 表示立即出手。 */
    public int getChargeTicks(EntityMaid maid) {
        return 0;
    }

    /** 两次出手之间的冷却 tick 数。 */
    public int getCooldownTicks(EntityMaid maid) {
        return 10;
    }

    /* ------------------------------------------------------------------ 搜索范围 */

    /** 本工作模式的射程配置项（显示在女仆全局设置的「女仆匠魂：远程工具」分类里）。 */
    protected abstract ForgeConfigSpec.IntValue rangeConfig();

    @Override
    public boolean canSee(EntityMaid maid, LivingEntity target) {
        return IRangedAttackTask.targetConditionsTest(maid, target, rangeConfig());
    }

    @Override
    public float searchRadius(EntityMaid maid) {
        return rangeConfig().get();
    }

    @Override
    public AABB searchDimension(EntityMaid maid) {
        if (canUseWeapon(maid) && hasAmmo(maid)) {
            float radius = searchRadius(maid);
            return maid.isPassenger()
                    ? new AABB(maid.blockPosition()).inflate(radius)
                    : maid.getBoundingBox().inflate(radius);
        }
        return IRangedAttackTask.super.searchDimension(maid);
    }

    /* ------------------------------------------------------------------ AI */

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        Predicate<ItemStack> weaponTest = stack -> isWeapon(maid, stack);
        List<Pair<Integer, BehaviorControl<? super EntityMaid>>> tasks = Lists.newArrayList();
        if (enableBackpackEquip()) {
            tasks.add(Pair.of(EQUIP_PRIORITY, new MaidTinkerEquipTask(weaponTest)));
        }
        BehaviorControl<? super EntityMaid> startAttacking = StartAttacking.create(
                this::canStartAttacking, IRangedAttackTask::findFirstValidAttackTarget);
        BehaviorControl<? super EntityMaid> stopAttacking = StopAttackingIfTargetInvalid.create(
                target -> shouldGiveUp(maid, target));
        BehaviorControl<? super EntityMaid> walkToTarget = MaidRangedWalkToTarget.create(WALK_SPEED);
        BehaviorControl<? super EntityMaid> strafe = new MaidAttackStrafingAnyItemTask(
                weaponTest, searchRadius(maid), STRAFE_SPEED);
        BehaviorControl<? super EntityMaid> shoot = new MaidTinkerShootTask(this, weaponTest);
        tasks.add(Pair.of(ATTACK_PRIORITY, startAttacking));
        tasks.add(Pair.of(ATTACK_PRIORITY, stopAttacking));
        tasks.add(Pair.of(ATTACK_PRIORITY, walkToTarget));
        tasks.add(Pair.of(ATTACK_PRIORITY, strafe));
        tasks.add(Pair.of(ATTACK_PRIORITY, shoot));
        return tasks;
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createRideBrainTasks(EntityMaid maid) {
        Predicate<ItemStack> weaponTest = stack -> isWeapon(maid, stack);
        List<Pair<Integer, BehaviorControl<? super EntityMaid>>> tasks = Lists.newArrayList();
        if (enableBackpackEquip()) {
            tasks.add(Pair.of(EQUIP_PRIORITY, new MaidTinkerEquipTask(weaponTest)));
        }
        BehaviorControl<? super EntityMaid> startAttacking = StartAttacking.create(
                this::canStartAttacking, IRangedAttackTask::findFirstValidAttackTarget);
        BehaviorControl<? super EntityMaid> stopAttacking = StopAttackingIfTargetInvalid.create(
                target -> shouldGiveUp(maid, target));
        BehaviorControl<? super EntityMaid> shoot = new MaidTinkerShootTask(this, weaponTest);
        tasks.add(Pair.of(ATTACK_PRIORITY, startAttacking));
        tasks.add(Pair.of(ATTACK_PRIORITY, stopAttacking));
        tasks.add(Pair.of(ATTACK_PRIORITY, shoot));
        return tasks;
    }

    private boolean canStartAttacking(EntityMaid maid) {
        return canUseWeapon(maid) && hasAmmo(maid);
    }

    /** 返回 true 表示应当放弃当前目标。 */
    private boolean shouldGiveUp(EntityMaid maid, LivingEntity target) {
        if (!canUseWeapon(maid) || !hasAmmo(maid)) {
            return true;
        }
        return maid.distanceTo(target) > searchRadius(maid);
    }

    /* ------------------------------------------------------------------ 界面提示 */

    @Override
    public List<Pair<String, Predicate<EntityMaid>>> getConditionDescription(EntityMaid maid) {
        return Lists.newArrayList(Pair.of(conditionKey(), this::conditionPredicate));
    }

    /** 条件提示使用的语言键后缀，默认沿用「主手持有工具」。 */
    protected String conditionKey() {
        return "has_tool";
    }

    protected boolean conditionPredicate(EntityMaid maid) {
        return canUseWeapon(maid);
    }

    @Override
    public String getMaidActionSummary() {
        return "Use a Tinkers' Construct ranged tool to attack entities";
    }
}
