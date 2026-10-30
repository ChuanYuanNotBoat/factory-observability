# 路线图

## 初始化：已完成

- [x] Git 仓库、Gradle Wrapper 和 Java 21 多模块构建。
- [x] Core 资源身份、单位、证据、观测与库存指标基础。
- [x] 带容量上限的内存查询存储。
- [x] 可表达未知值的 scoped availability 契约。
- [x] 适配器/provider SPI 和细粒度能力枚举。
- [x] 菜单快照归一化桥接、合成数据演示与核心语义测试。
- [x] CI 配置、开发文档、原始研究报告归档。

## V0：Generic client-only foundation

- [x] 固定 Minecraft 1.21.1 / NeoForge 21.1.248 / 官方 Mojang mappings，提供真实开发运行任务与安装包。
- [x] 实现不可变 Factory → Stage → Node → Edge 模型、引用/循环验证、本地加盐世界身份。
- [x] 当前世界会话自动分为容器和掉落物两组；自定义工厂范围与分组编辑待补充。
- [x] 接入菜单同步状态、资源组件身份、槽位替换/会话关闭与读取失败生命周期。
- [x] ItemEntity 可见库存、位置和生命周期删失记录；不从消失事件推断拾取或销毁。
- [x] 有限观察总线、提供者生命周期、观测区间、采样预算和订阅者错误隔离。
- [x] 可筛选/排序的列表、共享节点身份的基础 Graph、用户声明连接、中英文界面。
- [x] 安装包客户端验证：真实菜单/实体同步，以及双语言、双缩放、双视图截图。
- [ ] 自定义工厂范围、Stage 编辑和稳定物理容器位置映射；当前节点按菜单会话区分。
- [ ] 对齐 LItemFinder 的稳定 availability API 并实现桥接。

验收：在没有服务器 Mod、没有原生适配器时，打开未知机器菜单能观察已同步库存；断开可见性后明确显示数据缺口，不伪造零值或吞吐量。

## V0.5：工业监控可用

净流量估计、WIP、满仓/耗尽 ETA 区间、持久化历史、时间桶和降采样、CSV/JSON/Markdown 导出、Discovery 原型。先定义观测窗口和 coverage 聚合，再实现速率估计。

## V1：多资源与首批深度适配

Fluid、Energy、Create、AE2、机器状态、经验生长模型和告警。Standard API 仅在实际有权威访问能力的环境使用，不能作为远程纯客户端的假设。

## V1.5：Server telemetry

精确 transfer、离屏统计、完整实体生命周期、客户端/服务端来源仲裁和去重。

## V2 及以后

物料平衡、LP/MILP、理论/实际对比、what-if、高级瓶颈诊断；之后扩展 Mekanism、TFC、GTCEu、第三方 SPI 和外部遥测导出。

## 待定决策

| 决策 | 当前处理 |
|---|---|
| Minecraft / loader / mappings 版本 | 首个目标已固定为 1.21.1 / NeoForge 21.1.248 / 官方 mappings |
| Fabric 与 NeoForge 推进顺序 | NeoForge 首先落地，Fabric 保留边界 |
| LItemFinder API | 当前相邻工程没有稳定的 ResourceAvailability 提供接口，保留消费契约，不访问内部索引 |
| 历史存储 | 原型使用有限内存；SQLite 或分段文件在 V0.5 决定 |
| 精确数量与单位换算 | 当前监控用 double，精确遥测前决定数值与转换规则 |
| 发行许可证 | 尚未选择，不自动沿用相邻项目许可证 |
| 正式命名与 Mod ID | 仓库名 `factory-observability`，Mod ID / 指标 namespace 为 `factory_observability` |
