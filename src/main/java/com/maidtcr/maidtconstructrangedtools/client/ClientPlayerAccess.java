package com.maidtcr.maidtconstructrangedtools.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.concurrent.Callable;

/** 只在客户端加载的取本地玩家工具（服务端调用时不会加载客户端类）。 */
public final class ClientPlayerAccess {

    private ClientPlayerAccess() {
    }

    @Nullable
    public static Player getClientPlayer() {
        return DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> ClientPlayerHolder.INSTANCE);
    }

    private static final class ClientPlayerHolder implements Callable<Player> {
        private static final ClientPlayerHolder INSTANCE = new ClientPlayerHolder();

        @Override
        public Player call() {
            return Minecraft.getInstance().player;
        }
    }
}
