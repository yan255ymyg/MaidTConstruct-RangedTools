# Maid TConstruct: Ranged Tools / 女仆匠魂：远程工具

为 **Minecraft 1.20.1 + Forge 47.x** 编写的附属模组，把 **[匠魂 3](https://modrinth.com/mod/tinkers-construct)**（Tinkers' Construct 3.12）
的远程工具接进 **[车万女仆](https://modrinth.com/mod/touhou-little-maid)**（Touhou Little Maid 1.5.3）的工作模式系统。

| 项 | 值 |
|---|---|
| mod id | `maidtconstructrangedtools` |
| 显示名 | Maid TConstruct: Ranged Tools |
| 版本 | 1.0.0 |
| 加载器 | Forge（`javafml`），`loaderVersion [47,)` |
| 游戏版本 | 1.20.1 |
| 编译目标 | Java 17 |
| 授权 | MIT |

> **开发说明**
>
> 本模组由 **[DeepSeek Harness](https://github.com/deepseek-ai)（模型 deepseek-v4-flash）** 全程编写：
> 需求分析、对车万女仆 / 匠魂 3 的反编译与 API 调研、代码实现、构建环境排障，以及本文档。
>
> **当前版本 1.0.0 是第一个正式版。**

## 工作模式一览

注册顺序即女仆界面里的显示顺序。

| # | 工作模式 | 任务 ID | 使用的工具 | 要求 | 行为 |
|---|---|---|---|---|---|
| 1 | 弩 | `...:crossbow` | `tconstruct:crossbow`、`tconstruct:war_pick`（战镐） | 主手持有 | 先装填（弹药存进弩的 `tconstruct:crossbow_ammo`）再发射；装填耗时由 `draw_speed` 决定 |
| 2 | 钓鱼竿（不可用） | `...:fishing_rod` | `tconstruct:fishing_rod` | — | **已禁用**：图标为屏障方块，选中后没有任何效果，详见下文 |
| 3 | 标枪 | `...:javelin` | `tconstruct:javelin` | 主手持有 | 蓄力后投出匠魂标枪（`ThrownTool`），力度 = charge × velocity × 2；每次投掷损耗 1 点耐久 |
| 4 | 长弓 | `...:longbow` | `tconstruct:longbow` | 主手持有 | 拉弓射箭，蓄力时间由 `draw_speed` 决定 |
| 5 | 手里剑 | `...:shuriken` | `tconstruct:shuriken` | **背包内有** | 有敌人时自动换到主手再投掷 |
| 6 | 投斧 | `...:throwing_axe` | `tconstruct:throwing_axe` | **背包内有** | 同上（匠魂里投斧与手里剑共用投掷实现） |
| 7 | 喷流刃 | `...:swasher` | `tconstruct:swasher` | 主手持有 | 蓄力后喷出流体弹射物；空罐时自动用背包里的流体容器补满 |

**工作模式图标**统一使用「**骑士史莱姆（`tconstruct:knightslime`）材质**」的对应工具：
由匠魂自己的 `ToolBuildHandler.createSingleMaterial(...)` 构造，
骑士史莱姆没有对应部件属性的部位由匠魂自动回退到该部位首个可用材质。

<img width="127" height="203" alt="image" src="https://github.com/user-attachments/assets/32e61014-9de0-4c5e-9efa-abc67bac113d" />


## 各项细节

### 弹药：长弓 / 弩不要求背包里有箭

弹药解析完全交给匠魂自己的 `BowAmmoModifierHook`：

* 装了能力强化「**晶簇**」（`InfinityModule`）等无限箭强化时，女仆会在背包没有箭的情况下
  按匠魂的规则射出**水晶箭**并额外消耗 **4 点耐久/支**（数值与机制都由匠魂结算）；
* 顺带支持多重射击、烟花（弩）等匠魂特性；
* 只有「既没有强化、背包里也没有箭」时才射不出来；
* 女仆不是玩家，`LivingEntity#getProjectile` 对她恒返回空，匠魂的弹药搜索看不到 TLM 的背包
  （那是 Forge 物品栏）。本模组额外加了一层「女仆背包」兜底，是否消耗实体箭由配置决定。

### 瞄准：数值弹道求解

#### 事先说明，由于种种原因，女仆使用标枪、手里剑和投斧总是很难命中目标（笑

三种投掷/喷射类武器都不再使用「速度乘经验系数」的粗略估角，而是按**匠魂/原版真实的每刻
重力与阻力**做逐刻数值模拟，在女仆当前朝向附近搜索落点误差最小的俯仰角：

| 弹射物 | 实体 | 每刻重力 | 阻力 | 生成点 | 发射初速 | 有效射程 |
|---|---|---|---|---|---|---|
| 手里剑 | `ThrownShuriken` | **-0.03** | 0.99 | 眼睛下方 0.1 | `velocity`（匠魂基础 1.5） | ≈ 52 格 |
| 投斧 | `ThrownShuriken` | **-0.03** | 0.99 | 眼睛下方 0.1 | `velocity`（匠魂基础 0.75） | ≈ 16 格 |
| 标枪 | `ThrownTool`（= 原版 `AbstractArrow`） | **-0.05** | 0.99 | **脚下再下 0.1** | `velocity × 2` | ≈ 59 格 |
| 喷流刃 | `FluidEffectProjectile` | **-0.06** | 0.99 | 眼睛下方 0.1 | `velocity × 3` | > 80 格 |

模拟的积分顺序与 Minecraft 一致（先位移 → 再乘阻力 → 最后加重力），
目标高度取目标眼睛相对**弹射物生成点**的高度差。
搜索区间是女仆当前朝向的 **+60°（抬头）~ -20°（低头）**，步长 0.5°。

因为初速直接读工具属性，求解器会自然区分轻重：**手里剑轻、初速快、重力小，只需要很小
的修正；投斧和标枪又慢又重（标枪还从脚下生成、重力更大），必须抬得更多。**

弹道求解给的是理论值，实机还有碰撞盒、随机散布、生成点取整等偏差，所以在求解结果上
再叠加一个**各武器独立的经验修正量**（`AIM_TRIM_*`，正数压低、负数抬高）：

| 武器 | 修正量 | 说明 |
|---|---|---|
| 手里剑 | **+1.5°** | 略微压低 |
| 标枪 | **+2.0°** | 略微压低 |
| 投斧 | **−3.5°** | 略微抬高 |
| 喷流刃 | 0° | 不修正 |

> 调这个值只需要改 `TinkerRangedHelper` 里的 `AIM_TRIM_*` 常量。
> 投斧的初速只有 0.75，抬到极限也只能打到十几格，因此它的**默认射程设为 16 格**（而非 48）；
> 默认值可以在女仆全局设置 →「女仆匠魂：远程工具」里随时调整。

### 手里剑 / 投斧：背包里有就行

这两种是消耗品，所以不要求主手拿着：

* 只要有敌人、主手不是对应工具、而背包里有，就通过女仆自己的
  `TaskEquipUtil.tryEquipFromBackpack` 把工具换到主手（换装行为优先级 4，高于攻击行为的 5），
  这部分思路与农场模式一致；
* 投掷时按弹道求解自动抬起合适的角度，抵消弹射物下坠、提高命中率；
* 每次投掷消耗 1 个（可配置）。

### 标枪：耐久

匠魂原版对玩家是把标枪整个丢出去。为了女仆能持续作战，默认**保留标枪**（可配置为消耗），
代价是**每次投掷损耗 1 点耐久**。

### 喷流刃

* 远程部分直接调用匠魂 `SpittingModule#onStoppedUsing`，所以喷洒等级、多重射击、水阻力、
  `projectile_launch` 修饰符钩子、流体消耗与耐久损耗都与本体一致；
* 流体弹射物（`FluidEffectProjectile`）每刻受 **-0.06 重力**、水平速度按 **0.99** 衰减，
  轨迹和箭完全不同，因此开火前会用弹道求解算出俯仰角并短暂应用到女仆身上，喷完立即还原；
* 罐内没有流体、而背包里有装着流体的容器时自动补满：任何带 Forge 流体容器能力的物品都行，
  因此匠魂的铜罐、灯笼、量器、储罐以及其它模组的容器都能用（可在设置里关闭）；
* **只有「有效果」的流体才能喷出弹射物**：岩浆、牛奶、蜂蜜、史莱姆、熔融金属等；
  **水没有效果，喷不出来**；
* 不包含近战用法。

### 钓鱼竿：已禁用（保留代码）

女仆用匠魂钓鱼竿攻击时，会在玩家身上出现**鱼线**且**不造成伤害**
（匠魂战斗鱼钩本质是原版 `FishingHook`，其 `tick()` 要求“主人”是玩家），实用性不强，
因此该工作模式被停用：

* 仍然出现在工作模式列表里，图标换成**屏障方块**，说明文本写明有 bug、不可使用；
* 不挂载任何 AI 行为 → **选中后女仆不会有任何效果**；
* `TaskTinkerFishingRod#performRangedAttack`、`TinkerRangedHelper#castFishingHook`、
  `MaidCombatFishingHook`、假玩家代理与实体注册等代码**全部保留**，方便以后修复；
* 想用匠魂钓鱼竿**钓鱼**请选择女仆自带的「钓鱼」工作模式 —— 本模组已为它加了匠魂适配
  （`TinkerFishingType`：把匠魂的 `海之眷顾 / 诱饵` 映射成女仆钓钩的幸运与咬钩速度）。

## 配置

### 女仆全局设置界面

在女仆的全局设置界面中新增分类 **「女仆匠魂：远程工具」**（接入 Cloth Config，
与「女仆三叉戟范围」等设置并列），包含 7 项射程与 1 项开关：

| 设置项 | 默认 | 说明 |
|---|---|---|
| 女仆弩范围 | 64 | 弩 / 战镐 |
| 女仆钓鱼竿范围 | 48 | 该工作模式已禁用，此项保留 |
| 女仆标枪范围 | 48 | |
| 女仆长弓范围 | 48 | |
| 女仆手里剑范围 | 48 | |
| 女仆投斧范围 | 16 | 投斧初速低、抬到极限也只有十几格，故默认给短一些 |
| 女仆喷流刃范围 | 48 | |
| 喷流刃自动填充 | 开 | 空罐时是否自动用背包里的流体容器补充 |

射程范围均为 8 ~ 192，与女仆模组自带远程范围一致。

### 配置文件

`config/maidtconstructrangedtools-common.toml`：

| 键 | 默认 | 说明 |
|---|---|---|
| `ranged_tools.consume.consume_arrow` | `true` | 长弓/弩是否消耗女仆背包里的实体箭 |
| `ranged_tools.consume.consume_shuriken` | `true` | 手里剑投掷是否消耗 |
| `ranged_tools.consume.consume_throwing_axe` | `true` | 投斧投掷是否消耗 |
| `ranged_tools.consume.consume_javelin` | `false` | 标枪投掷是否消耗 |
| `ranged_tools.range.*` | 见上表 | 与设置界面同源 |
| `ranged_tools.swasher.auto_refill` | `true` | 喷流刃自动填充 |

## 构建

```sh
# Windows
gradlew.bat build
# Linux / macOS
./gradlew build
```

产物：`build/libs/maidtconstructrangedtools-1.0.0.jar`，放进 `mods/` 即可。

依赖通过 **[Modrinth Maven](https://api.modrinth.com/maven)** 自动拉取（版本已在 `build.gradle` 里钉死），
所以 `git clone` 之后直接构建即可，**无需手动准备任何 jar**。
如果你的环境访问不了 Modrinth，可以把模组 jar 放进 `libs/`（详见 [libs/README.md](libs/README.md)），
`build.gradle` 会优先使用本地 jar。

> 本机（`D:\Minecraft-PCL2`）构建时踩到的环境问题记录在仓库外的 `_过程文件/构建说明.txt`：
> Gradle 发行版不能放在含中文的路径下，foojay 自动装 JDK 17 走的 GitHub 不可达，
> 因此本机把 `GRADLE_USER_HOME` 指到了 ASCII 路径并手动放了一份 JDK 17。

## 依赖

运行时必需：`touhou_little_maid` ≥ 1.5.0、`tconstruct` ≥ 3.10.0（`mantle` 由匠魂自带依赖）。
可选：`cloth_config`（仅用于女仆全局设置界面里的分类，缺省时其它功能照常）。

## 目录结构

```
src/main/java/com/maidtcr/maidtconstructrangedtools/
├── MaidTConstructRangedTools.java            @Mod 主类
├── MaidTCRConfig.java                        Forge 通用配置
├── ai/
│   ├── MaidTinkerShootTask.java              “蓄力—放”射击 AI
│   └── MaidTinkerEquipTask.java              从背包换武器的一次性 AI
├── client/
│   ├── MaidClothConfig.java                  女仆全局设置界面里的分类
│   ├── ClientPlayerAccess.java               仅客户端加载的本地玩家读取
│   └── ClientSetup.java                      注册鱼钩渲染器
├── compat/
│   ├── LittleMaidCompat.java                 @LittleMaidExtension 附属入口
│   └── TinkerFishingType.java                匠魂钓鱼竿接入女仆「钓鱼」模式
├── entity/
│   ├── InitEntities.java                     实体类型注册
│   └── MaidCombatFishingHook.java            女仆版匠魂战斗鱼钩（当前不可达）
├── task/
│   ├── AbstractTinkerRangedAttackTask.java   远程工作模式公共实现
│   ├── TaskTinkerCrossbow.java
│   ├── TaskTinkerFishingRod.java             已禁用，仅占位
│   ├── TaskTinkerJavelin.java
│   ├── TaskTinkerLongbow.java
│   ├── TaskTinkerShuriken.java
│   ├── TaskTinkerThrowingAxe.java
│   ├── TaskTinkerSwasher.java
│   ├── TinkerRangedHelper.java               各工具的发射逻辑
│   └── TinkerToolLookup.java                 工具物品与骑士史莱姆图标缓存
└── util/
    └── MaidFishingProxy.java                 鱼钩用的假玩家代理（当前不可达）
```

## 已知限制

* 射击直接调用工作模式的 `performRangedAttack`，因此女仆本体里针对饰品改写远程攻击的那层逻辑不参与；
* 「钓鱼竿」工作模式已禁用（原因见上）；
* 本模组针对 Minecraft 1.20.1 + Forge 47.x + 车万女仆 1.5.3 + 匠魂 3.12 开发；
  其它版本组合如遇问题，请附带 `latest.log` 反馈。
