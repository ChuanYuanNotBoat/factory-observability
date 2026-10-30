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

`platform-*`、`compat-*` 的原生模块和 LItemFinder 集成当前为显式预留模块，只有 `base` 插件。选择目标版本后才添加对应构建插件和 Mod 依赖，不应为了让目录看似完整生成不可运行的加载器元数据。

## 验证语义

修改模型、投影或存储时，重点检查：跨世界/维度/主体/资源变体隔离、单位匹配、未知数量、有限数值、覆盖率与可信度分离、查询边界和容量限制。新增测试应验证用户可见语义或重要边界。

真实 loader 接入之后，还需补充运行客户端、可选服务器缺席、菜单关闭、区块卸载、退出世界和未安装可选 Mod 的人工验证。普通 JVM 演示不能替代游戏内验证。

## 构建工具来源

Gradle Wrapper 由官方 Gradle 9.2.1 发行包生成，发行包 SHA-256 固定在 Wrapper 配置中。参考 [Gradle 9.2.1](https://docs.gradle.org/9.2.1/release-notes.html)、[Wrapper 校验](https://docs.gradle.org/current/userguide/gradle_wrapper.html#sec:verification) 和 [官方校验值](https://gradle.org/release-checksums/)。JUnit 使用 [官方 5.13.4 文档](https://docs.junit.org/5.13.4/user-guide/index.html)。

升级构建或测试依赖时，修改固定版本并执行 `build` 与演示；不要引入动态版本或本机缓存路径。
