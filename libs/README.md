# libs/

**正常情况下这个目录是空的，不需要放任何东西。**

`build.gradle` 会优先检查本目录：只要这里有 `*.jar`，就用它们做 `compileOnly`；
否则自动从 [Modrinth Maven](https://api.modrinth.com/maven) 拉取下面这些依赖（已钉好版本 id）：

| 依赖 | 坐标 |
|---|---|
| 车万女仆 | `maven.modrinth:touhou-little-maid:g1SKoGQJ`（1.5.3-forge+mc1.20.1） |
| 匠魂 3 | `maven.modrinth:tinkers-construct:kfptyb1D`（3.12.0.220） |
| 地幔（匠魂前置） | `maven.modrinth:mantle:37ccXi7G`（1.11.117） |
| Cloth Config（可选，仅配置界面编译用） | `maven.modrinth:cloth-config:t8TXrZvZ`（11.1.136+forge） |

也就是说 `git clone` 之后直接 `gradlew build` 就能编译，**无需手动准备任何 jar**。

## 什么时候需要往这里放 jar

* 网络访问不了 Modrinth，但本地整合包里已经有这些模组；
* 想针对某个特定的模组构建版本做验证。

放进来即可，文件名随意，`build.gradle` 会把本目录下的所有 `*.jar` 都加进编译类路径。
这些 jar **只用于编译**，不会被打进产物，也已被 `.gitignore` 忽略（不要提交到仓库）。

> 提示：需要「Mojang 官方映射」版的类。匠魂、女娲（TLM）等使用 ForgeGradle 构建的正式发布包
> 就满足要求，直接拿来当 `compileOnly` 即可（1.20.1 起 Forge 运行期使用官方映射，
> 模组自身类未混淆）。
