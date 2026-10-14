# XP Jetpack · 经验喷气背包

把玩家的**经验值**直接当燃料烧掉的喷气背包：用 `/fly` 开关喷气、空中按空格推进。
**不添加任何物品**，纯指令 + 玩家状态驱动；支持鞘翅滑翔时**无烟花推进**。

- 支持加载器：**Forge / NeoForge / Fabric**
- 支持版本：**1.20.1 → 26.3**（共 22 个分支，见 [VERSIONS.md](VERSIONS.md)）
- 依赖：零前置（Fabric 需要 Fabric API）

> **构建状态：22 / 22 分支全部编译通过 ✅**
> 全部由真实 GitHub Actions 编译验证，逐个分支产出 JAR。
> 运行记录：<https://github.com/Zxin-Pro/xp-jetpack/actions>
> 下载产物：进入任意分支对应的 workflow run → 页面底部 **Artifacts** 区下载
> （命名 `xp-jetpack-<loader>-<mc_version>`，内含 `xpjetpack-<loader>-<mc>-1.0.0.jar`）。

---

## 1. 玩法

| 操作 | 效果 |
|---|---|
| `/fly` | 切换喷气开关（聊天栏提示 `喷气状态：开/关`） |
| 空中按住 **空格** | 非鞘翅状态：向上喷气（最大上升速度 0.5） |
| 鞘翅滑翔时按住 **空格** | 沿视线方向推进（速度上限 1.5，**不消耗烟花**） |
| 松开空格 | 立即停止推力，保留惯性 |

燃料只有经验，**没有中间资源**：默认**消耗 1 点经验可以喷气 50 tick（2.5 秒）**，
鞘翅模式按 2 倍速率烧（1 点 / 25 tick）。起喷时先扣掉 1 点，之后按比例续烧，
经验烧光立刻停喷并提示「经验耗尽，喷气已停止！」。

## 2. 指令

| 指令 | 权限 | 说明 |
|---|---|---|
| `/fly` | 0（可配置） | 切换开关 |
| `/fly on` / `off` / `toggle` | 0 | 显式设置 |
| `/fly status` | 0 | 查看经验点与剩余可飞时长 |
| `/fly reload` | 2 | 重载配置 |

## 2.1 HUD

热键栏正上方一行**居中的文字**，显示当前可用经验点：

- 正常：`经验 37`
- 经验吃紧（≤3 点）：文字转黄
- 经验烧光：变红显示 `经验不足`
- 开启「仅鞘翅」模式时追加 `· 仅鞘翅`

默认**只在 `/fly` 开启后显示**，想要常驻把 `hud.always_visible` 设为 `true`；
位置用 `hud.offset_y` 调（默认 45，即热键栏上方）。纯客户端绘制，零网络开销。

## 3. 配置

Forge/NeoForge：`config/xpjetpack-common.toml`
Fabric：`config/xpjetpack.json`

| 键 | 默认 | 说明 |
|---|---|---|
| `experience.ticks_per_point` | 50.0 | 1 点经验可喷气多少 tick（直立） |
| `experience.elytra_multiplier` | 2.0 | 鞘翅模式消耗倍率 |
| `experience.require_level` | false | 要求经验等级 > 0 才能起飞 |
| `jetpack.command_permission_level` | 0 | `/fly` 权限等级 |
| `jetpack.elytra_only` | false | 只能鞘翅模式用 |
| `jetpack.creative_infinite` | true | 创造模式零消耗 |
| `jetpack.prevent_fall_damage` | true | 喷气时清除摔落伤害 |
| `jetpack.prevent_flying_kick` | true | 规避原版「悬浮太久」踢人 |
| `thrust.vertical_acceleration` | 0.08 | 直立模式加速度/tick |
| `thrust.vertical_max_speed` | 0.5 | 直立模式最大上升速度 |
| `thrust.vertical_damping` | 0.9 | 超速阻尼 |
| `thrust.elytra_thrust_per_tick` | 0.015 | 鞘翅模式推力/tick |
| `thrust.elytra_max_speed` | 1.5 | 鞘翅模式速度硬上限 |
| `thrust.elytra_requires_fallflying` | true | 必须处于滑翔 |
| `input.timeout_ticks` | 20 | 输入包失联判定 |
| `effects.particles` / `sounds` / `sound_volume` | true / true / 0.6 | 视觉音效 |
| `network.sync_interval_ticks` | 40 | 状态同步周期 |
| `hud.enabled` | true | 是否显示经验燃料 HUD |
| `hud.always_visible` | false | 未开启喷气时也显示 |
| `hud.offset_y` | 45 | 文字距屏幕底部像素（热键栏上方） |

## 4. 安装

1. 从 [Releases](../../releases) 或对应分支的 Actions 产物下载 `xpjetpack-<loader>-<mc>-1.0.0.jar`；
2. 放进 `mods/` 目录（Fabric 还需安装 **Fabric API**）；
3. 启动游戏，进服后输入 `/fly`。

> 客户端与服务端都要装（本模组有客户端预测与粒子音效）。

## 5. 架构

```
common  ← 22 个分支共用的同一份逻辑（服务端权威 + 客户端预测）
  physics/JetpackPhysics     物理（两侧调用同一函数）
  logic/JetpackServerLogic   服务端 tick：消耗/判定/施力/同步
  player/PlayerStore         世界级 JSON 持久化
  network/InputPacket,StatePacket
loader  ← 只做「事件注册 + 网络封装 + 配置承载」三件事
```

- **服务端权威**：是否开启、空气是否足够、是否在空中、是否滑翔，全部在服务端判定；
  客户端只做预测让手感顺滑，不能凭空获得推力。
- **不用 Mixin**：防踢所需的 `aboveGroundTickCount` 通过**反射**清零（字段名自动探测
  `aboveGroundTickCount` / `f_9788_`），22 个版本共用同一份代码，也不需要 AT/AW 配置文件。
- 详细接口约定见 [ARCHITECTURE.md](ARCHITECTURE.md)。

## 6. 构建

每个版本分支都是**独立可编译**的 Gradle 项目：

```bash
git checkout neoforge-1.21.4
./gradlew build          # 产物在 build/libs/
```

本仓库由 `tools/gen.py` + `tools/templates/` **统一生成**，不要直接手改某个分支——
改模板后重新生成，保证 22 个分支逻辑一致：

```bash
python3 tools/gen.py            # 生成全部
python3 tools/gen.py fabric-1.20.1
```

CI：`.github/workflows/build-all.yml`（main）用 matrix 一次性编译全部 22 个分支，
每个分支自身也带 `.github/workflows/build.yml`。

## 7. 常见问题

**Q：为什么喷气时不会被判定成飞行作弊？**
A：力度与时长受配置约束、且服务端每 tick 重新判定；同时主动清零原版 `aboveGroundTickCount`
计数器（该计数器是原版「floating too long」踢人的依据）。若你的服务端装了额外反作弊，
可把 `jetpack.prevent_flying_kick` 关掉自行处理。

**Q：能和其他喷气背包模组共存吗？**
A：可以，本模组不注册任何物品/方块，只读写自己的玩家状态文件。

**Q：空气没了会怎样？**
A：立刻停推并在聊天栏提示「经验耗尽，喷气已停止！」。经验一烧光就无法继续推进。

**Q：多人服务器需要所有人都装吗？**
A：服务端必须装；客户端不装则没有按键预测与粒子音效，但仍能被服务端推动。

## 8. 参考与致谢

手感设计参考了 [Create Jetpack](https://github.com/PssbleTrngle/CreateJetpack)（Forge/NeoForge）与
[CreateJetpack-Fabric](https://github.com/PssbleTrngle/CreateJetpack-Fabric) 的公开实现思路
（直立模式「加速度 + 速度上限」、鞘翅模式「沿视线推力」）。本项目为**独立重写**，未复制其源码。

## 9. 许可

MIT © 2026 Zxin-Pro
