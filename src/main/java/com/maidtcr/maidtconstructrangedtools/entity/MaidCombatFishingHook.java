package com.maidtcr.maidtconstructrangedtools.entity;

import com.maidtcr.maidtconstructrangedtools.client.ClientPlayerAccess;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import slimeknights.tconstruct.tools.entity.CombatFishingHook;

import javax.annotation.Nullable;

/**
 * 匠魂战斗鱼钩的女仆版本。
 *
 * <p>原版 {@code FishingHook}（匠魂的 {@link CombatFishingHook} 继承自它）在 {@code tick()}
 * 里要求 {@code getPlayerOwner()} 非空，否则会立刻自毁；而女仆不是玩家。这里的做法是：</p>
 * <ul>
 *   <li>实体真正的 owner 是<b>女仆</b>，因此伤害结算、拉拽、工具损耗都算在女仆头上；</li>
 *   <li>{@code getPlayerOwner()} 覆写为返回一个与女仆位置同步的假玩家代理，
 *       让原版的“主人手持可抛竿物品 / 距离不超过 32 格”等判断照常工作；</li>
 *   <li>客户端上代理不存在，于是在没有玩家主人时回退到本地玩家——原版逻辑在客户端
 *       只把这个返回值用于一个非空判断，因此这样做是安全的，而且能让鱼钩正常渲染。</li>
 * </ul>
 */
public class MaidCombatFishingHook extends CombatFishingHook {

    private static final float PI = (float) Math.PI;

    /** 服务端的假玩家代理；客户端为 null。 */
    @Nullable
    private Player proxy;

    public MaidCombatFishingHook(EntityType<MaidCombatFishingHook> type, Level level) {
        super(type, level);
    }

    /** 以女仆为发射者抛竿。 */
    public static MaidCombatFishingHook cast(LivingEntity shooter, Player proxy, Level level,
                                             int luck, int lure, float velocity, float inaccuracy) {
        MaidCombatFishingHook hook = new MaidCombatFishingHook(InitEntities.MAID_FISHING_HOOK.get(), level);
        hook.proxy = proxy;
        hook.launch(shooter, velocity, inaccuracy);
        // setOwner 内部会调用 updateOwnerInfo，从而把 proxy.fishing 指向本实体
        hook.setOwner(shooter);
        return hook;
    }

    @Override
    @Nullable
    public Player getPlayerOwner() {
        if (this.proxy != null) {
            return this.proxy;
        }
        return ClientPlayerAccess.getClientPlayer();
    }

    /** 复刻匠魂 {@code CombatFishingHook(Player, ...)} 的初始位置与速度计算。 */
    private void launch(LivingEntity shooter, float velocity, float inaccuracy) {
        float xRot = shooter.getXRot();
        float yRot = shooter.getYRot();
        float yAngle = (-yRot * PI / 180f) - PI;
        float dz = Mth.cos(yAngle);
        float dx = Mth.sin(yAngle);
        this.moveTo(shooter.getX() - dx * 0.3, shooter.getEyeY(), shooter.getZ() - dz * 0.3, yRot, xRot);

        float xAngle = -xRot * (PI / 180f);
        float yCos = -Mth.cos(xAngle);
        float ySin = Mth.sin(xAngle);
        Vec3 delta = new Vec3(-dx, Mth.clamp(-ySin / yCos, -5f, 5f), -dz);
        double length = delta.length();
        double maxRandom = 0.03 * inaccuracy * inaccuracy;
        delta = delta.multiply(
                velocity / length + this.random.triangle(0.5, maxRandom),
                velocity / length + this.random.triangle(0.5, maxRandom),
                velocity / length + this.random.triangle(0.5, maxRandom));
        this.setDeltaMovement(delta);
        this.setYRot((float) (Mth.atan2(delta.x, delta.z) * (180 / PI)));
        this.setXRot((float) (Mth.atan2(delta.y, delta.horizontalDistance()) * (180 / PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }
}
