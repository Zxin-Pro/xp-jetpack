# 增量接口约定文档 · XP Survival Suite

> **产出方：架构子代理** ｜ 版本 v1.0 ｜ 适用对象：`Zxin-Pro/xp-jetpack` 全部 22 个分支
> 本文是**强约定**：所有功能模块必须遵守，主代理负责裁决冲突并更新本文。

---

## 0. 设计目标

1. **增量**：不动喷气代码，喷气行为 100% 不变
2. **可扩展**：未来新增功能 = 新增 1 个类 + 注册 1 行 + 写 1 段语言文件，**不改核心逻辑**
3. **三加载器统一**：功能代码只写一份（`templates/common/`），加载器差异全部沉在适配层

---

## 1. 新增功能挂载点（Mount Points）

| 挂载点 | 位置 | 由谁调用 | 用途 |
|---|---|---|---|
| `Feature#onServerTick` | common | `JetpackServerLogic.tick()` 内一行分发 | 每 tick 自动运行（呼吸/防火） |
| `Feature#onDamage` | common | 各加载器受伤事件 → `FeatureRegistry.damage()` | 伤害拦截（护盾） |
| `Feature#onBlockBreak` | common | 各加载器挖掘事件 → `FeatureRegistry.blockBreak()` | 挖掘后处理（熔炉） |
| `Feature#declareConfig` | common | 各加载器配置初始化 → `ConfigFacade` | 配置自动注册 |
| `Feature#commandName` | common | `SurvivalCommand.register()` | 指令自动注册 |
| `Feature#describe/status` | common | `/xps help`、`/xps status` | 展示自动注册 |

**约定：功能代码里禁止出现任何加载器 import（`net.minecraftforge.*` / `net.neoforged.*` / `net.fabricmc.*`）。**

---

## 2. 统一经验消耗接口（唯一入口）

`util/XpCost.java`（新建，**所有功能与喷气共用**）：

```java
public final class XpCost {
    /** 玩家可用经验点。 */
    public static int available(Player player)
    /** 创造模式免责。 */
    public static boolean isFree(Player player, JetpackConfig cfg)
    /** 按「点」扣费；不足返回 false 且不扣任何经验。 */
    public static boolean pay(ServerPlayer player, FeatureState st, double amount, JetpackConfig cfg)
    /** 按「点/秒」累积扣费（内部按 tick 累加，凑满 1 点才真正扣）。 */
    public static boolean payPerSecond(ServerPlayer player, FeatureState st, double perSecond, JetpackConfig cfg)
    /** 是否够付下一次。 */
    public static boolean canAfford(Player player, double amount, JetpackConfig cfg)
}
```

规则（**所有功能一致**）：
- **不足即停**：付不起 → 立即返回 false，功能当 tick 不生效 + 发一次提示（`exhaustedNotified` 去重）
- **优先点经验**：直接扣 `totalExperience`；不碰经验等级
- **小数累积**：`payPerSecond` 用 `st.accumulator` 累加，凑满 1 点才扣，避免每 tick 取整丢精度
- **创造模式**：`cfg.creativeFree` 为真时不扣（默认真）
- **手动优先**：玩家手动关掉的功能，经验恢复后**不会**自动开启（`st.manualDisabled` 标记）

> 喷气的 `JetState.burn`（1.0→0.0 单点进度）模型**保持原样**，不改。新功能用 `FeatureState.accumulator`，两套互不干扰。

---

## 3. Feature 接口

`feature/Feature.java`：

```java
public interface Feature {
    /** 唯一 id：用于配置段名、指令名、语言键前缀、状态键。全小写英文。 */
    String id();

    /** 显示名（用于 /xps help|status）。 */
    Component displayName();

    /** 一行说明。 */
    default Component description() { return Component.empty(); }

    /** 独立指令根名；返回 null 表示不注册独立指令。 */
    default String commandName() { return id(); }

    /** 默认是否开启（约定：全部默认 true）。 */
    default boolean defaultEnabled() { return true; }

    /** 声明自己的配置键（自动注册进三套配置系统）。 */
    default void declareConfig(ConfigFacade cfg) { }

    /** 每 tick 自动运行。功能自己判断前置条件（在水下/在火里…）。 */
    default void onServerTick(FeatureContext ctx) { }

    /** 受伤事件。可修改 info.amount 或取消。 */
    default void onDamage(FeatureContext ctx, DamageInfo info) { }

    /** 挖掘事件（已挖掉、掉落尚未生成或已生成，按加载器能力）。 */
    default void onBlockBreak(FeatureContext ctx, BlockBreakInfo info) { }

    /** /xps status 的一行简报。 */
    default Component statusLine(FeatureContext ctx) {
        return Component.literal(id() + ": " + (ctx.state().enabled ? "开" : "关"));
    }

    /** 尝试启用（供统一开关调用）。默认实现：清掉缺料提示标记。 */
    default void onEnable(FeatureContext ctx) { }
}
```

---

## 4. FeatureState（玩家级功能状态）

`feature/FeatureState.java` —— **每个玩家 × 每个功能一份**，落在 `JetState.features` 里（自动持久化）：

```java
public final class FeatureState {
    public boolean enabled;            // 当前是否生效（默认取 feature.defaultEnabled()）
    public boolean manualDisabled;     // 玩家手动关过 → 经验恢复也不自动开
    public boolean exhaustedNotified;  // 缺料提示去重
    public double accumulator;         // 不足 1 点的零头
    // 护盾专用：合并提示
    public transient double pendingDamage;
    public transient double pendingCost;
    public transient long lastNotifyTick;
    public transient String lastBlockId;   // 熔炉专用：避免重复加工同一方块
    public transient long lastBlockTick;
}
```

**兼容性**：`JetState.features` 是 `Map<String, FeatureState>`，**旧存档读到 null 时按 `defaultEnabled()` 初始化**（见 `FeatureStates.of(state, feature)`）。

---

## 5. 功能注册表 + 引导

`feature/FeatureRegistry.java`（机制） + `feature/Features.java`（唯一注册点）：

```java
// Features.java —— 新增功能只改这一行
public static void bootstrap() {
    FeatureRegistry.register(new RespirationFeature());
    FeatureRegistry.register(new FireproofFeature());
    FeatureRegistry.register(new ShieldFeature());
    FeatureRegistry.register(new AutoSmeltFeature());
    // ← 未来在这里加一行
}
```

`FeatureRegistry` 提供：
```java
register(Feature)         // 幂等，按 id 去重
all() / get(id)           // 只读遍历（LinkedHashMap 保序）
tick(ctx)                 // 分发 onServerTick
damage(ctx, info)         // 分发 onDamage
blockBreak(ctx, info)     // 分发 onBlockBreak
declareAllConfig(ConfigFacade)  // 分发 declareConfig
```

---

## 6. 配置自动注册

**机制**：功能在 `declareConfig(ConfigFacade)` 里声明自己的键 → 三套配置系统自动出现这些键。

```java
public interface ConfigFacade {
    void bool(String path, String comment, boolean def);
    void dbl(String path, String comment, double def, double min, double max);
    void intVal(String path, String comment, int def, int min, int max);
    void str(String path, String comment, String def);
}
```

**读取**：功能通过 `FeatureConfig.getBool("respiration.costPerSecond", 2.0)` 按路径读（内部是一张 `Map<String,Object>`，由加载器在 reload 时灌入）。

**与喷气配置的关系**：
- 喷气的 `JetpackConfig` 字段**保持原样，一个都不动**（保证喷气行为零变化）
- 新功能走 `FeatureConfig` 路径表，**不往 `JetpackConfig` 加字段**
- `/xps config` 能列出所有已注册键的当前值（自动生成，无需手写）

### 命名规范
`<featureId>.<key>`，例如 `respiration.costPerSecond`、`shield.blockRatio`。
全局键用 `global.*`。

---

## 7. 指令自动注册

**根指令**：`/xpsurvival`，别名 `/xps`。子指令由注册表自动生成：

```
/xps                       → help
/xps help                  → 列出所有功能 + 用法（自动）
/xps status                → 汇总状态（经验点 + 每个功能一行，自动）
/xps on <feature>          → 开
/xps off <feature>         → 关
/xps toggle <feature>      → 切换
/xps config [feature]      → 列出配置键与当前值（自动）
/xps reload                → 重载配置（权限 2）
```

**独立指令**（每个功能自带，由 `commandName()` 决定）：

```
/respiration [on|off|toggle|status]
/fireproof   [on|off|toggle|status]
/shield      [on|off|toggle|status]
/autosmelt   [on|off|toggle|status]
```

**权限**：普通子指令默认 0（只能改自己）；`reload` 默认 2。
**版本分叉**：`//#if MC>=12111` 用 `.requires(source -> true)`，否则 `source.hasPermission(n)` —— 沿用仓库既有写法。

---

## 8. 事件分发机制

加载器各自挂钩原生事件，**统一转成 common 的 `DamageInfo` / `BlockBreakInfo` 后调用注册表**：

| 加载器 | 伤害 | 挖掘 |
|---|---|---|
| Forge | `LivingHurtEvent`（可取消、可改 `amount`） | `BlockEvent.BreakEvent` |
| NeoForge | 见 `research/EVENTS-API.md`（1.21.x 类名有迁移） | 见同上 |
| Fabric | `ServerLivingEntityEvents.ALLOW_DAMAGE` / `AFTER_DAMAGE` | `PlayerBlockBreakEvents.AFTER` |

**约定**：新增功能**不要**自己写事件监听；只实现 `Feature#onDamage` / `onBlockBreak`，由注册表转发。

---

## 9. 提示（聊天栏）

`util/FeatureNotifier.java`：
- `notify(player, Component)` —— 受 `global.chatNotify` 总开关控制
- `notifyOnce(player, st, key, Component)` —— 去重（缺料提示）
- `notifyMerged(player, st, mode, Component, damage, cost)` —— 支持 `every` / `merged` / `broken` 三档
- **全程无音效**（约定：`shield.soundEnabled` 固定 false，不实现任何音效）

---

## 10. 版本分支策略

| 项 | 约定 |
|---|---|
| 分支 | **沿用现有 22 个，不增不减** |
| 新文件位置 | `templates/common/...`（功能）/ `templates/<loader>/...`（仅适配层） |
| 生成器 | **不用改**（`copy_tree` 递归，新文件自动带上全部分支） |
| 版本分叉语法 | `//#if` / `//#elif` / `//#else` / `//#endif` / `//@only MC>=xxxxx` |
| 阈值常量 | `12002`(Forge SimpleChannel) `12005`(载荷) `12100`(fromNamespaceAndPath) `12105`(Neo 发包) `12108`(Fabric HUD) `12111`(Identifier/权限) `26000`(26.x 渲染/载荷注册) `26300`(Neo 配置枚举) |

---

## 11. ★ 未来新增一个功能的标准步骤

> **目标：只新增文件 + 一行注册，不改任何核心逻辑。**

### 步骤 1：新建功能类
路径：`templates/common/dev/zxin/xpjetpack/feature/impl/<Name>Feature.java`

```java
package dev.zxin.xpjetpack.feature.impl;

import dev.zxin.xpjetpack.feature.Feature;
import dev.zxin.xpjetpack.feature.FeatureContext;
import dev.zxin.xpjetpack.feature.ConfigFacade;
import net.minecraft.network.chat.Component;

public final class MyFeature implements Feature {

    @Override
    public String id() {
        return "myfeature";              // ← 配置段名 / 指令名 / 语言键前缀
    }

    @Override
    public Component displayName() {
        return Component.translatable("xpjetpack.feature.myfeature");
    }

    @Override
    public void declareConfig(ConfigFacade cfg) {
        cfg.bool("myfeature.enabled", "是否开启", true);
        cfg.dbl("myfeature.costPerSecond", "每秒消耗经验", 1.0D, 0.0D, 100.0D);
    }

    @Override
    public void onServerTick(FeatureContext ctx) {
        if (!ctx.state().enabled) {           // 统一开关判定
            return;
        }
        if (!MyConditions.applies(ctx.player())) {  // 功能自己的前置条件
            return;
        }
        // 统一经验扣费（付不起会自动停用 + 提示一次）
        if (!XpCost.payPerSecond(ctx.player(), ctx.state(),
                FeatureConfig.dbl("myfeature.costPerSecond", 1.0D), ctx.config())) {
            return;
        }
        // 实际效果
        ctx.player().addEffect(...);
    }
}
```

### 步骤 2：在注册表登记一行
`templates/common/dev/zxin/xpjetpack/feature/Features.java`：

```java
FeatureRegistry.register(new MyFeature());   // ← 就这一行
```

### 步骤 3：写语言文件
`templates/resources/assets/xpjetpack/lang/zh_cn.json` + `en_us.json`：

```json
"xpjetpack.feature.myfeature": "我的功能",
"xpjetpack.feature.myfeature.desc": "一句话说明"
```

### 步骤 4（可选）：需要新的游戏事件
若功能需要 tick / 伤害 / 挖掘之外的事件（例如「玩家死亡」「方块放置」）：
在 `Feature` 接口加一个 `default void onXxx(...) {}` + 在 `FeatureRegistry` 加一个 `xxx(...)` 分发方法，
然后**只需在一个加载器适配类里挂一次原生事件**（三个加载器各一次，之后所有功能共享）。

### 步骤 5：验证
```bash
python3 gen.py                          # 重新生成 22 个分支
rg -n "myfeature" out/fabric-1.20.1/src # 抽查是否带上
python3 push_api.py                     # 推送 → Actions 自动编译 22 个分支
```

### 不需要改的东西（重要）
- ❌ 不用改 `gen.py`
- ❌ 不用改 `FlyCommand` / `JetpackServerLogic` / `JetpackPhysics`（分发点早已接好）
- ❌ 不用改三个加载器的事件类（除非引入了全新事件类型）
- ❌ 不用改 `NeoConfig` / `FabricConfig`（配置自动注册）
- ❌ 不用改任何网络包（除非功能需要 C2S 输入）

---

## 12. 冲突与裁决记录

| # | 冲突点 | 裁决 |
|---|---|---|
| 1 | 新功能配置放 `JetpackConfig` 还是独立？ | **独立**（`FeatureConfig` 路径表）。理由：喷气配置字段零改动 = 喷气行为零风险 |
| 2 | 护盾是否加音效？ | **不加**。`shield.soundEnabled` 固定 false，仅注释保留 |
| 3 | S2C 状态包是否扩展？ | **本轮不扩展**。4 个功能纯服务端自动运行，客户端无需感知 |
| 4 | `/fly` 是否并入 `/xps`？ | **不并**。`/fly` 原样保留；`/xps status` 汇总展示（含喷气一行） |
| 5 | 熔炉的 `silkTouchDisabled` / `fortuneEnabled` | 前者固定 true（不提供关闭选项），后者可配 |
