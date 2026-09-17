package com.maidtcr.maidtconstructrangedtools.util;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 每个女仆一个“假玩家”代理。
 *
 * <p>匠魂战斗鱼钩底层是原版 {@code FishingHook}，它在 {@code tick()} 里要求
 * {@code getPlayerOwner()} 非空，并且会用这个玩家做“主人手持可抛竿物品 / 距离不超过 32 格”判断。
 * 女仆不是玩家，所以这里给她配一个位置同步、主手放着同一把鱼竿的假玩家；
 * 实体真正的 owner 仍然是女仆，伤害与拉拽都算女仆的。</p>
 */
public final class MaidFishingProxy {

    private static final String PROXY_NAME = "[MaidTCR]";

    private MaidFishingProxy() {
    }

    public static FakePlayer get(ServerLevel level, EntityMaid maid) {
        UUID id = UUID.nameUUIDFromBytes(
                (maid.getUUID() + ":maidtconstructrangedtools").getBytes(StandardCharsets.UTF_8));
        return FakePlayerFactory.get(level, new GameProfile(id, PROXY_NAME));
    }
}
