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

## 2.2 生存套件（XP Survival Suite）

除飞行外，模组还提供四个**默认开启、全自动**的生存辅助功能，全部用经验点驱动，
**不需要任何物品**，也不需要手动触发。每个功能都能单独开关。

| 功能 | 默认消耗 | 触发条件 | 说明 |
|---|---|---|---|
| 🌊 水下呼吸 | 2 经验/秒 | 头部浸入水中 | 维持呼吸，**不含夜视** |
| 🔥 防火 | 3 经验/秒 | 着火或身处岩浆 | 免疫火焰/岩浆伤害，可选立即灭火 |
| 🛡 经验护盾 | 4 经验/点伤害 | 受到伤害 | 默认抵挡 50%，**无音效**，提示按 40 tick 合并 |
| ⛏ 经验熔炉 | 10 经验/块 | 挖掘矿石 | 掉落物直接换成熔炼产物；精准采集自动跳过；时运照常生效 |

**经验不足时功能自动停用并聊天提示**，经验恢复后自动继续（若曾手动关闭则不会自动开启）。
本模组**不添加任何音效**，所有提示都走聊天栏。

### 指令

| 指令 | 作用 |
|---|---|
| `/xpsurvival`（别名 `/xps`） | 查看全部功能状态 |
| `/xps <功能> on\|off\|toggle` | 单独开关某个功能 |
| `/xps <功能> status` | 查看单个功能状态与当前消耗 |
| `/xps config <功能>` | 列出该功能的全部配置键 |
| `/xps reload` | 重载配置 |
| `/respiration`、`/fireproof`、`/shield`、`/autosmelt` | 每个功能的独立指令（等同 `/xps <功能>`） |

### 配置键

配置写在**同一个文件**里（见 §3），键名以功能 id 开头：

```toml
[global]
    chatNotify = true          # 是否在聊天栏提示
    creativeFree = true        # 创造模式不消耗经验
    autoReenable = true        # 经验恢复后自动重新启用
    respectManualToggle = true # 尊重手动关闭，不自动开启

[respiration]
    enabled = true
    costPerSecond = 2.0

[fireproof]
    enabled = true
    costPerSecond = 3.0
    immunityLava = true
    immunityBurn = true
    extinguish = true

[shield]
    enabled = true
    blockRatio = 0.5           # 抵挡比例 0~1
    costPerDamage = 4.0
    notifyMode = "merged"      # every / merged / broken

[autosmelt]
    enabled = true
    costPerOre = 10.0
    oreTags = "c:ores,forge:ores,minecraft:ores"
    nameFallback = true        # 名称以 _ore 结尾的方块也算矿石
    blacklist = ""             # 永不熔炼的方块 id，逗号分隔
```

### 扩展：如何新增一个功能

见 [docs/INCREMENT-CONTRACT.md](docs/INCREMENT-CONTRACT.md) §11。标准流程只需两步：

1. 在 `feature/impl/` 新建一个类实现 `Feature` 接口（`id` / `displayName` / `declareConfig` / 需要的事件回调）；
2. 在 `feature/Features.java` 的 `bootstrap()` 里加一行 `FeatureRegistry.register(new YourFeature());`。

配置键、独立指令、状态存储、聊天提示、语言文件、事件分发都会**自动接入**，
三个加载器的代码一行都不用改。

## 3. 配置

Forge/NeoForge：`config/xpjetpack-common.toml`
Fabric：`config/xpjetpack.json`

生存套件（§2.2）的配置键与飞行配置写在**同一份文件**里（键名以功能 id 开头）。
Fabric 侧因为飞行配置是 Gson 序列化的强类型对象，功能配置另存为
`config/xpjetpack-features.json`（扁平「键路径 → 值」表），互不干扰。

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

### 直接下载全部 jar（推荐给测试用）

到 [Actions](../../actions/workflows/build-all.yml) 页面：
**Run workflow** → 勾选 `release` → 运行。构建完成后会自动创建一个 Release，
**22 个 jar 全部挂在附件里**，逐个下载即可，不用解压 artifact 压缩包。

也可以随便点开一次成功的 `Build All Versions` 运行，在页面底部 **Artifacts** 里
下载 `xp-jetpack-<loader>-<mc>-<run_number>`（zip，内含 jar）。

### 测试指引

装进游戏后建议按这个顺序验：

1. `/fly` 开关喷气 → 空中按空格能往上飞，HUD 显示经验在掉
2. 经验烧光 → 立刻停喷，聊天栏提示「经验不足」，HUD 变红
3. 跳进水里憋气 → 聊天栏应出现呼吸扣经验的提示，**不会**掉氧
4. 走进火里/岩浆 → 不掉血，经验在掉
5. 让怪打你一下 → 伤害减半，聊天栏提示「经验护盾抵挡了 X 点伤害，消耗 Y 经验」
6. 挖一块铁矿石 → 直接掉铁锭而不是粗铁，扣 10 经验；带**精准采集**再挖 → 掉原矿、不扣经验
7. `/xps` 看总状态；`/xps shield off` 关掉护盾再挨打 → 伤害恢复全额

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
