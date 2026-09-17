package com.maidtcr.maidtconstructrangedtools.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.maidtcr.maidtconstructrangedtools.MaidTCRConfig;
import com.maidtcr.maidtconstructrangedtools.MaidTConstructRangedTools;
import com.maidtcr.maidtconstructrangedtools.entity.MaidCombatFishingHook;
import com.maidtcr.maidtconstructrangedtools.util.MaidFishingProxy;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectManager;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.BowAmmoModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.tools.capability.EntityModifierCapability;
import slimeknights.tconstruct.library.tools.capability.PersistentDataCapability;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableCrossbowItem;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableLauncherItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.FloatToolStat;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerToolActions;
import slimeknights.tconstruct.tools.entity.CombatFishingHook;
import slimeknights.tconstruct.tools.entity.ThrownShuriken;
import slimeknights.tconstruct.tools.entity.ThrownTool;
import slimeknights.tconstruct.tools.modules.interaction.FishingModule;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * 把匠魂各远程工具的“玩家右键”逻辑改写成以女仆为发射者。
 *
 * <p>这里的弹射物构造参数都对齐匠魂本体源码，保证工具属性（velocity、
 * projectile_damage、water_inertia、accuracy 等）以及各类修饰符钩子照常生效。</p>
 */
public final class TinkerRangedHelper {

    /** 弹射物每刻的阻力（三者都是 0.99，见各自 tick）。 */
    private static final double PROJECTILE_DRAG = 0.99;
    /** 手里剑/投斧：匠魂 ThrownShuriken 每刻 -0.03 重力。 */
    private static final double SHURIKEN_GRAVITY = 0.03;
    /** 标枪：ThrownTool 继承原版 AbstractArrow，每刻 -0.05 重力。 */
    private static final double JAVELIN_GRAVITY = 0.05;
    /** 喷流刃：匠魂 FluidEffectProjectile 每刻 -0.06 重力。 */
    private static final double SWASHER_GRAVITY = 0.06;

    /** 弹射物生成点相对发射者眼睛的高度差。 */
    private static final double SHURIKEN_SPAWN_OFFSET = -0.1;
    /** ThrownTool 的构造里是 setPos(owner.getY() - 0.1, ...)，也就是从脚下稍下生成。 */
    private static final double JAVELIN_SPAWN_AT_FEET_OFFSET = -0.1;
    private static final double SWASHER_SPAWN_OFFSET = -0.1;

    /**
     * 求解瞄准角时在女仆当前俯仰角附近搜索的范围（度）。
     *
     * <p>负方向是抬头：慢速弹射物（投斧、标枪）在中远距离需要相当高的抛射角才能打到，
     * 所以向上的范围要留足；向下不需要多少。</p>
     */
    private static final float AIM_MAX_UP = 60.0f;
    private static final float AIM_MAX_DOWN = 20.0f;
    private static final float AIM_SEARCH_STEP = 0.5f;
    private static final int AIM_SIM_MAX_TICKS = 600;

    /**
     * 各弹射物的经验微调量（度）。<b>正数 = 压低一点，负数 = 抬高一点。</b>
     *
     * <p>弹道求解给的是理论值，实际游戏里还有碰撞盒、随机散布、生成点取整等偏差，
     * 这里用常数补偿补齐。</p>
     */
    private static final float AIM_TRIM_SHURIKEN = 1.5f;
    private static final float AIM_TRIM_THROWING_AXE = -3.5f;
    private static final float AIM_TRIM_JAVELIN = 2.0f;
    private static final float AIM_TRIM_SWASHER = 0.0f;

    private static final String SPITTING_ID = "tconstruct:spitting";

    /** 「背包里是否有可用流体容器」的短时缓存，避免每 tick 反复扫描背包。 */
    private static final Map<EntityMaid, ContainerCache> CONTAINER_CACHE = new WeakHashMap<>();
    private static final int CONTAINER_CACHE_TICKS = 20;

    private record ContainerCache(long tick, boolean present) {
    }

    private TinkerRangedHelper() {
    }

    private static float stat(IToolStackView tool, LivingEntity entity, FloatToolStat toolStat) {
        return ConditionalStatModifierHook.getModifiedStat(tool, entity, toolStat);
    }

    /** 依据匠魂拉弓/蓄力速度换算 tick 数。 */
    private static int drawTicks(ToolStack tool, EntityMaid maid, int min, float multiplier) {
        float drawSpeed = Math.max(0.05f, stat(tool, maid, ToolStats.DRAW_SPEED)) * multiplier;
        return Math.max(min, (int) Math.ceil(20.0f / drawSpeed));
    }

    /* ------------------------------------------------------------------ 弹道求解 */

    /**
     * 按真实弹道模型模拟一步：返回水平飞过 {@code targetDistance} 时的相对高度。
     *
     * <p>与 Minecraft 的积分顺序一致：先位移，再乘阻力，最后加重力。</p>
     */
    private static double simulateHeight(float pitchDeg, double speed, double gravity, double drag, double targetDistance) {
        double pitch = Math.toRadians(pitchDeg);
        // shootFromRotation：竖直分量是 -sin(pitch)，所以 pitch 为正表示向下
        double vx = speed * Math.cos(pitch);
        double vy = -speed * Math.sin(pitch);
        double x = 0.0;
        double y = 0.0;
        for (int tick = 0; tick < AIM_SIM_MAX_TICKS; tick++) {
            x += vx;
            y += vy;
            vx *= drag;
            vy = vy * drag - gravity;
            if (x >= targetDistance) {
                break;
            }
            if (y < -512.0) {
                break;
            }
        }
        return y;
    }

    /**
     * 解出命中目标所需的俯仰角。
     *
     * <p>不再用“速度乘个经验系数”去估角度，而是拿匠魂/原版真实的每刻重力与阻力
     * （手里剑 -0.03、标枪 -0.05、喷流刃 -0.06，阻力都是 0.99）做逐刻数值模拟，
     * 在女仆当前朝向附近搜索误差最小的角度。这样轻而快的弹射物（手里剑）
     * 自然只抬一点点，重而慢的（投斧、标枪）才会抬得多。</p>
     *
     * @param spawnOffsetFromEye 弹射物生成点相对发射者眼睛的高度差
     * @param trimDeg            实机微调量（正数压低、负数抬高）
     */
    private static float solvePitch(EntityMaid maid, LivingEntity target, double speed, double gravity,
                                    double spawnOffsetFromEye, float trimDeg) {
        float basePitch = maid.getXRot();
        if (target == null || speed <= 0.05) {
            return basePitch;
        }
        double dx = target.getX() - maid.getX();
        double dz = target.getZ() - maid.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 1.0) {
            return basePitch;
        }
        // 目标相对弹射物生成点的高度差
        double targetHeight = target.getEyeY() - (maid.getEyeY() + spawnOffsetFromEye);

        float bestPitch = basePitch;
        double bestError = Double.MAX_VALUE;
        // delta 为负 = 抬头（Minecraft 里 pitch 为正表示往下看）
        for (float delta = -AIM_MAX_UP; delta <= AIM_MAX_DOWN + 1.0e-3f; delta += AIM_SEARCH_STEP) {
            float pitch = Mth.clamp(basePitch + delta, -90.0f, 90.0f);
            double height = simulateHeight(pitch, speed, gravity, PROJECTILE_DRAG, distance);
            double error = Math.abs(height - targetHeight);
            if (error < bestError) {
                bestError = error;
                bestPitch = pitch;
            }
        }
        return Mth.clamp(bestPitch + trimDeg, -90.0f, 90.0f);
    }

    /* ------------------------------------------------------------------ 弹药 */

    /** 长弓/弩可用的弹药判定。 */
    public static Predicate<ItemStack> launcherAmmoTest(ItemStack launcher) {
        if (launcher.getItem() instanceof ModifiableCrossbowItem crossbow) {
            // 匠魂的弩支持箭与烟花
            return crossbow.getSupportedHeldProjectiles();
        }
        return ProjectileWeaponItem.ARROW_ONLY;
    }

    /**
     * 取一份弹药。
     *
     * <p>顺序与匠魂本体一致：先问修饰符（例如能力强化「晶簇」= {@code InfinityModule}，
     * 它会提供水晶箭并按 4 点/支扣除耐久），拿不到再退回女仆背包里的实体箭。</p>
     *
     * <p>注意：女仆不是玩家，{@code LivingEntity#getProjectile} 对她始终返回空，
     * 所以匠魂的 {@code BowAmmoModifierHook} 搜不到她的背包（TLM 的背包是 Forge 物品栏），
     * 这一层兜底是必要的。</p>
     */
    public static ItemStack takeAmmo(EntityMaid maid, ItemStack launcher, ToolStack tool, Predicate<ItemStack> ammoTest) {
        ItemStack ammo = BowAmmoModifierHook.consumeAmmo(tool, launcher, maid, null, ammoTest);
        if (!ammo.isEmpty()) {
            return ammo;
        }
        CombinedInvWrapper inv = maid.getAvailableInv(true);
        int slot = ItemsUtil.findStackSlot(inv, ammoTest);
        if (slot < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack found = inv.getStackInSlot(slot);
        if (found.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (!MaidTCRConfig.get(MaidTCRConfig.CONSUME_ARROW, true)) {
            // 不消耗：只借一支的“样子”，不动背包
            return found.copyWithCount(1);
        }
        ItemStack taken = found.split(1);
        inv.setStackInSlot(slot, found);
        return taken;
    }

    /* ------------------------------------------------------------------ 长弓 */

    /** 依据匠魂拉弓速度换算需要的蓄力 tick 数。 */
    public static int longbowChargeTicks(ToolStack tool, EntityMaid maid) {
        return drawTicks(tool, maid, 1, 1.0f);
    }

    public static void fireLongbow(EntityMaid maid) {
        ItemStack bow = maid.getMainHandItem();
        if (bow.isEmpty()) {
            return;
        }
        ToolStack tool = ToolStack.from(bow);
        if (tool.isBroken()) {
            return;
        }
        Level level = maid.level();
        if (level.isClientSide) {
            return;
        }

        // 没有实体箭也行：匠魂的「晶簇」等强化会在这里给出水晶箭并扣耐久
        ItemStack ammo = takeAmmo(maid, bow, tool, launcherAmmoTest(bow));
        if (ammo.isEmpty()) {
            return;
        }

        float power = stat(tool, maid, ToolStats.VELOCITY);
        if (power < 0.1f) {
            return;
        }
        float inaccuracy = ModifierUtil.getInaccuracy(tool, maid);

        int count = ammo.getCount();
        float startAngle = ModifiableLauncherItem.getAngleStart(count);
        int primaryIndex = count / 2;
        ModifierNBT modifiers = tool.getModifiers();

        for (int index = 0; index < count; index++) {
            ArrowItem arrowItem = ammo.getItem() instanceof ArrowItem arrow ? arrow : (ArrowItem) Items.ARROW;
            AbstractArrow projectile = arrowItem.createArrow(level, ammo, maid);
            float angle = startAngle + (10 * index);
            projectile.shootFromRotation(maid, maid.getXRot() + angle, maid.getYRot(), 0.0f, power * 3.0f, inaccuracy);
            projectile.setCritArrow(true);

            // 匠魂把原版箭的 2 点基础伤害扣掉，换成工具自己的弹射物伤害
            float baseDamage = (float) (projectile.getBaseDamage() - 2 + tool.getStats().get(ToolStats.PROJECTILE_DAMAGE));
            projectile.setBaseDamage(ConditionalStatModifierHook.getModifiedStat(
                    tool, maid, ToolStats.PROJECTILE_DAMAGE, baseDamage));

            runProjectileLaunchHooks(tool, maid, ammo, projectile, modifiers, index == primaryIndex);

            level.addFreshEntity(projectile);
            level.playSound(null, maid.getX(), maid.getY(), maid.getZ(),
                    SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f,
                    1.0f / (level.getRandom().nextFloat() * 0.4f + 1.2f) + 0.5f + (angle / 10f));
        }
        ToolDamageUtil.damageAnimated(tool, count, maid, EquipmentSlot.MAINHAND);
    }

    /* ------------------------------------------------------------------ 弩 */

    public static boolean isCrossbowLoaded(EntityMaid maid) {
        ItemStack stack = maid.getMainHandItem();
        if (stack.isEmpty()) {
            return false;
        }
        return !ToolStack.from(stack).getPersistentData()
                .getCompound(ModifiableCrossbowItem.KEY_CROSSBOW_AMMO).isEmpty();
    }

    /** 装填所需 tick 数（匠魂用 draw_time 而不是原版装填时间）。 */
    public static int crossbowLoadTicks(ToolStack tool, EntityMaid maid) {
        return drawTicks(tool, maid, 5, 1.0f);
    }

    /**
     * 一次“弩的动作”：已装填就发射，没装填就装填。
     *
     * <p>装填走匠魂的 {@code BowAmmoModifierHook#consumeAmmo}，所以「晶簇」的水晶箭
     * 与 4 点耐久消耗都由匠魂自己结算；弹药按 NBT 存在弩上，与本体行为一致。</p>
     */
    public static void crossbowAttack(EntityMaid maid) {
        ItemStack stack = maid.getMainHandItem();
        if (stack.isEmpty()) {
            return;
        }
        ToolStack tool = ToolStack.from(stack);
        if (tool.isBroken()) {
            return;
        }
        Level level = maid.level();
        if (level.isClientSide) {
            return;
        }

        ModDataNBT data = tool.getPersistentData();
        CompoundTag heldAmmo = data.getCompound(ModifiableCrossbowItem.KEY_CROSSBOW_AMMO);
        if (!heldAmmo.isEmpty()) {
            ModifiableCrossbowItem.fireCrossbow(tool, maid, false, InteractionHand.MAIN_HAND, heldAmmo);
            return;
        }

        ItemStack ammo = takeAmmo(maid, stack, tool, launcherAmmoTest(stack));
        if (ammo.isEmpty()) {
            return;
        }
        data.put(ModifiableCrossbowItem.KEY_CROSSBOW_AMMO, ammo.save(new CompoundTag()));
        level.playSound(null, maid.getX(), maid.getY(), maid.getZ(),
                SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0f,
                1.0f / (level.getRandom().nextFloat() * 0.5f + 1.0f) + 0.2f);
    }

    /* ------------------------------------------------------------------ 手里剑 / 投斧 */

    public static void throwShuriken(EntityMaid maid, LivingEntity target, boolean consume) {
        throwThrown(maid, target, consume, AIM_TRIM_SHURIKEN);
    }

    public static void throwThrowingAxe(EntityMaid maid, LivingEntity target, boolean consume) {
        throwThrown(maid, target, consume, AIM_TRIM_THROWING_AXE);
    }

    /** 手里剑与投斧共用匠魂的 ThrownShuriken，只是初速与微调量不同。 */
    private static void throwThrown(EntityMaid maid, LivingEntity target, boolean consume, float aimTrim) {
        ItemStack stack = maid.getMainHandItem();
        if (stack.isEmpty()) {
            return;
        }
        ToolStack tool = ToolStack.from(stack);
        if (tool.isBroken()) {
            return;
        }
        Level level = maid.level();
        if (level.isClientSide) {
            return;
        }

        ThrownShuriken shuriken = new ThrownShuriken(level, maid);
        IToolStackView projectileTool = shuriken.onCreate(stack, maid);
        float velocity = ConditionalStatModifierHook.getModifiedStat(projectileTool, maid, ToolStats.VELOCITY);
        float pitch = solvePitch(maid, target, velocity, SHURIKEN_GRAVITY, SHURIKEN_SPAWN_OFFSET, aimTrim);
        shuriken.shootFromRotation(maid, pitch, maid.getYRot(), 0.0f, velocity, 1.0f);
        level.addFreshEntity(shuriken);

        level.playSound(null, maid.getX(), maid.getY(), maid.getZ(),
                Sounds.SHURIKEN_THROW.getSound(), SoundSource.NEUTRAL, 0.5f,
                0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));

        if (consume) {
            stack.shrink(1);
        }
    }

    /* ------------------------------------------------------------------ 标枪 */

    public static void throwJavelin(EntityMaid maid, LivingEntity target, boolean consume) {
        ItemStack stack = maid.getMainHandItem();
        if (stack.isEmpty()) {
            return;
        }
        ToolStack tool = ToolStack.from(stack);
        if (tool.isBroken()) {
            return;
        }
        Level level = maid.level();
        if (level.isClientSide) {
            return;
        }

        float charge = 1.0f;
        float velocity = stat(tool, maid, ToolStats.VELOCITY);
        float waterInertia = stat(tool, maid, ToolStats.WATER_INERTIA);
        float speed = charge * velocity * 2.0f;
        // 标枪又慢又重（重力 0.05），而且生成点在脚下稍下，需要明显抬头
        double spawnOffset = (maid.getY() + JAVELIN_SPAWN_AT_FEET_OFFSET) - maid.getEyeY();
        float pitch = solvePitch(maid, target, speed, JAVELIN_GRAVITY, spawnOffset, AIM_TRIM_JAVELIN);

        // 不消耗时投掷的是副本，并且禁止拾取，避免刷物品
        ItemStack thrownStack = consume ? stack : stack.copy();

        ThrownTool thrown = new ThrownTool(level, maid, thrownStack, charge, velocity, waterInertia);
        thrown.setOriginalSlot(-1);
        thrown.shootFromRotation(maid, pitch, maid.getYRot(), 0.0f,
                speed, ModifierUtil.getInaccuracy(tool, maid));
        if (!consume) {
            thrown.pickup = AbstractArrow.Pickup.DISALLOWED;
        }

        ModDataNBT data = PersistentDataCapability.getOrWarn(thrown);
        thrown.onRelease(maid, data);

        level.addFreshEntity(thrown);
        level.playSound(null, thrown, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0f, 1.0f);

        if (consume) {
            maid.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        } else {
            // 匠魂原版对玩家是把标枪整个丢出去；这里为了女仆能持续作战保留了标枪，
            // 所以改成每次投掷损耗 1 点耐久来平衡。
            ToolDamageUtil.damageAnimated(tool, 1, maid, EquipmentSlot.MAINHAND);
        }
    }

    /* ------------------------------------------------------------------ 喷流刃 */

    /** 喷流刃的蓄力 tick 数（匠魂用 1.5 倍时间系数）。 */
    public static int swasherChargeTicks(ToolStack tool, EntityMaid maid) {
        return drawTicks(tool, maid, 5, 1.5f);
    }

    private static ModifierEntry findSpitting(IToolStackView tool) {
        for (ModifierEntry entry : tool.getModifierList()) {
            if (SPITTING_ID.equals(entry.getId().toString())) {
                return entry;
            }
        }
        return null;
    }

    /** 工具储罐里是否装着「有效果」的流体（只有这种流体才能喷出弹射物）。 */
    private static boolean hasUsefulFluid(IToolStackView tool) {
        FluidStack fluid = ToolTankHelper.TANK_HELPER.getFluid(tool);
        if (fluid.isEmpty()) {
            return false;
        }
        try {
            return FluidEffectManager.INSTANCE.find(fluid.getFluid()).hasEffects();
        } catch (Exception e) {
            return true;
        }
    }

    /** 女仆背包里是否有一个装着流体的容器（匠魂的罐、灯笼、量器等）。 */
    private static boolean hasFluidContainer(EntityMaid maid) {
        CombinedInvWrapper inv = maid.getAvailableBackpackInv();
        for (int slot = 0; slot < inv.getSlots(); slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack.isEmpty() || stack.getCount() != 1) {
                continue;
            }
            IFluidHandlerItem handler = stack.copy()
                    .getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
            if (handler != null && !handler.getFluidInTank(0).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** 带缓存的「背包里是否有可用流体容器」。 */
    private static boolean hasFluidContainerCached(EntityMaid maid) {
        ContainerCache cache = CONTAINER_CACHE.get(maid);
        long now = maid.level().getGameTime();
        if (cache != null && now - cache.tick() < CONTAINER_CACHE_TICKS) {
            return cache.present();
        }
        boolean present = hasFluidContainer(maid);
        CONTAINER_CACHE.put(maid, new ContainerCache(now, present));
        return present;
    }

    /** 喷流刃现在能不能打（罐里有可用流体，或者能靠背包里的容器补上）。 */
    public static boolean canSwasherSpit(EntityMaid maid) {
        ItemStack stack = maid.getMainHandItem();
        if (stack.isEmpty()) {
            return false;
        }
        IToolStackView tool = ToolStack.from(stack);
        if (hasUsefulFluid(tool)) {
            return true;
        }
        if (!MaidTCRConfig.get(MaidTCRConfig.SWASHER_AUTO_REFILL, true)) {
            return false;
        }
        return hasFluidContainerCached(maid);
    }

    /**
     * 用背包里的流体容器给喷流刃补充流体（只在罐子为空时执行）。
     *
     * <p>任何带 Forge 流体容器能力的物品都行，因此匠魂的铜罐、灯笼、量器、储罐
     * 以及其它模组的容器都能用。</p>
     */
    public static boolean refillSwasher(EntityMaid maid, ToolStack tool) {
        if (!ToolTankHelper.TANK_HELPER.getFluid(tool).isEmpty()) {
            return false;
        }
        int capacity = ToolTankHelper.TANK_HELPER.getCapacity(tool);
        if (capacity <= 0) {
            return false;
        }
        CombinedInvWrapper inv = maid.getAvailableBackpackInv();
        for (int slot = 0; slot < inv.getSlots(); slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack.isEmpty() || stack.getCount() != 1) {
                continue;
            }
            IFluidHandlerItem handler = stack.copy()
                    .getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);
            if (handler == null || handler.getFluidInTank(0).isEmpty()) {
                continue;
            }
            FluidStack drained = handler.drain(capacity, IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty()) {
                continue;
            }
            ToolTankHelper.TANK_HELPER.setFluid(tool, drained.copy());
            ItemStack result = handler.getContainer();
            inv.setStackInSlot(slot, result.isEmpty() ? ItemStack.EMPTY : result);
            return true;
        }
        return false;
    }

    /**
     * 女仆用喷流刃喷出流体。
     *
     * <p>直接交给匠魂 {@code SpittingModule} 的 {@code onStoppedUsing} 执行，
     * 因此喷洒等级、多重射击、水阻力、{@code projectile_launch} 修饰符钩子、
     * 流体消耗与耐久损耗全部与本体一致。</p>
     *
     * <p>流体弹射物每刻受 -0.06 重力且水平速度按 0.99 衰减，轨迹与箭完全不同，
     * 所以这里按真实参数做数值弹道求解，临时抬高女仆的俯仰角来补偿下坠，喷完立即还原。</p>
     */
    public static void spitSwasher(EntityMaid maid, LivingEntity target) {
        ItemStack stack = maid.getMainHandItem();
        if (stack.isEmpty()) {
            return;
        }
        ToolStack tool = ToolStack.from(stack);
        if (tool.isBroken()) {
            return;
        }
        Level level = maid.level();
        if (level.isClientSide) {
            return;
        }

        if (!hasUsefulFluid(tool) && MaidTCRConfig.get(MaidTCRConfig.SWASHER_AUTO_REFILL, true)) {
            refillSwasher(maid, tool);
        }

        ModifierEntry spitting = findSpitting(tool);
        if (spitting == null) {
            return;
        }
        GeneralInteractionModifierHook hook = spitting.getHook(ModifierHooks.GENERAL_INTERACT);
        int charge = swasherChargeTicks(tool, maid);

        // onStoppedUsing 里会用 getToolCharge(蓄力时间 / 记录在工具上的 drawtime)，
        // 这里把 drawtime 设成我们的蓄力时间，等于满蓄力出手。
        tool.getPersistentData().putInt(GeneralInteractionModifierHook.KEY_DRAWTIME, charge);

        int duration = hook.getUseDuration(tool, spitting);
        // 喷流刃的弹射物要在实体朝向方向上生成，所以先算出补偿后的俯仰角、临时套到女仆身上
        float oldPitch = maid.getXRot();
        float speed = stat(tool, maid, ToolStats.VELOCITY) * 3.0f;
        float pitch = solvePitch(maid, target, speed, SWASHER_GRAVITY, SWASHER_SPAWN_OFFSET, AIM_TRIM_SWASHER);
        maid.setXRot(pitch);
        try {
            hook.onStoppedUsing(tool, spitting, maid, duration - charge);
        } finally {
            maid.setXRot(oldPitch);
        }
    }

    /* ------------------------------------------------------------------ 钓鱼竿（当远程武器用，当前不可用） */

    /**
     * 女仆抛出匠魂战斗鱼钩。
     *
     * <p><b>已禁用</b>：女仆用钓鱼竿攻击时会在玩家身上出现鱼线且不造成伤害，
     * 因此「钓鱼竿」工作模式不再挂任何 AI 行为，本方法与相关实体代码仅作保留。</p>
     */
    public static void castFishingHook(EntityMaid maid) {
        ItemStack rod = maid.getMainHandItem();
        if (rod.isEmpty()) {
            return;
        }
        ToolStack tool = ToolStack.from(rod);
        if (tool.isBroken()) {
            return;
        }
        Level level = maid.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        FakePlayer proxy = MaidFishingProxy.get(serverLevel, maid);
        if (proxy.fishing != null) {
            proxy.fishing.discard();
        }
        proxy.moveTo(maid.getX(), maid.getY(), maid.getZ(), maid.getYRot(), maid.getXRot());
        proxy.setYHeadRot(maid.getYHeadRot());
        proxy.setItemInHand(InteractionHand.MAIN_HAND, rod);

        int luck = Math.max(0, (int) stat(tool, maid, ToolStats.SEA_LUCK));
        int lure = Math.max(0, (int) stat(tool, maid, ToolStats.LURE));
        float velocity = stat(tool, maid, ToolStats.VELOCITY);
        float inaccuracy = ModifierUtil.getInaccuracy(tool, maid);

        MaidCombatFishingHook hook = MaidCombatFishingHook.cast(maid, proxy, level, luck, lure, velocity, inaccuracy);
        hook.setPower(stat(tool, maid, ToolStats.PROJECTILE_DAMAGE));
        try {
            hook.setMaterial(tool.getMaterial(tool.getVolatileData().getInt(FishingModule.HOOK_MATERIAL)).getVariant());
        } catch (Exception e) {
            MaidTConstructRangedTools.LOGGER.debug("Fishing hook material lookup failed", e);
        }

        EntityModifierCapability.EntityModifiers capability = EntityModifierCapability.getCapability(hook);
        if (capability != null) {
            capability.setModifiers(tool.getModifiers());
        }
        if (ModifierUtil.canPerformAction(tool, TinkerToolActions.GRAPPLE_HOOK)) {
            hook.setGrapple(ModifierUtil.canPerformAction(tool, TinkerToolActions.DRILL_ATTACK)
                    ? CombatFishingHook.GrappleType.DRILL : CombatFishingHook.GrappleType.DASH);
        }
        if (ModifierUtil.canPerformAction(tool, TinkerToolActions.ITEM_HOOK)) {
            hook.setCollecting();
        }

        ModDataNBT data = PersistentDataCapability.getOrWarn(hook);
        if (data != null) {
            for (ModifierEntry entry : tool.getModifierList()) {
                entry.getHook(ModifierHooks.PROJECTILE_LAUNCH)
                        .onProjectileLaunch(tool, entry, maid, ItemStack.EMPTY, hook, null, data, true);
            }
        }

        level.addFreshEntity(hook);
        level.playSound(null, maid.getX(), maid.getY(), maid.getZ(),
                SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5f,
                0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));

        ToolDamageUtil.damageAnimated(tool, 1, maid, EquipmentSlot.MAINHAND);
    }

    /* ------------------------------------------------------------------ 公共 */

    /** 让炽热、击退等修饰符有机会修改弹射物。 */
    public static void runProjectileLaunchHooks(IToolStackView tool, LivingEntity shooter, ItemStack ammo,
                                                AbstractArrow arrow, ModifierNBT modifiers, boolean isPrimary) {
        try {
            EntityModifierCapability.EntityModifiers capability = EntityModifierCapability.getCapability(arrow);
            if (capability != null) {
                capability.addModifiers(modifiers);
            }
            ModDataNBT arrowData = PersistentDataCapability.getOrWarn(arrow);
            if (arrowData == null) {
                return;
            }
            for (ModifierEntry entry : tool.getModifierList()) {
                ProjectileLaunchModifierHook hook = entry.getHook(ModifierHooks.PROJECTILE_LAUNCH);
                hook.onProjectileLaunch(tool, entry, shooter, ammo, arrow, arrow, arrowData, isPrimary);
            }
        } catch (Exception e) {
            MaidTConstructRangedTools.LOGGER.warn("Failed to run projectile launch modifier hooks", e);
        }
    }
}
