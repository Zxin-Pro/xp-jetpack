# XP Jetpack · 接口约定文档（v1.0）

> 由「架构子代理」产出，所有子代理（Forge/NeoForge/Fabric/CI/文档/验证）必须遵守。
> 主代理拥有最终裁决权；任何偏离需回写本文件并递增版本。

## 0. 项目标识

| 项 | 值 |
|---|---|
| mod_id | `xpjetpack` |
| 显示名 | XP Jetpack / 经验喷气背包 |
| Maven group | `dev.zxin` |
| Java 包根 | `dev.zxin.xpjetpack` |
| 版本 | `1.0.0` |
| LICENSE | MIT |
| 产物名 | `xpjetpack-<loader>-<mc>-1.0.0.jar` |

## 1. 目录 / 分支策略

- `main`：README、LICENSE、.gitignore、版本支持表、`.github/workflows/build-all.yml`（matrix 编译所有分支）。
- 每个「加载器 + MC 版本」一个分支：`forge-1.20.1`、`neoforge-1.21.4`、`fabric-26.3` 等，共 **22** 个。
- 每个分支是一个**完整可独立编译的 Gradle 项目**（含 gradle wrapper）。
- 所有分支由**同一套模板**经 `gen.py` 条件预处理（`//#if / //#else / //#endif`）生成，禁止手工分叉实现。

## 2. 包结构（所有加载器一致）

```
dev.zxin.xpjetpack
├── XpJetpack.java              # 常量 + ResourceLocation 工厂
├── Bridge.java                 # 平台桥（配置/发包/日志），由加载器注入
├── config/JetpackConfig.java   # 唯一配置模型（POJO），所有加载器共用
├── player/JetState.java        # 单玩家状态 POJO
├── player/PlayerStore.java     # 世界级 JSON 持久化（world/xpjetpack/players.json）
├── physics/JetpackPhysics.java # 纯物理函数（客户端+服务端共用）
├── logic/JetpackServerLogic.java # 服务端权威 tick
├── logic/ServerNetworkHandler.java # 收到输入包后的权威校验入口
├── command/FlyCommand.java     # /fly 指令树
├── network/InputPacket.java    # C2S DTO（thrusting）
├── network/StatePacket.java    # S2C DTO（air/enabled/物理参数）
├── network/InputPayload.java   # MC>=1.20.5：CustomPacketPayload 包装
├── network/StatePayload.java   # MC>=1.20.5
├── util/XpUtil.java            # 经验点扣除（仅用 totalExperience + giveExperiencePoints）
├── client/ClientState.java     # 客户端镜像状态
├── client/PacketHandlers.java  # 客户端收包落点（仅客户端类加载）
└── client/JetpackClientLogic.java # 客户端预测 + 按键 + 粒子音效
```

加载器包：`dev.zxin.xpjetpack.forge` / `.neoforge` / `.fabric`（各含主类、Config、Network、Events）。

## 3. 配置键（键名跨加载器必须一致）

| 键 | 类型 | 默认 | 说明 |
|---|---|---|---|
| `experience.ratio` | double | 100.0 | 1 点经验 → 多少压缩空气 |
| `experience.auto_refill` | bool | true | 自动用经验补空气 |
| `experience.refill_threshold` | double | 0.5 | 低于上限的比例时补 |
| `experience.max_xp_per_tick` | int | 1 | 每 tick 最多消耗经验点（限流） |
| `air.max` | double | 10000.0 | 压缩空气上限 |
| `air.cost_per_tick` | double | 2.0 | 喷气每 tick 消耗 |
| `jetpack.command_permission_level` | int | 0 | /fly 权限等级 |
| `jetpack.elytra_only` | bool | false | 仅鞘翅可用 |
| `jetpack.creative_infinite_air` | bool | true | 创造模式无限空气 |
| `jetpack.prevent_fall_damage` | bool | true | 喷气时清零摔落距离 |
| `jetpack.prevent_flying_kick` | bool | true | 清零 aboveGroundTickCount 防踢 |
| `thrust.vertical_acceleration` | double | 0.08 | 直立模式每 tick 加速度 |
| `thrust.vertical_max_speed` | double | 0.5 | 直立模式最大上升速度 |
| `thrust.vertical_damping` | double | 0.9 | 超速时的阻尼系数 |
| `thrust.elytra_thrust_per_tick` | double | 0.015 | 鞘翅模式每 tick 沿视线推力 |
| `thrust.elytra_max_speed` | double | 1.5 | 鞘翅模式硬上限 |
| `thrust.elytra_requires_fallflying` | bool | true | 鞘翅模式必须处于滑翔 |
| `input.timeout_ticks` | int | 20 | 输入包失联判定 |
| `effects.particles` | bool | true | 粒子 |
| `effects.sounds` | bool | true | 音效 |
| `effects.sound_volume` | double | 0.6 | 音量 |
| `network.sync_interval_ticks` | int | 40 | S2C 状态同步周期 |

Forge/NeoForge 用 `ForgeConfigSpec`/`ModConfigSpec` 承载；Fabric 用 `config/xpjetpack.json`（Gson）。
三者读取后都注入同一个 `JetpackConfig` 实例。

## 4. 网络协议

| 方向 | ID（MC≥1.20.5） | Forge 数字 ID | 载荷 |
|---|---|---|---|
| C2S | `xpjetpack:input` | 0 | `boolean thrusting` |
| S2C | `xpjetpack:state` | 1 | air, maxAir, enabled, 物理参数（见 StatePacket） |

- 客户端**仅在状态变化时**发输入包；持续喷气时每 10 tick 重发一次保活。
- 服务端 `input.timeout_ticks` 内未收到新包则视为松手。
- 1.20.5+ 用原版 `CustomPacketPayload` + `StreamCodec`；1.20.1/1.20.2/1.20.4 Fabric 用裸 `FriendlyByteBuf` 通道；Forge ≤1.20.4 用 `SimpleChannel`。

## 5. 状态与持久化

`JetState`：`air(double)` / `enabled(boolean)` / `thrusting` / `inputTicks` / `exhaustedNotified`。
持久化：`<world>/xpjetpack/players.json`（Gson，UUID 为键，30s 或退出时落盘）。
**不使用** Capability/DataAttachment/SavedData —— 22 个版本 API 差异过大，JSON 文件零 API 依赖且跨版本稳定。

## 6. 物理约定（双方共用 `JetpackPhysics`）

- 直立：`y' = y + accel`，上限 `vertical_max_speed`；超过上限时乘 `vertical_damping`。
- 鞘翅：`v' = v + look * elytra_thrust_per_tick`，随后 `|v'| ≤ elytra_max_speed` 硬夹紧。
- 松开即停：不施加任何反向力，保留原版惯性（只在按下 tick 施力）。
- 客户端做**预测**，服务端做**权威判定**，两侧调用同一函数，参数由 S2C 同步。

## 7. 服务端权威校验清单（防作弊）

1. `enabled == true`
2. `air >= cost_per_tick`（创造模式无限空气除外）
3. 输入包在 `timeout_ticks` 内
4. 直立模式：`!onGround()` 且 `!isFallFlying()`
5. 鞘翅模式：`isFallFlying()`（`elytra_requires_fallflying`）
6. `elytra_only` 时禁止直立推力
7. 施力后速度不得超过配置上限（硬夹紧）
8. 每 tick 消耗由服务端扣除，客户端只是预测

## 8. 版本分支构建矩阵（真实 Maven 元数据）

| 分支 | 构建 | 版本号 | Java | Gradle |
|---|---|---|---|---|
| forge-1.20.1 | ForgeGradle 6 | forge 47.4.27 | 17 | 8.14.5 |
| forge-1.20.2 | ForgeGradle 6 | forge 48.1.0 | 17 | 8.14.5 |
| forge-1.20.4 | ForgeGradle 6 | forge 49.2.11 | 17 | 8.14.5 |
| neoforge-1.20.6 | ModDevGradle 2.0.148 | 20.6.141 | 21 | 9.2.1 |
| neoforge-1.21.1 | ModDevGradle 2.0.148 | 21.1.259 | 21 | 9.2.1 |
| neoforge-1.21.4 | ModDevGradle 2.0.148 | 21.4.158 | 21 | 9.2.1 |
| neoforge-1.21.8 | ModDevGradle 2.0.148 | 21.8.54 | 21 | 9.2.1 |
| neoforge-1.21.11 | ModDevGradle 2.0.148 | 21.11.45 | 21 | 9.2.1 |
| neoforge-26.1 | ModDevGradle 2.0.148 | 26.1.0.19-beta | 25 | 9.2.1 |
| neoforge-26.2 | ModDevGradle 2.0.148 | 26.2.0.90 | 25 | 9.2.1 |
| neoforge-26.3 | ModDevGradle 2.0.148 | 26.3.0.69-beta | 25 | 9.2.1 |
| fabric-1.20.1 | Loom(remap) 1.18 | loader 0.19.5 / api 0.92.12+1.20.1 | 17 | 9.7.1 |
| fabric-1.20.2 | 同上 | api 0.91.6+1.20.2 | 17 | 9.7.1 |
| fabric-1.20.4 | 同上 | api 0.97.3+1.20.4 | 17 | 9.7.1 |
| fabric-1.20.6 | 同上 | api 0.100.8+1.20.6 | 21 | 9.7.1 |
| fabric-1.21.1 | 同上 | api 0.116.17+1.21.1 | 21 | 9.7.1 |
| fabric-1.21.4 | 同上 | api（见 gen.py） | 21 | 9.7.1 |
| fabric-1.21.8 | 同上 | api 0.136.1+1.21.8 | 21 | 9.7.1 |
| fabric-1.21.11 | 同上 | api 0.141.6+1.21.11 | 21 | 9.7.1 |
| fabric-26.1 | Loom 1.18 | api 0.145.1+26.1 | 25 | 9.7.1 |
| fabric-26.2 | Loom 1.18 | api 0.161.0+26.2 | 25 | 9.7.1 |
| fabric-26.3 | Loom 1.18 | api 0.162.0+26.3 | 25 | 9.7.1 |

## 9. 预处理器指令

模板中支持：`//#if MC>=12005`、`//#if MC>=12105`、`//#if LOADER_FABRIC`、`//#if JAVA>=21`、`//#else`、`//#endif`。
生成器 `gen.py` 按目标版本求值并输出纯 Java/资源文件。
