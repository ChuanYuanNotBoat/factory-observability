# Factory Helper / Factory Observability

面向 Minecraft 生电与工业整合包的辅助 Mod。优先提供简单、直观的操作：打开箱子，点“查看物品记录”，直接看图标和数量。玩家可以给记录改名、备注物品去向。资源与观测模型保留在后台，支持后续扩展。

当前提供 **V0 客户端辅助版本**，目标为 **Minecraft 1.21.1 + NeoForge 21.1.248**。可以记录容器库存、区分组件变体和记录附近掉落物；通过“箱子 / 机器”“掉落物”“物品去向”三个入口使用。服务器无需安装本 Mod。

Factory/Stage 当前按世界会话自动组织为“容器会话 / 掉落物”两组。自定义工厂范围、深度 Mod 适配、LItemFinder 实际联动和持久化历史仍在后续阶段。

## 安装与使用

构建后的客户端 Mod：`platform-neoforge/build/libs/factory-observability-neoforge-1.21.1-0.1.0-SNAPSHOT.jar`。将它放入对应 NeoForge 实例的 `mods/` 目录。不要安装 `sources` JAR、普通库 JAR 或测试用 smoke JAR。

1. 打开箱子或机器，点界面左上角的 **“查看物品记录”**。助手会先记下当前物品，再打开记录页，无需等满一秒。
2. 左边选一条箱子记录，右边直接看物品图标和数量。可搜索物品或箱子，点“改名”给助手里的记录起个好认的名字；不会改动世界里的容器。
3. 若想备注物品去向，点一种物品 → **“标记这个物品的去向”** → 选择另一个已记录的箱子 → 确认。可在“物品去向”里取消或撤销；这些是手动备注，不会自动搬运物品。
4. 随时点右上角 **“怎么用”** 查看三步指引。游戏中也可按 **O** 或使用客户端命令 **`/factoryobs`** 打开。快捷键可在控制设置修改，不会拦截界面里的文字输入。

打开助手会关闭当前容器菜单，并显示“上次记录”。再打开容器会留下新的记录；同一物理容器的多次打开目前分别记录。掉落物放在独立页，离开视野后保留最后数量、去向未知。退出世界后清空内存历史，改名和去向备注也只在本次会话中保留。

首次启动会生成 `config/factory-observability.json`，可调整采样周期、槽位与实体数量上限、队列和历史容量。默认每 20 个客户端 tick 采样，最多 256 个非玩家菜单槽位、128 个掉落物、512 个节点和 20,000 个库存样本。超过槽位预算时显示部分覆盖；超过实体预算时累计跳过数量。不会每 tick 扫描全部世界实体。

本地世界身份使用实例内 `factory-observability/world-identity.salt` 加盐生成，不将明文服务器地址写入观测数据。配置读取失败会回退到有上限的默认值。

## 快速开始

需要完整 **JDK 21**，并将 `JAVA_HOME` 指向它；不需要安装全局 Gradle。首次构建需要联网下载 Gradle、Minecraft 开发依赖和测试依赖；运行客户端时还会下载游戏资源。构建不自动安装 JDK。

Windows PowerShell：

```powershell
.\gradlew.bat build
.\gradlew.bat :test-fixtures:run
.\gradlew.bat :platform-neoforge:runClient
```

Linux / macOS：

```sh
./gradlew build
./gradlew :test-fixtures:run
./gradlew :platform-neoforge:runClient
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
| `compat-api` | 适配器/provider SPI、细粒度能力、可清理且故障隔离的 provider 生命周期 |
| `minecraft-common` | 菜单会话、有限节点索引、世界身份、共用观测会话与分析管线 |
| `test-fixtures` | 可执行的合成数据演示 |
| `platform-neoforge` | 真实菜单/实体事件、组件指纹、容器入口、图标库存列表、改名/去向引导、按键、配置和客户端验证 |
| `platform-fabric` | 预留模块，尚未接入 Fabric |
| `compat-create` / `compat-ae2` | 预留原生适配器模块 |
| `compat-mekanism` / `compat-tfc` / `compat-gtceu` | 预留后续适配器模块 |
| `integration-litemfinder` | 预留 API 联动模块；不读取其他项目的内部索引 |

预留模块只使用 Gradle `base` 插件，不生成空的“Mod JAR”。NeoForge 安装包直接包含本项目的 Core、common 和 SPI 类，不需要额外安装这些库。

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
- [当前版本的验证记录与限制](docs/verification/v0.md)
- [用户提供的原始研究报告](docs/research/design-report.md)

原始报告作为设计输入原样保存，其中的生成式引用标记没有可解析的来源映射，不代表已在本仓库验证的兼容性承诺。实现依赖的版本和文档应在对应阶段固定。

项目发行许可证尚未选择；Gradle Wrapper 的第三方声明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
