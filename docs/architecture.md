# 架构与数据语义

## 方向与当前范围

目标是观察工厂运行，而非仅计算配方。LItemFinder 提供资源位置、数量和 availability，本项目负责流动、堆积、产线状态、预测与诊断。

初始化只实现一个可在普通 JVM 中验证的数据路径：

```mermaid
flowchart LR
    Input[合成菜单槽位数据] --> Bridge[minecraft-common: MenuObservationBridge]
    Bridge --> Observation[core: Observation + ResourceSnapshot]
    Observation --> Projector[SnapshotMetricProjector]
    Projector --> Store[InMemoryMetricStore]
    Store --> Demo[查询与演示输出]
```

这条路径尚无游戏 hook、观察总线、生产速率推断或持久化。快照桥接仅接受已归一化的同步数据，不读取客户端或服务器对象。

## 模块依赖

当前生产代码依赖：

```text
core                  -> JDK
compat-api            -> core
minecraft-common      -> core
test-fixtures         -> minecraft-common -> core
```

后续 loader 层负责事件、网络、路径、配置与生命周期，具体 Mod 适配器负责将其世界状态转成标准观测。它们可以依赖 `minecraft-common` 和 `compat-api`，不得反向成为 Core 的依赖。预留模块当前没有任何第三方 Mod 编译依赖。

Core 的 `check` 会验证生产源码的 import 边界和空的外部运行时依赖。未来引入数学或序列化库时，需明确调整这一规则并记录理由。

## 资源与主体身份

`ResourceId` 由 namespace、path、kind、qualifiers 组成。组件变体必须通过稳定的规范化值或哈希进入 qualifiers，不能只用物品注册名合并库存，也不能保留可变游戏对象。qualifiers 防御性复制并保持只读。

`ResourceStack` 验证有限非负数量以及资源种类与单位的一致性。单位是扩展值；mB、FE 等不能隐式互换。当前数量采用 `double`，用于监控值，不应据此实现精确交易账本；超大整数和精确小数的表示在持久化/精确遥测阶段决定。

`SubjectRef` 包含本地 worldId、dimensionId 和主体 id。不同世界、维度、槽位和资源变体的样本不能混在一起。worldId 不能直接使用明文服务器地址；实际 endpoint 加盐哈希、生命周期和持久化尚待 loader 层实现。

菜单槽位 id 必须包含菜单会话身份，避免复用 container id 或槽位序号混淆对象。玩家背包槽位不应被重复计入机器库存。槽位清空或资源替换时，调用者需对原资源发出显式零值；关菜单、卸载区块或丢失可见性时，不得伪造零值。

## 数据证据

| 字段 | 含义 |
|---|---|
| provenance | 服务器事件、客户端同步、原生适配或推断等数据来源 |
| quality | EXACT、OBSERVED、ESTIMATED、INFERRED、UNKNOWN |
| coverage | 提供者声明的目标时间/空间范围里，实际观测所覆盖的比例 |
| confidence | 在已有证据下，对观测或推断的信任程度 |

例如，只在一小时窗口中的 12 分钟看见工厂，窗口 coverage 为 0.2，即使同步槽位值的 confidence 为 1.0。当前证据只随单个快照传递，尚未记录完整观测区间，因此不能由两次快照推出两者之间一直可见，也不能从样本 coverage 直接得到任意查询窗口的 coverage。

菜单归一化只发出 `CLIENT_SYNC / OBSERVED`。它不宣称 `EXACT`、离屏连续监测或服务器能力可用。服务端观测、多个来源之间的去重/仲裁和精度合并均未实现。

## 指标与内存存储

初始 `MetricKey` 表示带资源身份和单位的数量指标；`inventory` 是槽位/主体所观测到的库存 gauge。`SnapshotMetricProjector` 原样保留证据，不从库存差生成 insert/extract、生产、消耗或总流量。

`MetricStore.query` 使用半开区间 `[start, end)`，返回按时间排序、只读且独立于后续写入的快照。没有样本时返回空列表，不补零、不插值。`InMemoryMetricStore` 的上限是全局样本数，按到达顺序淘汰；乱序写入也按到达顺序占用容量。它只用于原型和测试，没有持久化、时间桶、去重或分流保留策略。

## Availability 与适配器

`ResourceAvailability` 区分 observed、reachable、allocated、available，每个字段允许未知。available 由提供者依据范围和分配规则给出，不能在消费侧简单计算为 observed 减 allocated。该契约包含 scope、asOf、单位和 evidence；LItemFinder 的真实提供者尚未接入。

SPI 声明 provider 生命周期和细粒度能力。首次接入 loader 时，需实现 adapter 发现、重复 id 检查、异常隔离、start/stop 对称性和卸载清理。能力声明只表示提供者可产生的观测，不能自动提升每个样本的质量。

## 后续运行约束

- 事件优先，变化检测和低频采样补充；避免每 tick 全量扫描。
- 游戏对象在游戏线程读取，异步分析只处理不可变 DTO。
- ItemEntity 丢失可见性属于删失观测，不能直接归类为拾取或销毁。
- 流体、能源、物品不直接相加；WIP 需要按资源或目标产品等价量统计。
- 未知 Mod 的推断需携带 hypothesis、证据与 confidence，用户声明应可覆盖推断。
- 历史存储和导出必须有 schema version；数据库、迁移和世界身份版本在实现前明确。
