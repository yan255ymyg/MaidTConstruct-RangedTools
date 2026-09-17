package com.maidtcr.maidtconstructrangedtools.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.maidtcr.maidtconstructrangedtools.MaidTCRConfig;
import com.mojang.datafixers.util.Pair;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeConfigSpec;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * 「钓鱼竿」工作模式——<b>当前不可用，仅作占位</b>。
 *
 * <p>女仆用匠魂钓鱼竿攻击时，会在玩家身上出现鱼线且不造成伤害
 * （匠魂战斗鱼钩本质是原版 {@code FishingHook}，要求“主人”是玩家）。
 * 考虑到实用性不强，这里保留全部实现代码但让它不可达：</p>
 * <ul>
 *   <li>工作模式仍然出现在列表里，图标换成屏障方块，方便玩家知道它被禁用了；</li>
 *   <li>不挂载任何 AI 行为，因此选中后女仆不会有任何动作；</li>
 *   <li>{@link #performRangedAttack} 与鱼钩实体、假玩家代理等代码保留，供以后修复时使用。</li>
 * </ul>
 *
 * <p>想用匠魂钓鱼竿钓鱼，请使用女仆自带的「钓鱼」工作模式
 * （本模组已为那个模式补上匠魂适配，见 {@code TinkerFishingType}）。</p>
 */
public class TaskTinkerFishingRod extends AbstractTinkerRangedAttackTask {

    public TaskTinkerFishingRod() {
        super("fishing_rod", "fishing_rod");
    }

    @Override
    protected ForgeConfigSpec.IntValue rangeConfig() {
        return MaidTCRConfig.FISHING_ROD_RANGE;
    }

    /** 禁用状态：图标固定为屏障方块。 */
    @Override
    public ItemStack getIcon() {
        return Items.BARRIER.getDefaultInstance();
    }

    @Override
    @Nullable
    public SoundEvent getAmbientSound(EntityMaid maid) {
        return null;
    }

    /** 没有任何 AI 行为 → 选中该工作模式后女仆不会有任何效果。 */
    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        return Collections.emptyList();
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createRideBrainTasks(EntityMaid maid) {
        return Collections.emptyList();
    }

    @Override
    public List<Pair<String, Predicate<EntityMaid>>> getConditionDescription(EntityMaid maid) {
        return Collections.singletonList(Pair.of("bugged", ignored -> false));
    }

    /** 保留但不可达：见类注释。 */
    @Override
    public void performRangedAttack(EntityMaid maid, LivingEntity target, float velocity) {
        TinkerRangedHelper.castFishingHook(maid);
    }

    @Override
    public String getMaidActionSummary() {
        return "Disabled: the Tinkers' Construct fishing rod attack is bugged and does nothing";
    }
}
