package com.maidtcr.maidtconstructrangedtools.ai;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.github.tartaricacid.touhoulittlemaid.util.TaskEquipUtil;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * 「发现敌人在身侧但主手没拿对的工具 → 从背包换到主手」的一次性行为。
 *
 * <p>手里剑、投斧这类消耗性工具要求的是「背包里有」，所以需要在开打前自动换上，
 * 思路与女仆的农场模式（用背包里的种子/工具）一致，直接复用女仆自己的
 * {@link TaskEquipUtil#tryEquipFromBackpack}。</p>
 *
 * <p>优先级要比攻击行为高（数值更小），这样它才有机会先执行。</p>
 */
public class MaidTinkerEquipTask extends Behavior<EntityMaid> {

    private final Predicate<ItemStack> weaponTest;

    public MaidTinkerEquipTask(Predicate<ItemStack> weaponTest) {
        super(ImmutableMap.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
                MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT), 1200);
        this.weaponTest = weaponTest;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        if (maid.isHolding(weaponTest)) {
            return false;
        }
        return ItemsUtil.findStackSlot(maid.getAvailableBackpackInv(), weaponTest) >= 0;
    }

    /** 一次性行为：换上就退场，让攻击行为接管。 */
    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        return false;
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        TaskEquipUtil.tryEquipFromBackpack(maid, weaponTest);
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long gameTime) {
    }
}
