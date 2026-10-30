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

- [ ] 固定首个 Minecraft + loader + mappings 组合，并添加真实开发运行任务。
- [ ] 实现 Factory → Stage → Node → Edge、世界身份和工厂范围。
- [ ] 接入菜单同步事件、资源组件身份及槽位替换/会话关闭生命周期。
- [ ] ItemEntity 生命周期与可见性删失记录。
- [ ] 观察总线、提供者管理、观测区间、采样预算和错误隔离。
- [ ] Structured View 与共享节点身份的基础 Graph。
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
| Minecraft / loader / mappings 版本 | 不从研究报告推测；先保持 Core 可独立构建，再选定首个目标组合 |
| Fabric 与 NeoForge 推进顺序 | 保留双平台边界，不承诺同时支持所有版本 |
| LItemFinder API | 先定义消费契约，待核对上游真实公开接口后实现 |
| 历史存储 | 原型使用有限内存；SQLite 或分段文件在 V0.5 决定 |
| 精确数量与单位换算 | 当前监控用 double，精确遥测前决定数值与转换规则 |
| 发行许可证 | 尚未选择，不自动沿用相邻项目许可证 |
| 正式命名与 Mod ID | 仓库名 `factory-observability`；暂用指标 namespace `factory_observability`，loader metadata 未创建 |
