package com.maidtcr.maidtconstructrangedtools.compat;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import com.maidtcr.maidtconstructrangedtools.task.TaskTinkerCrossbow;
import com.maidtcr.maidtconstructrangedtools.task.TaskTinkerFishingRod;
import com.maidtcr.maidtconstructrangedtools.task.TaskTinkerJavelin;
import com.maidtcr.maidtconstructrangedtools.task.TaskTinkerLongbow;
import com.maidtcr.maidtconstructrangedtools.task.TaskTinkerShuriken;
import com.maidtcr.maidtconstructrangedtools.task.TaskTinkerSwasher;
import com.maidtcr.maidtconstructrangedtools.task.TaskTinkerThrowingAxe;

/**
 * 车万女仆的附属入口。
 *
 * <p>女仆模组会扫描所有模组 class 文件里的 {@link LittleMaidExtension} 注解，
 * 反射实例化（需要公有无参构造）后回调 {@link ILittleMaid} 的默认方法。</p>
 */
@LittleMaidExtension
public class LittleMaidCompat implements ILittleMaid {

    @Override
    public void addMaidTask(TaskManager manager) {
        // 注册顺序即工作模式界面里的顺序
        manager.add(new TaskTinkerCrossbow());
        // 「钓鱼竿」当前禁用（只占位，无 AI 行为），详见 TaskTinkerFishingRod
        manager.add(new TaskTinkerFishingRod());
        manager.add(new TaskTinkerJavelin());
        manager.add(new TaskTinkerLongbow());
        manager.add(new TaskTinkerShuriken());
        manager.add(new TaskTinkerThrowingAxe());
        manager.add(new TaskTinkerSwasher());
    }
}
