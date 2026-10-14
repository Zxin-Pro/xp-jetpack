# 版本支持表

构建号全部取自官方 Maven / Fabric Meta（2026-10 实测），详见 [research/报告.md](research/报告.md)。

| # | 分支 | 加载器 | MC | 加载器版本 | Java | Gradle | 构建工具 |
|---|---|---|---|---|---|---|---|
| 1 | `forge-1.20.1` | Forge | 1.20.1 | 47.4.27 | 17 | 8.14.5 | ForgeGradle 6 |
| 2 | `forge-1.20.2` | Forge | 1.20.2 | 48.1.0 | 17 | 8.14.5 | ForgeGradle 6 |
| 3 | `forge-1.20.4` | Forge | 1.20.4 | 49.2.11 | 17 | 8.14.5 | ForgeGradle 6 |
| 4 | `neoforge-1.20.6` | NeoForge | 1.20.6 | 20.6.141 | 21 | 9.2.1 | ModDevGradle 2.0.148 |
| 5 | `neoforge-1.21.1` | NeoForge | 1.21.1 | 21.1.259 | 21 | 9.2.1 | ModDevGradle 2.0.148 |
| 6 | `neoforge-1.21.4` | NeoForge | 1.21.4 | 21.4.158 | 21 | 9.2.1 | ModDevGradle 2.0.148 |
| 7 | `neoforge-1.21.8` | NeoForge | 1.21.8 | 21.8.54 | 21 | 9.2.1 | ModDevGradle 2.0.148 |
| 8 | `neoforge-1.21.11` | NeoForge | 1.21.11 | 21.11.45 | 21 | 9.2.1 | ModDevGradle 2.0.148 |
| 9 | `neoforge-26.1` | NeoForge | 26.1 | 26.1.0.19-beta | 25 | 9.2.1 | ModDevGradle 2.0.148 |
| 10 | `neoforge-26.2` | NeoForge | 26.2 | 26.2.0.90 | 25 | 9.2.1 | ModDevGradle 2.0.148 |
| 11 | `neoforge-26.3` | NeoForge | 26.3 | 26.3.0.69-beta | 25 | 9.2.1 | ModDevGradle 2.0.148 |
| 12 | `fabric-1.20.1` | Fabric | 1.20.1 | loader 0.19.5 / API 0.92.12+1.20.1 | 17 | 9.7.1 | Loom 1.18 |
| 13 | `fabric-1.20.2` | Fabric | 1.20.2 | loader 0.19.5 / API 0.91.6+1.20.2 | 17 | 9.7.1 | Loom 1.18 |
| 14 | `fabric-1.20.4` | Fabric | 1.20.4 | loader 0.19.5 / API 0.97.3+1.20.4 | 17 | 9.7.1 | Loom 1.18 |
| 15 | `fabric-1.20.6` | Fabric | 1.20.6 | loader 0.19.5 / API 0.100.8+1.20.6 | 21 | 9.7.1 | Loom 1.18 |
| 16 | `fabric-1.21.1` | Fabric | 1.21.1 | loader 0.19.5 / API 0.116.17+1.21.1 | 21 | 9.7.1 | Loom 1.18 |
| 17 | `fabric-1.21.4` | Fabric | 1.21.4 | loader 0.19.5 / API 0.119.4+1.21.4 | 21 | 9.7.1 | Loom 1.18 |
| 18 | `fabric-1.21.8` | Fabric | 1.21.8 | loader 0.19.5 / API 0.136.1+1.21.8 | 21 | 9.7.1 | Loom 1.18 |
| 19 | `fabric-1.21.11` | Fabric | 1.21.11 | loader 0.19.5 / API 0.141.6+1.21.11 | 21 | 9.7.1 | Loom 1.18 |
| 20 | `fabric-26.1` | Fabric | 26.1 | loader 0.19.5 / API 0.145.1+26.1 | 25 | 9.7.1 | Loom 1.18 |
| 21 | `fabric-26.2` | Fabric | 26.2 | loader 0.19.5 / API 0.161.0+26.2 | 25 | 9.7.1 | Loom 1.18 |
| 22 | `fabric-26.3` | Fabric | 26.3 | loader 0.19.5 / API 0.162.0+26.3 | 25 | 9.7.1 | Loom 1.18 |

## 版本差异处理点

| 差异 | 处理方式 |
|---|---|
| Forge 1.20.1/1.20.2/1.20.4 事件总线 | `@Mod.EventBusSubscriber`（FORGE / MOD 两条总线） |
| NeoForge 1.20.6+ 事件总线 | 直接在构造器里 `modBus.addListener` / `NeoForge.EVENT_BUS.addListener` |
| 服务器 tick 事件 | Forge `TickEvent.ServerTickEvent(END)`；NeoForge `ServerTickEvent.Post` |
| 客户端 tick 事件 | Forge `TickEvent.ClientTickEvent(END)`；NeoForge `ClientTickEvent.Post`；Fabric `ClientTickEvents.END_CLIENT_TICK` |
| 网络（Forge ≤1.20.4） | `SimpleChannel` + 数字包 ID |
| 网络（NeoForge / Fabric ≥1.20.5） | 原版 `CustomPacketPayload` + `StreamCodec`，ID `xpjetpack:input` / `xpjetpack:state` |
| 网络（Fabric ≤1.20.4） | 裸 `FriendlyByteBuf` + 通道 `ResourceLocation` |
| `ResourceLocation` 构造 | 1.21+ 用 `fromNamespaceAndPath`，之前用构造器 |
| Mapping | 全部使用 **Mojang 官方名**（Forge `official`、NeoForge mojmap、Fabric `loom.officialMojangMappings()`） |
| 26.x 无混淆 | Fabric 改用 `net.fabricmc.fabric-loom` 且依赖用 `implementation`（不再 remap）；NeoForge/Forge 仍走官方 jar |
| Gradle JVM | 1.20.x→17，1.20.6–1.21.11→21，26.x→25 |
| 持久化 | 统一 JSON 文件（`world/xpjetpack/players.json`），避开 Capability/DataAttachment/SavedData 的三套 API 差异 |
| 经验扣除 | 只使用 `Player#totalExperience` + `Player#giveExperiencePoints`，跨版本稳定 |
| 防踢 | **反射**清零 `ServerGamePacketListenerImpl#aboveGroundTickCount`（自动探测 `aboveGroundTickCount` / `f_9788_` 两个候选名，**不使用 Mixin，也不需要 AT/AW 配置**） |

## 已知未验证项

- NeoForge 26.1 官方只有 beta（`26.1.0.19-beta`），无稳定版；
- Fabric 26.x 官方不再构建 yarn，模板默认 mappings 由 Loom 决定。

---

# 实测构建结果（GitHub Actions 真实编译）

> 最后更新：2026-10-13 · 仓库 <https://github.com/Zxin-Pro/xp-jetpack/actions>

| # | 分支 | 结果 | 产物 |
|---|---|---|---|
| 1 | `forge-1.20.1` | ✅ BUILD SUCCESSFUL | `xpjetpack-forge-1.20.1-1.0.0.jar` |
| 2 | `forge-1.20.2` | ✅ BUILD SUCCESSFUL | `xpjetpack-forge-1.20.2-1.0.0.jar` |
| 3 | `forge-1.20.4` | ✅ BUILD SUCCESSFUL | `xpjetpack-forge-1.20.4-1.0.0.jar` |
| 4 | `neoforge-1.20.6` | ✅ BUILD SUCCESSFUL | `xpjetpack-neoforge-1.20.6-1.0.0.jar` |
| 5 | `neoforge-1.21.1` | ✅ BUILD SUCCESSFUL | `xpjetpack-neoforge-1.21.1-1.0.0.jar` |
| 6 | `neoforge-1.21.4` | ✅ BUILD SUCCESSFUL | `xpjetpack-neoforge-1.21.4-1.0.0.jar` |
| 7 | `neoforge-1.21.8` | ✅ BUILD SUCCESSFUL | `xpjetpack-neoforge-1.21.8-1.0.0.jar` |
| 8 | `neoforge-1.21.11` | ✅ BUILD SUCCESSFUL | `xpjetpack-neoforge-1.21.11-1.0.0.jar` |
| 9 | `neoforge-26.1` | ✅ BUILD SUCCESSFUL | `xpjetpack-neoforge-26.1-1.0.0.jar` |
| 10 | `neoforge-26.2` | ✅ BUILD SUCCESSFUL | `xpjetpack-neoforge-26.2-1.0.0.jar` |
| 11 | `neoforge-26.3` | ✅ BUILD SUCCESSFUL | `xpjetpack-neoforge-26.3-1.0.0.jar` |
| 12 | `fabric-1.20.1` | ✅ BUILD SUCCESSFUL | `xpjetpack-fabric-1.20.1-1.0.0.jar` |
| 13 | `fabric-1.20.2` | ✅ BUILD SUCCESSFUL | `xpjetpack-fabric-1.20.2-1.0.0.jar` |
| 14 | `fabric-1.20.4` | ✅ BUILD SUCCESSFUL | `xpjetpack-fabric-1.20.4-1.0.0.jar` |
| 15 | `fabric-1.20.6` | ✅ BUILD SUCCESSFUL | `xpjetpack-fabric-1.20.6-1.0.0.jar` |
| 16 | `fabric-1.21.1` | ✅ BUILD SUCCESSFUL | `xpjetpack-fabric-1.21.1-1.0.0.jar` |
| 17 | `fabric-1.21.4` | ✅ BUILD SUCCESSFUL | `xpjetpack-fabric-1.21.4-1.0.0.jar` |
| 18 | `fabric-1.21.8` | ✅ BUILD SUCCESSFUL | `xpjetpack-fabric-1.21.8-1.0.0.jar` |
| 19 | `fabric-1.21.11` | ✅ BUILD SUCCESSFUL | `xpjetpack-fabric-1.21.11-1.0.0.jar` |
| 20 | `fabric-26.1` | ✅ BUILD SUCCESSFUL | `xpjetpack-fabric-26.1-1.0.0.jar` |
| 21 | `fabric-26.2` | ✅ BUILD SUCCESSFUL | `xpjetpack-fabric-26.2-1.0.0.jar` |
| 22 | `fabric-26.3` | ✅ BUILD SUCCESSFUL | `xpjetpack-fabric-26.3-1.0.0.jar` |

覆盖范围：**1.20.1 → 26.3 全版本 × 全加载器（Forge 3 + NeoForge 8 + Fabric 11 = 22），22 / 22 分支全部编译通过并产出 JAR**，
全部由真实 GitHub Actions 编译验证。

# 26.x 适配记录（本次修复的三处断裂点）

26.1 起 Minecraft 移除代码混淆，官方同时做了大规模 API 重构。实测修复如下（三处均已并入模板，
按版本自动切换，不影响 ≤1.21.11 的分支）：

| # | 断裂点 | 影响版本 | 替代写法 |
|---|---|---|---|
| 1 | `Player#displayClientMessage(Component, boolean)` 被移除 | 26.1 / 26.2 / 26.3（全加载器） | 改用 1.19+ 全版本通用的 `Player#sendSystemMessage(Component)` |
| 2 | `ModConfig.Type` 枚举 `COMMON/SERVER` → `LOCAL/SYNCED` | **仅 NeoForge 26.3**（26.1/26.2 仍是旧名） | `//#if MC>=26300` 用 `ModConfig.Type.SYNCED`，否则 `COMMON` |
| 3 | `PayloadTypeRegistry.playC2S()/playS2C()` 改名 | Fabric 26.1 / 26.2 / 26.3 | 改用 `serverboundPlay()` / `clientboundPlay()` |

另外三处更早的 1.21.11 断裂点（`ResourceLocation`→`Identifier`、`CommandSourceStack#hasPermission(int)`→`PermissionSet`、
`Minecraft#screen` 字段移除、`Entity#getServer()` 移除）在 1.21.11 分支已修复并沿用至 26.x。

> 排查方法：26.1+ 官方 jar 已去混淆，用自写 Python class 解析器直接 dump 真实类名/方法签名定位替代 API
> （工具见 `tools/jcls.py` 的等价实现，仓库内模板已固化结论）。

---

## 经验生存套件（2026-10-14 增量）

四个功能已合入**全部 22 个分支**，每个分支的 CI 均实测通过：

| 功能 | 默认消耗 | 跨版本处理 |
|---|---|---|
| 水下呼吸 | 2 经验/秒 | 无分叉（`isUnderWater()` + 6 参 `MobEffectInstance` 全版本一致） |
| 防火 | 3 经验/秒 | 无分叉（`isInLava()` / `isOnFire()` / `clearFire()` 全版本一致） |
| 经验护盾 | 4 经验/点伤害 | Fabric 分叉：1.21+ 用 `AFTER_DAMAGE` + `heal()` 回补；1.20.x 无该事件，改用 `ALLOW_DAMAGE` + 取消原伤害 + 按削减值补刀 |
| 经验熔炉 | 10 经验/块 | 配方 API 四支分叉，见下表 |

### 熔炼配方查询的四处分叉（实测 mappings 得出）

| 版本区间 | 取结果方式 | 输入容器 |
|---|---|---|
| 1.20.1 | `recipe.getResultItem(regs)` | `SimpleContainer` |
| 1.20.2 – 1.20.6 | `recipeHolder.value().getResultItem(regs)` | `SimpleContainer` |
| 1.21.1 – 1.21.11 | `recipeHolder.value().assemble(input, regs)` | `SingleRecipeInput` |
| 26.1 – 26.3 | `recipeHolder.value().assemble(input)` | `SingleRecipeInput` |

> **`RecipeHolder` 是 1.20.2 引入的，不是 1.20.5** —— 版本阈值必须逐版核实。
> `RecipeInput` / `SingleRecipeInput` 则确实自 1.21.0 才出现。

### 加载器事件分叉（实测）

| 加载器 | 伤害事件 | 方块破坏事件 |
|---|---|---|
| Forge | `LivingHurtEvent` | `BlockEvent.BreakEvent` |
| NeoForge 1.20.6 | `LivingHurtEvent` | `BlockEvent.BreakEvent` |
| NeoForge 1.21.1 – 26.1 | `LivingIncomingDamageEvent` | `BlockEvent.BreakEvent` |
| NeoForge 26.2 – 26.3 | `LivingIncomingDamageEvent` | **`BreakBlockEvent`**（内嵌类已被移除） |
| Fabric 1.20.x | `ALLOW_DAMAGE`（仅能取消） | `PlayerBlockBreakEvents.AFTER` |
| Fabric 1.21+ | `AFTER_DAMAGE` | `PlayerBlockBreakEvents.AFTER` |

### CI 环境要点

**Loom 1.18 要求 Gradle 运行在 JVM 25**（哪怕编译目标是 17/21）。
`setup-java` 装两次会互相覆盖 `JAVA_HOME`，因此必须**先装「编译目标 JDK」、后装「Gradle 运行 JDK 25」**。
分支工作流与聚合工作流（`build-all.yml`）均已按此顺序配置。

