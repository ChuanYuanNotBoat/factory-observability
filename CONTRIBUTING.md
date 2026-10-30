# 开发说明

## 环境与构建

使用 JDK 21 和仓库内 Gradle Wrapper。当前固定 Gradle 9.2.1、JUnit 5.13.4，不需要安装全局 Gradle。

```powershell
.\gradlew.bat build
.\gradlew.bat :test-fixtures:run
```

`build` 在根项目聚合所有子项目的 `assemble` 和 `check`。`check` 包含 JUnit 测试与 Core 隔离检查。Linux/macOS 使用 `./gradlew`。

如果系统默认 Java 版本较旧，可在当前终端设置 `JAVA_HOME` 为已安装的 JDK 21 路径，然后重试。不要将本机路径提交到 `gradle.properties`，也不要在任务中修改系统级 Java 配置。

## 模块与依赖

生产 Core 当前只允许 JDK。外部适配必须放在相应模块，不得把 `ItemStack`、`BlockPos`、loader event 或任意 Mod 对象传入 Core。记录 DTO 时复制可变数据。

NeoForge 已接入 ModDevGradle 2.0.147、NeoForge 21.1.248 和 Minecraft 1.21.1 官方 mappings。`platform-fabric`、各原生 `compat-*` 和 LItemFinder 集成仍为预留模块。不要将 NeoForge 类型扩散到 common 或 Core。

NeoForge 运行任务：

```powershell
.\gradlew.bat :platform-neoforge:runClient
.\gradlew.bat :platform-neoforge:verifyModJar
.\gradlew.bat :platform-neoforge:verifySmokeClient
```

`verifySmokeClient` 会启动独立客户端，在 `platform-neoforge/run-smoke/` 下创建新的测试世界、打开测试容器并验证同步；随后捕获中英文、GUI 缩放 2/3、列表/图两种视图共八张截图并正常退出。只允许测试夹具操作这个运行目录，测试 addon 不得进入安装包。该任务需要图形环境，不属于普通 CI 的 `build`。

依赖已完整下载后可使用 `--offline` 避免远程依赖解析。不要为了离线构建把缓存路径或第三方游戏文件提交到仓库。

## 验证语义

修改模型、投影或存储时，重点检查：跨世界/维度/主体/资源变体隔离、单位匹配、未知数量、有限数值、覆盖率与可信度分离、查询边界和容量限制。新增测试应验证用户可见语义或重要边界。

普通 JVM 测试覆盖模型和数据语义；独立客户端验证覆盖安装包加载、真实容器同步、组件分离、玩家库存排除、资源替换和可见性中断。真正的远程纯原版服务器握手、切换维度、大型未知 Mod 菜单及满预算长时间运行仍需专项验证，不能用单人烟雾测试代替。

## 构建工具来源

Gradle Wrapper 由官方 Gradle 9.2.1 发行包生成，发行包 SHA-256 固定在 Wrapper 配置中。参考 [Gradle 9.2.1](https://docs.gradle.org/9.2.1/release-notes.html)、[Wrapper 校验](https://docs.gradle.org/current/userguide/gradle_wrapper.html#sec:verification) 和 [官方校验值](https://gradle.org/release-checksums/)。JUnit 使用 [官方 5.13.4 文档](https://docs.junit.org/5.13.4/user-guide/index.html)。

升级构建或测试依赖时，修改固定版本并执行 `build` 与演示；不要引入动态版本或本机缓存路径。
