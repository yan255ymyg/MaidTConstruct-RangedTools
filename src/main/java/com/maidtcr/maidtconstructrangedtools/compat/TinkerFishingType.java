package com.maidtcr.maidtconstructrangedtools.compat;

import com.github.tartaricacid.touhoulittlemaid.api.entity.fishing.IFishingType;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.fishing.FishingTypeManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.projectile.MaidFishingHook;
import com.maidtcr.maidtconstructrangedtools.MaidTConstructRangedTools;
import com.maidtcr.maidtconstructrangedtools.util.TinkerToolLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

/**
 * 让女仆原有的“钓鱼”工作模式认识匠魂钓鱼竿。
 *
 * <p>匠魂钓鱼竿自带 {@code tconstruct:fishing} 特性，能响应 {@code ToolActions.FISHING_ROD_CAST}，
 * 因此女仆模组默认的 {@code DefaultFishingType} 已经能识别它；本类在此之上把匠魂工具的
 * <b>海之眷顾 / 诱饵</b> 属性映射到女仆钓钩的幸运与咬钩速度上，让“匠魂钓鱼竿钓鱼”
 * 与匠魂本体手感一致。</p>
 */
public class TinkerFishingType implements IFishingType {

    private static final String TCONSTRUCT = "tconstruct";
    private static final String FISHING_ROD = "fishing_rod";

    @Override
    public boolean isFishingRod(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getItem() == TinkerToolLookup.toolItem(FISHING_ROD);
    }

    @Override
    public MaidFishingHook getFishingHook(EntityMaid maid, Level level, ItemStack stack, Vec3 pos) {
        int luck = 0;
        int lure = 0;
        try {
            IToolStackView tool = ToolStack.from(stack);
            luck = (int) ConditionalStatModifierHook.getModifiedStat(tool, maid, ToolStats.SEA_LUCK);
            lure = (int) ConditionalStatModifierHook.getModifiedStat(tool, maid, ToolStats.LURE);
        } catch (Exception e) {
            MaidTConstructRangedTools.LOGGER.warn("读取匠魂钓鱼竿属性失败，回退为默认钓钩", e);
        }
        return new MaidFishingHook(maid, level, luck, lure, pos);
    }

    /**
     * 注册到女仆模组的钓鱼类型列表。
     *
     * <p>{@code FishingTypeManager} 是 final 且没有暴露实例，但 {@code addFishingType}
     * 只操作静态列表，因此这里新建一个实例调用即可（与自带的 Aquaculture 兼容层同样思路）。</p>
     */
    public static void register() {
        new FishingTypeManager().addFishingType(new TinkerFishingType());
        MaidTConstructRangedTools.LOGGER.info("Registered Tinkers' Construct fishing rod support for maids");
    }
}
