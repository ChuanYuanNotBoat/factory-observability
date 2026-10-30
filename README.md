# Factory Observability

面向 Minecraft 生电与工业整合包的工厂可观测性平台：观察资源库存与流动，逐步建立运行监控、推断、预测和诊断能力。

当前处于 **V0 初始化阶段**。已提供独立 Java 核心、适配器 SPI、菜单快照归一化桥接，以及无需 Minecraft 的合成数据演示。**尚未生成可放入 `mods/` 的 Mod，也没有游戏内界面或真实游戏事件接入。**

## 快速开始

需要完整 **JDK 21**，并将 `JAVA_HOME` 指向它；不需要安装 Gradle 或 Minecraft。首次构建需要联网下载 Gradle 和测试依赖。构建不自动安装 JDK。

Windows PowerShell：

```powershell
.\gradlew.bat build
.\gradlew.bat :test-fixtures:run
```

Linux / macOS：

```sh
./gradlew build
./gradlew :test-fixtures:run
```

演示输入同一菜单槽位的两次合成同步快照：32 → 48 个铁锭。输出保留 `CLIENT_SYNC / OBSERVED`、coverage 和 confidence。差值只代表库存净变化；生产、消耗、真实流量仍然未知。演示不会启动或连接游戏。

常用检查：

```powershell
.\gradlew.bat check
.\gradlew.bat :core:verifyCoreIsolation
```

## 模块状态

| 模块 | 当前内容 |
|---|---|
| `core` | JDK-only 资源模型、观测、证据、库存指标、有容量限制的内存存储、availability 契约 |
| `compat-api` | 适配器和观测提供者 SPI、细粒度能力声明 |
| `minecraft-common` | 无游戏类型依赖的菜单槽位快照归一化桥接；真实 `ItemStack` 等映射待实现 |
| `test-fixtures` | 可执行的合成数据演示 |
| `platform-fabric` / `platform-neoforge` | 预留模块；尚未选择 MC/loader 版本和接入构建插件 |
| `compat-create` / `compat-ae2` | 预留原生适配器模块 |
| `compat-mekanism` / `compat-tfc` / `compat-gtceu` | 预留后续适配器模块 |
| `integration-litemfinder` | 预留 API 联动模块；不读取其他项目的内部索引 |

预留模块只使用 Gradle `base` 插件，不生成空的“Mod JAR”。当前库 JAR 也不包含加载器元数据。

## 设计原则

- **资源优先**：Item、Fluid、Energy、Chemical、Heat、Custom 均有独立身份与单位。
- **客户端先行**：服务端支持用于提升精度，不作为基础功能的前提。
- **证据可追溯**：provenance、quality、coverage、confidence 分别记录。高置信度不提高观测覆盖率。
- **未知保持未知**：availability 中未观测的数量使用空值，不能当作零；库存净变化不能冒充总吞吐量。
- **边界清晰**：Core 不引用 Minecraft、loader 或其他 Mod 类型；游戏线程只向分析侧传递不可变 DTO。
- **逐级增强**：Generic → Standard API → Semantic → Native → Server Telemetry。

## 文档

- [架构与数据语义](docs/architecture.md)
- [阶段路线图与待定决策](docs/roadmap.md)
- [开发说明与验证](CONTRIBUTING.md)
- [用户提供的原始研究报告](docs/research/design-report.md)

原始报告作为设计输入原样保存，其中的生成式引用标记没有可解析的来源映射，不代表已在本仓库验证的兼容性承诺。实现依赖的版本和文档应在对应阶段固定。

项目发行许可证尚未选择；Gradle Wrapper 的第三方声明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
