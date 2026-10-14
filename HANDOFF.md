# XP Jetpack · 交接文档

> 交接时间：2026-10-14
> 仓库：https://github.com/Zxin-Pro/xp-jetpack
> Actions：https://github.com/Zxin-Pro/xp-jetpack/actions
> 最新 Release：https://github.com/Zxin-Pro/xp-jetpack/releases/tag/v1.0.0-34

---

## 0. 一句话现状

喷气功能 + 经验生存套件（4 功能）已全部实现，**22 个版本分支 CI 全绿**，
但 **Fabric 26.3 在游戏里加载失败**（版本范围写成精确匹配），**这是当前唯一的阻塞问题**，修复方案见 §1。

## 0.1 接手第一步（给接手的 AI）

1. **读 §1**，按那里的步骤修 `gen.py` 里 `mc_range()` 的 26.x 精确匹配问题
2. **同样的 bug** 在 `Infinite Stacks` 和 `Unbounded Height` 两个仓库里也存在，要分别修（见 §9 待办）
3. §4–§8 是背景知识（工程结构 / 架构 / 跨版本分叉表 / 踩坑 / 构建推送流程），**改动时再查，不必先通读**

---

## 1. 🔴 当前阻塞：26.x 的 Minecraft 依赖写成精确匹配

### 现象

Fabric Loader 启动报错：

```
模组 'XP Jetpack' (xpjetpack) 1.0.0 需要 'Minecraft' (minecraft) 的 [26.3] 版本，
但已经安装了的版本 26.3 不对！
```

同一份报错还涉及另外两个模组（**也是这个仓库体系产出的**）：
`Infinite Stacks` (infinitestacks)、`Unbounded Height` (unbounded_height)。
三者是同一个 bug，需要分别修各自的仓库。

### 根因

`gen.py` 的 `mc_range()` 对 26.x 走了特判，返回**精确匹配**；1.x 走的是正常区间。

```python
def mc_range(mc):
    """mods.toml / fabric.mod.json 的 minecraft 依赖范围。"""
    if mc.startswith('26.'):
        return '[' + mc + ']'          # ← BUG：精确匹配，26.3.1 / 26.3-rc1 之类全对不上
    parts = mc.split('.')
    major, minor = int(parts[0]), int(parts[1])
    return '[' + mc + ',' + str(major) + '.' + str(minor + 1) + ')'
```

各分支实际生成结果（已核实）：

| 分支 | 生成值 | 判定 |
|---|---|---|
| `out/fabric-1.20.1` | `[1.20.1,1.21)` | ✅ |
| `out/fabric-1.20.4` | `[1.20.4,1.21)` | ✅ |
| `out/fabric-1.21.1` | `[1.21.1,1.22)` | ✅ |
| `out/fabric-26.1` | `[26.1]` | ❌ |
| `out/fabric-26.2` | `[26.2]` | ❌ |
| `out/fabric-26.3` | `[26.3]` | ❌ |

Maven 范围语法：`[26.3]` = **精确等于**；`[26.3,26.4)` = 大于等于 26.3 且小于 26.4。

### ⚠️ 动手前先确认一件事

报错里那句「但已经安装了的版本 26.3 不对」是**中文翻译截断**的结果。
英文原文形如 `but only <实际版本> is installed`——**关键是那个「实际版本」到底是多少**。

先让用户确认（或让他看 `logs/latest.log` / 启动器里的版本号）：

- 如果实际是 **`26.3.1` / `26.3.2`** 这类 → 用 `[26.3,26.4)` 即可
- 如果实际是 **`26.3-rc1` / `26.3-pre2`** 这类预发布 → 用 **`>=26.3`** 更稳
- **不确定就直接用 `>=26.3`**（甚至 `*`），先把游戏跑起来，再收窄

### 修复方案（推荐）

删掉特判，让 26.x 也走统一区间逻辑：

```python
def mc_range(mc):
    """mods.toml / fabric.mod.json 的 minecraft 依赖范围。"""
    parts = mc.split('.')
    major, minor = int(parts[0]), int(parts[1])
    return '[' + mc + ',' + str(major) + '.' + str(minor + 1) + ')'
```

这样 `26.3 → [26.3,26.4)`，可覆盖 `26.3.1`、`26.3.2` 等补丁版，且不会误匹配 `26.4`。

> ⚠️ **注意**：`MC_RANGE` 占位符**同时供三个加载器使用**：
> - `templates/resources/fabric.mod.json` → `"minecraft": "{{MC_RANGE}}"`
> - `templates/resources/mods.toml`（Forge）→ 第 29 行 `versionRange="{{MC_RANGE}}"`
> - `templates/resources/neoforge.mods.toml`（NeoForge）→ 同上
>
> **改这一个函数，三加载器同时生效。**

### 改完后

```bash
cd <仓库根>
python3 gen.py          # 重新生成 22 个分支到 out/
python3 push_api.py     # 推送到 GitHub（22 个分支会各自触发 CI）
# 等 CI 全绿后，发 Release：
gh workflow run build-all.yml -f release=true
```

---

## 2. 🚑 应急方案：不重新构建，直接改 jar（用于快速验证）

如果只是想立刻在游戏里跑起来、验证功能，**不必重新构建**，直接改 jar 里的元数据：

```bash
JAR=xp-jetpack-fabric-26.3-34.jar

# 1. 解出 fabric.mod.json
unzip -o "$JAR" fabric.mod.json -d /tmp/fix

# 2. 把 "minecraft": "[26.3]" 改成 "minecraft": ">=26.3"
#    （或 "[26.3,26.4)"，或直接 "*" 最宽松）
sed -i 's/"minecraft": "\[26.3\]"/"minecraft": ">=26.3"/' /tmp/fix/fabric.mod.json

# 3. 替换回 jar（保持路径在根目录）
cd /tmp/fix && zip -X "$OLDPWD/$JAR" fabric.mod.json
```

或者用任何压缩软件（7-Zip / WinRAR）打开 jar，把 `fabric.mod.json` 拖出来改完再拖回去。

**同样的操作要做三遍**：`xpjetpack`、`infinitestacks`、`unbounded_height` 三个 jar 都要改，
否则 Fabric Loader 会因为其中任意一个而拒绝启动。

> 元数据里若还有其它 `depends` 项（如 `fabricloader`、`fabric-api`、`java`），
> 报错信息会逐条列出，按同样方式放宽即可。

---

## 3. 环境准备（在电脑上）

### 必需

| 项 | 版本 | 说明 |
|---|---|---|
| JDK 25 | **必须** | Loom 1.18 要求 **Gradle 运行在 JVM 25**，哪怕编译目标是 17/21 |
| JDK 17 / 21 | 按分支 | 编译目标 JDK（1.20.x=17，1.21.x=21，26.x=25） |
| Python 3 | ≥3.8 | 跑 `gen.py` / `push_api.py` |
| `gh` CLI | 最新 | 推送与查 CI 用，需 `gh auth login` |

### 认证

`push_api.py` 走 `gh api`，**前置条件只有一条**：`gh auth status` 通过。

```bash
gh auth login          # 选 GitHub.com → HTTPS → 用浏览器或 token 登录
gh auth status         # 确认 ✓ Logged in to github.com account Zxin-Pro
```

> 脚本内部**不硬编码任何 token**，全部复用 `gh` 的凭据。
> 如果你更习惯 `git push`，也可以——但注意：某些网络环境下 `git push` 的 TCP 连接会被掐断，
> 而 `api.github.com` 始终可用，所以仓库提供了 API 推送脚本作为兜底。

---

## 4. 工程结构

```
xp-jetpack/
├── gen.py                  # 生成器：模板 → out/ 下 22 个分支
├── push_api.py             # 推送器：GitHub Git Data API 批量推 22 分支
├── make_icon.py            # 图标生成
├── templates/              # ★ 唯一真源，改代码只改这里
│   ├── common/             # 三加载器共用的代码（业务逻辑全在这）
│   │   └── dev/zxin/xpjetpack/
│   │       ├── feature/        # 可扩展功能框架（见 §5）
│   │       │   └── impl/       # 四个功能实现
│   │       ├── util/           # XpCost / FeatureNotifier / OreTags / XpUtil
│   │       ├── command/        # FlyCommand / SurvivalCommand
│   │       ├── logic/          # JetpackServerLogic（喷气物理 + 功能分发）
│   │       ├── player/         # JetState / PlayerStore
│   │       └── ...
│   ├── forge/              # Forge 专有（事件注册、配置、网络）
│   ├── neoforge/           # NeoForge 专有
│   ├── fabric/             # Fabric 专有
│   ├── resources/          # 语言文件、模组元数据（fabric.mod.json / mods.toml）
│   ├── build/              # 各加载器的 build.gradle 模板
│   └── workflows/          # 各分支的 CI 工作流模板
├── out/                    # 生成产物（22 个完整分支，可直接 cd 进去构建）
├── main/                   # main 分支内容（README / VERSIONS / docs / 聚合工作流）
├── wrapper/                # gradle wrapper
└── research/               # 早期调研笔记
```

### 核心机制：**改模板，不要改 out/**

`out/` 是生成产物，**任何手改都会在下次 `gen.py` 时被覆盖**。
所有修改都改 `templates/`（或 `main/`），然后重新生成。

### 生成器怎么工作

`gen.py` 用 `copy_tree()` **递归**拷贝模板目录到 `out/<loader>-<mc>/`，
并按分支的版本号做**预处理**替换：

| 指令 | 作用 |
|---|---|
| `//#if MC>=12100` | 版本条件 |
| `//#elif` / `//#else` / `//#endif` | 分支链 |
| `//@only MC>=12005` | 整行只在满足条件时保留 |
| `{{MC_RANGE}}` 等 | 占位符替换 |

> ⚠️ **`//#if/#elif/#else` 是「整条链只取一个分支」**。
> **两件互不相关的事必须各写一个独立的 `//#if`**，不能塞进同一个链。
> （我们就是踩了这个坑：`MC>=26200` 分支只拿到了 `BreakBlockEvent`，丢了伤害事件导入，导致 26.2/26.3 编译失败。）

### 版本号编码

`mc_code()`：非 1.x 为 `major*1000 + minor*100 + patch`，1.x 为 `10000 + minor*100 + patch`。

| MC | 编码 | | MC | 编码 |
|---|---|---|---|---|
| 1.20.1 | 12001 | | 1.21.11 | 12111 |
| 1.20.2 | 12002 | | 26.1 | 26100 |
| 1.20.4 | 12004 | | 26.2 | 26200 |
| 1.20.6 | 12006 | | 26.3 | 26300 |
| 1.21.1 | 12101 | | | |

> `gen.py` 里 `mc_code` 的 docstring 写着 `26.3 -> 26003`，**是错的**，以代码为准（`26300`）。

---

## 5. 架构：可扩展功能注册表

这是这个项目的核心设计。**新增功能只需两步**：

### 第 1 步：在 `templates/common/.../feature/impl/` 新建类

```java
public final class MyFeature implements Feature {
    @Override public String id() { return "myfeature"; }              // 配置键前缀 + 指令名
    @Override public Component displayName() { return Component.literal("我的功能"); }  // 抽象，必须实现
    @Override public boolean defaultEnabled() { return true; }

    @Override public void declareConfig(ConfigFacade cfg) {
        cfg.bool("myfeature.enabled", "是否开启", true);
        cfg.dbl("myfeature.costPerSecond", "每秒消耗经验", 1.0D, 0.0D, 1000.0D);
    }

    @Override public void onServerTick(FeatureContext ctx) { /* 每 tick 逻辑 */ }
    // 可选：onDamage(ctx, DamageInfo) / onBlockBreak(ctx, BlockBreakInfo) / statusLine(ctx)
}
```

### 第 2 步：在 `Features.bootstrap()` 加一行

```java
public static void bootstrap() {
    FeatureRegistry.register(new RespirationFeature());
    FeatureRegistry.register(new FireproofFeature());
    FeatureRegistry.register(new ShieldFeature());
    FeatureRegistry.register(new AutoSmeltFeature());
    FeatureRegistry.register(new MyFeature());      // ← 只加这一行
}
```

**自动接入的东西**（一行都不用再写）：

| 能力 | 说明 |
|---|---|
| 配置键注册 | 自动进 `config/xpjetpack-common.toml`（Forge/NeoForge）或 `config/xpjetpack-features.json`（Fabric） |
| 独立指令 | 自动生成 `/myfeature on|off|toggle|status` |
| 状态存储 | `JetState.features` 里自动分配槽位，随玩家数据持久化 |
| 事件分发 | tick / 伤害 / 挖掘 三路自动派发 |
| 聊天提示 | `FeatureNotifier` 自动按 40 tick 合并 |
| 语言文件 | 用 `xpjetpack.feature.<id>` 约定键 |

**加载器侧完全不用动**——三个加载器各只有一个「只接线、零逻辑」的事件桥：
`ForgeFeatureEvents` / `NeoFeatureEvents` / `FabricFeatureEvents`。

### 统一经验消耗 API

所有功能走 `util/XpCost.java`，**不要自己扣经验**：

| 方法 | 用途 |
|---|---|
| `XpCost.pay(player, st, amount)` | 一次性扣，返回是否成功 |
| `XpCost.payPerSecond(player, st, perSecond)` | 按秒计费，内部按 20 tick/秒累加 |
| `XpCost.isFree(player)` | 创造模式免费判定（读 `global.creativeFree`） |
| `XpCost.remainingSeconds(player, perSecond)` | 剩余可用秒数（给 HUD 用） |

> **喷气不走这个 API**，它保留自己的 `burnXp`。这样改功能层永远不会影响喷气手感。

### 全局配置（`feature/GlobalConfig.java`）

| 键 | 默认 | 说明 |
|---|---|---|
| `global.chatNotify` | true | 总开关，关掉所有聊天提示 |
| `global.autoReenable` | true | 经验耗尽停用后，经验恢复时自动重新启用 |
| `global.respectManualToggle` | true | 玩家手动关掉的，不自动开回来 |
| `global.creativeFree` | true | 创造模式不耗经验 |
| `global.experiencePerSecond` | — | 全局每秒消耗基准 |
| `global.experiencePerDamage` | — | 全局每点伤害消耗基准 |

---

## 6. 跨版本 API 分叉表（全部实测，非推测）

> 这些结论都是下载官方 jar / `client_mappings` 逐版核对得出的，可直接复用。

### 6.1 熔炼配方查询（`AutoSmeltFeature`）

| MC 范围 | `getRecipeFor` | 取结果 |
|---|---|---|
| `< 12002`（1.20.1） | `(RecipeType, Container, Level)` | `found.get().getResultItem(regs)` |
| `12002 ~ 12099`（1.20.2–1.20.6） | 同上 | `found.get().value().getResultItem(regs)` |
| `12100 ~ 25999`（1.21.1–1.21.11） | `(RecipeType, RecipeInput, Level)` | `found.get().value().assemble(input, regs)` |
| `≥ 26000`（26.1+） | 同上 | `found.get().value().assemble(input)` ← **RegistryAccess 参数被移除** |

**关键事实**：
- **`RecipeHolder` 是 1.20.2 引入的**，不是 1.20.5。
- `RecipeInput` / `SingleRecipeInput` 是 **1.21.1 才出现**的，1.20.6 还没有（所以 1.20.6 仍用 `SimpleContainer`）。
- 输入容器：`MC<12100` 用 `new SimpleContainer(stack)`；`MC>=12100` 用 `new SingleRecipeInput(stack)`。

### 6.2 事件 API

| 加载器 / 范围 | 玩家受伤 | 方块破坏 |
|---|---|---|
| Forge（全版本） | `LivingHurtEvent` + `event.setAmount(float)` | `BlockEvent.BreakEvent` |
| NeoForge `1.20.6` | `LivingHurtEvent` | `BlockEvent.BreakEvent` |
| NeoForge `1.21.1 ~ 26.1` | `LivingIncomingDamageEvent` | `BlockEvent.BreakEvent` |
| NeoForge `≥ 26.2` | `LivingIncomingDamageEvent` | **`BreakBlockEvent`**（独立类，不再内嵌） |
| Fabric `1.20.x` | `ServerLivingEntityEvents.ALLOW_DAMAGE`（只能整体取消） | `PlayerBlockBreakEvents.AFTER` |
| Fabric `≥ 1.21` | `ServerLivingEntityEvents.AFTER_DAMAGE` | 同上 |

**关键事实**：
- NeoForge `≥26.2`：`BreakEvent` 从 `BlockEvent` 内部类**拆成了独立类** `net.neoforged.neoforge.event.level.block.BreakBlockEvent`。
- Fabric 的 `AFTER_DAMAGE` **在 1.20.x 的 Fabric API 里根本不存在**（最高 0.100.8 也没有），
  升级 Fabric API 无解 → 1.20.x 只能走 `ALLOW_DAMAGE` + 「取消原伤害 + 按削减值补一刀」，
  用 `REAPPLYING` 静态标志防重入。
- **`LivingEntity#hurt(DamageSource, float)` 在 1.21.8 之前已改名 `hurtServer(ServerLevel, DamageSource, float)`**，
  但 `heal(float)` 全版本通用 → 减伤功能统一走「受伤后 `heal()` 回补」，避开这个分叉。
- `BlockEvent` 的 `getLevel()` 返回 `LevelAccessor`（不是 `Level`），需要 `instanceof` 转换。

### 6.3 其它跨版本事实

| 项 | 分叉点 | 写法 |
|---|---|---|
| `ResourceLocation` 构造 | `≥12100` | `ResourceLocation.fromNamespaceAndPath(ns, path)`（构造函数私有） |
| `ResourceLocation` → `Identifier` | `≥12111` | 类名变更，包路径也变 |
| `CommandSourceStack#hasPermission(int)` | `≥12111` | 改为 `permissions()` / `PermissionSet` |
| `Minecraft#screen` 字段 | `≥12111` | 字段消失，HUD 判定改为直接读按键 |
| 文本发送 | 全版本 | 用 `sendSystemMessage(Component)`，不要用 `displayClientMessage` |
| 取 server | 全版本 | `player.level().getServer()`（`player.getServer()` 在 26.x 被移除） |
| HUD 渲染 | `≥26000` | 26.x 的 `GuiGraphics` 被 `GuiGraphicsExtractor` 取代，用 `graphics.text(...)` |
| Fabric HUD 注册 | `≥12108` | `HudElementRegistry.addLast(...)`，旧版用 `HudRenderCallback` |
| Fabric 网络注册 | 26.x | `PayloadTypeRegistry.playC2S/playS2C` → `serverboundPlay/clientboundPlay` |
| NeoForge 配置类型 | `≥26300` | `ModConfig.Type.COMMON/SERVER` → `LOCAL/SYNCED`（**只有 26.3 改，26.1/26.2 仍是旧名**） |
| 药水效果构造 | 全版本 | `new MobEffectInstance(MobEffects.X, dur, amp, false, false, false)`（6 参，隐藏粒子/图标） |
| 水下/岩浆判定 | 全版本 | `isUnderWater()` / `isInLava()` / `isOnFire()` / `clearFire()` 全版本一致 |

---

## 7. 踩坑清单（血泪排序）

1. **`//#if/#elif/#else` 链只能表达「互斥的一件事」**。
   把「伤害事件导入」和「方块破坏事件导入」塞进同一个链 → `MC>=26200` 分支只拿到后者，前者丢失 → 26.2/26.3 编译失败。
   **定位技巧：看生成物而不是看模板**（`rg import out/neoforge-26.2/.../NeoFeatureEvents.java`）一击命中。

2. **Loom 1.18 要求 Gradle 跑在 JVM 25**（哪怕编译目标是 17/21）。
   CI 里 `setup-java` 装两次会**互相覆盖 `JAVA_HOME`** → **必须先装「编译目标 JDK」，后装「Gradle 运行 JDK 25」**。
   漏了第二步就会报 `Dependency requires at least JVM runtime version 25. This build uses a Java 17 JVM.`

3. **`RecipeHolder` 是 1.20.2 引入的，不是 1.20.5**；`RecipeInput`/`SingleRecipeInput` 是 1.21.1 才有的。
   版本阈值必须逐版核实，不能想当然。

4. **Fabric 生产 jar 被 remap 成 intermediary 名**（`class_XXXX`），不是 Mojang 名。
   用 `strings | grep SimpleContainer` 之类搜 Mojang 名会**假阴性**。
   对照：`class_1277`=`SimpleContainer`、`class_7225`=`RecipeInput`、`class_8786`=`RecipeHolder`、`class_9696`=`SingleRecipeInput`。

5. **`gen.py` 的 `copy_tree` 是递归的** → 新增 `.java`/资源文件会被 **22 个分支自动带上**，不用改生成器。
   这是「只新增模块」可扩展架构的物理基础。

6. **排查 CI 要挖到 `Caused by` 深处**。我们先后误判为「Maven 并发挤爆」（加 `max-parallel: 4`）和「Gradle 缓存争用」（加 `cache-disabled: true`），**两次都无效**；
   真因藏在 `Caused by: VariantSelectionByAttributesException`。事后把两处无效的投机修改清理掉了，不留脏代码。

7. **`maven.neoforged.net` 在某些网络环境被 DNS 劫持/不可达**；`raw.githubusercontent.com` 也常不通。
   替代：用 `gh api` 读文件内容，加 `-H "Accept: application/vnd.github.raw+json"`。

8. **不要批量编辑同一个文件的多个位置**——工具会互相覆盖。一次改一处。

---

## 8. 构建 / 推送 / 发布

### 本地构建单个分支

```bash
cd out/fabric-26.3
./gradlew build          # 产物在 build/libs/
```

> **必须用 JDK 25 跑 Gradle**（`JAVA_HOME` 指向 JDK 25），否则 Loom 直接报错。

### 推送到 GitHub

```bash
python3 push_api.py      # 推送 out/ 下全部 22 个分支（含 main）
```

- 走 GitHub Git Data API，不依赖 `git push`（某些网络下 `git push` 会被掐断）。
- 带磁盘 blob 缓存，第二次起只推变化的文件。
- **没有分支过滤参数**（曾以为有 `ONLY_BRANCH`，实测无效）。
  只想改 main 的单个文件时，用 Contents API：

```bash
gh api -X PUT repos/Zxin-Pro/xp-jetpack/contents/<path> --input body.json
# body.json 需含 base64 的 content、sha（原文件 sha）、branch、message
```

### CI 与发布

| 工作流 | 位置 | 触发 |
|---|---|---|
| 分支构建 | 各分支 `.github/workflows/build.yml` | push 到该分支 |
| 聚合构建 | `main/.github/workflows/build-all.yml` | push 到 main / 手动 |
| **发 Release** | 同上，`release` 作业 | 手动 + 勾选 `release` |

```bash
# 构建 + 发布（22 个 jar 挂到 Release 附件）
gh workflow run build-all.yml -f release=true

# 只看构建不发布
gh workflow run build-all.yml
```

发布产物命名：`xp-jetpack-<loader>-<mc>-<run_number>.jar`
Release tag：`v1.0.0-<run_number>`

### 查 CI 状态

```bash
# 最近 30 次运行
gh run list --repo Zxin-Pro/xp-jetpack --limit 30 \
  --json status,conclusion,headBranch,createdAt \
  -q '.[] | "\(.headBranch)\t\(.status)\t\(.conclusion)"' | sort

# 某次运行详情
gh run view <run-id> --repo Zxin-Pro/xp-jetpack

# 只看失败的日志
gh run view <run-id> --repo Zxin-Pro/xp-jetpack --log-failed | rg -i "error|Caused by" | head -20
```

> ⚠️ **不要用 `sort | awk '!seen[$1]++'` 去重**——`sort` 会破坏「最新优先」排序，
> 导致读到旧的失败状态。要按分支取最新一条，用 `gh run list --branch <b> --limit 1`。

---

## 9. 交付物现状

### 已完成

| 项 | 状态 |
|---|---|
| 22 个版本分支 CI | ✅ 全绿 |
| 聚合工作流（22 作业） | ✅ 全绿 |
| 喷气功能 | ✅ 完成，本轮**一行未改** |
| 经验生存套件（4 功能） | ✅ 完成 |
| 可扩展架构 + 增量接口文档 | ✅ 完成 |
| Release `v1.0.0-32` / `v1.0.0-34` | ✅ 各含 22 个 jar |

### 四个功能

| 功能 | 指令 | 消耗 | 要点 |
|---|---|---|---|
| 水下呼吸 | `/respiration` | 2 经验/秒 | 不可见水肺效果，**不含夜视** |
| 防火 | `/fireproof` | 3 经验/秒 | 不可见抗火 + 可配灭火/岩浆免疫 |
| 经验护盾 | `/shield` | 4 经验/点伤害 | 默认抵挡 50%，**无音效**，提示按 40 tick 合并 |
| 经验熔炉 | `/autosmelt` | 10 经验/块 | 标签识别矿石；**不取消原版破坏**（耐久/统计/时运照旧），只替换掉落物；精准采集自动跳过 |

指令树：`/xpsurvival`（别名 `/xps`）+ 四个独立指令，全部由注册表自动生成。

### 已知限制

**Fabric 1.20.1 / 1.20.2 / 1.20.4 / 1.20.6** 上，护盾走的是替代实现
（Fabric API 在 1.21 前没有「伤害后」事件，只能用 `ALLOW_DAMAGE` 取消原伤害再按削减值补一刀）。
行为等价，但若装了其它也监听伤害事件的模组，可能看到多一次事件。
**其余三个功能在全部 22 个版本行为一致。**

### 待办

- [ ] **修 `mc_range()` 的 26.x 精确匹配问题**（§1，当前阻塞）
- [ ] 同样的 bug 存在于 **`Infinite Stacks`** 与 **`Unbounded Height`** 两个仓库，需分别修复
- [ ] （可选）把生存套件补充进 `main/VERSIONS.md` 的版本表

---

## 10. 相关文档

| 文档 | 位置 | 内容 |
|---|---|---|
| 仓库结构分析 | `main/docs/ANALYSIS.md` | 模板树、扩展点定位、版本差异表 |
| **增量接口约定** | `main/docs/INCREMENT-CONTRACT.md` | **含「未来如何新增一个功能」的标准步骤** |
| 版本构建表 | `main/VERSIONS.md` | 22 分支实测结果 + 跨版本分叉表 |
| 架构说明 | `main/ARCHITECTURE.md` | 整体设计 |
| 使用说明 | `main/README.md` | 功能、配置、下载、7 步测试清单 |

---

## 11. 快速自检清单

接手后建议按顺序确认：

```bash
# 1. 认证
gh auth status

# 2. 仓库可访问
gh repo view Zxin-Pro/xp-jetpack --json name,defaultBranchRef -q '"\(.name) \(.defaultBranchRef.name)"'

# 3. 最新 CI 状态（应为全绿）
gh run list --repo Zxin-Pro/xp-jetpack --limit 5 --json headBranch,conclusion \
  -q '.[] | "\(.headBranch)\t\(.conclusion)"'

# 4. 现有 Release
gh release list --repo Zxin-Pro/xp-jetpack --limit 3

# 5. 本地能生成
python3 gen.py | tail -3
```

有任何异常，先看 `main/docs/INCREMENT-CONTRACT.md` 与 `main/VERSIONS.md`，
这两份文档记录了几乎全部设计决策与跨版本结论。
