# 现有仓库结构分析报告

> **产出方：仓库分析子代理** ｜ 分析对象：`Zxin-Pro/xp-jetpack`（本地工作区 `/var/minis/workspace/xp-jetpack/`）
> 分析时间：2026-10-14

---

## 1. 仓库概况

| 项 | 值 |
|---|---|
| 仓库 | https://github.com/Zxin-Pro/xp-jetpack |
| 分支 | 23 个（`main` + 22 个版本分支） |
| 构建状态 | **22 / 22 全部编译通过**（GitHub Actions 实测） |
| 模组 ID | `xpjetpack` |
| 根包名 | `dev.zxin.xpjetpack` |
| 构建方式 | **模板 + 生成器**（`gen.py` 生成 22 个分支，不是手写 22 份代码） |
| 映射 | 全版本 Mojang 官方映射（Forge `official`、NeoForge mojmap、Fabric `loom.officialMojangMappings()`） |

### 分支列表（按加载器）

- **Forge**：`forge-1.20.1`、`forge-1.20.2`、`forge-1.20.4`
- **NeoForge**：`neoforge-1.20.6`、`-1.21.1`、`-1.21.4`、`-1.21.8`、`-1.21.11`、`-26.1`、`-26.2`、`-26.3`
- **Fabric**：`fabric-1.20.1`、`-1.20.2`、`-1.20.4`、`-1.20.6`、`-1.21.1`、`-1.21.4`、`-1.21.8`、`-1.21.11`、`-26.1`、`-26.2`、`-26.3`

---

## 2. 工作区目录树（生成器视角）

```
/var/minis/workspace/xp-jetpack/
├── gen.py                      ← 生成器：22 个分支的唯一真源
├── push_api.py                 ← 走 GitHub Git Data API 推送（沙箱 git push 会断）
├── make_icon.py
├── ARCHITECTURE.md             ← 一期接口约定
├── main/                       ← main 分支内容（README/VERSIONS/LICENSE/push_to_github.sh/workflows）
├── templates/                  ← 所有源码模板
│   ├── common/dev/zxin/xpjetpack/          ← 跨版本共用（占 90%）
│   ├── forge/dev/zxin/xpjetpack/forge/     ← Forge 适配层
│   ├── neoforge/dev/zxin/xpjetpack/neoforge/
│   ├── fabric/dev/zxin/xpjetpack/fabric/
│   ├── resources/                          ← mods.toml / fabric.mod.json / lang / icon
│   ├── build/{forge,neoforge,fabric}/      ← build.gradle / gradle.properties
│   └── workflows/build.yml                 ← 每个分支的 CI
├── out/                        ← 生成的 22 个分支（构建产物，可随时重建）
└── research/                   ← 版本矩阵调研数据
```

### ★★★ 关键机制：新增文件会被自动带上所有分支

`gen.py` 的 `copy_tree()` 用 `os.walk` **递归**拷贝：

```python
# gen.py:236 generate_one()
copy_tree(os.path.join(TPL, 'common'),   os.path.join(dest, 'src/main/java'), flags, t)
copy_tree(os.path.join(TPL, t['loader']), os.path.join(dest, 'src/main/java'), flags, t)
copy_tree(os.path.join(TPL, 'resources', 'assets'), os.path.join(res, 'assets'), flags, t)
```

**结论：新增任何 `.java` 或 `assets/` 下的资源文件，只要放进 `templates/` 对应目录，22 个分支自动获得，`gen.py` 一行都不用改。**
这是本次可扩展架构能落地的前提。

---

## 3. 代码结构（common 层）

| 文件 | 作用 | 本次是否要动 |
|---|---|---|
| `XpJetpack.java` | MOD_ID / `id(String)`（随版本返回 `ResourceLocation` 或 `Identifier`） | 不动 |
| `Bridge.java` | **平台桥**：配置读写、C2S/S2C 发包、日志 | 加 1 处：注册事件发送器（可选） |
| `config/JetpackConfig.java` | 配置 POJO（纯字段 + `clamp()` + `copy()` + `defaults()`） | **加新功能字段** |
| `player/JetState.java` | 单玩家状态（`enabled`/`burn`/`xpPoints`/`thrusting`/…） | **加功能状态容器** |
| `player/PlayerStore.java` | `<world>/xpjetpack/players.json` 持久化（Gson） | 不动（自动带上新字段） |
| `util/XpUtil.java` | **经验读写入口**：`available` / `hasFuel` / `consume` / `seconds` | **扩展为统一经验 API** |
| `logic/JetpackServerLogic.java` | 服务端权威 tick：判定→施力→烧经验→同步 | **加一行功能分发** |
| `logic/PacketCodec.java` | 网络编解码分流（1.20.5+ 载荷 / 旧版裸缓冲） | 加新包分流 |
| `logic/ServerNetworkHandler.java` | 服务端收包处理 | 不动 |
| `logic/AntiKick.java` | 反射清 `aboveGroundTickCount` 防踢 | 不动 |
| `physics/JetpackPhysics.java` | 推力物理 | 不动 |
| `command/FlyCommand.java` | `/fly` 指令树 | 不动 |
| `network/{Input,State}Packet.java` | DTO + 手写编解码 | 不动 |
| `network/{Input,State}Payload.java` | 1.20.5+ 载荷包装（`//@only MC>=12005`） | 不动 |
| `client/*` | 客户端预测、HUD、按键 | 不动 |

### 加载器适配层

| 文件 | 作用 |
|---|---|
| `forge/XpJetpackForge.java` | 主类（`@Mod`）、注册配置 |
| `forge/ForgeEvents.java` | **FORGE 总线**：指令注册、ServerTick、玩家登出 |
| `forge/ForgeModEvents.java` | MOD 总线：配置加载/重载 |
| `forge/ForgeNetwork.java` | 网络通道（1.20.2+ 用 `ChannelBuilder`） |
| `forge/ForgeClientSetup.java` / `ForgeClientEvents.java` | 客户端发包 / HUD |
| `neoforge/XpJetpackNeoForge.java` | 主类、注册配置、事件监听 |
| `neoforge/NeoConfig.java` | **ModConfigSpec 配置声明**（Forge 版由 `transform_config()` 自动转换） |
| `neoforge/NeoNetwork.java` / `NeoClientSetup.java` | 网络 / 客户端 |
| `fabric/XpJetpackFabric.java` | 主入口 |
| `fabric/FabricConfig.java` | **JSON 配置**（Gson 自动序列化 POJO，**加字段零改动**） |
| `fabric/FabricNetworkServer.java` / `client/*` | 网络 / 客户端 |

---

## 4. 四个关键入口（本次扩展要挂的地方）

### 4.1 经验消耗入口

`util/XpUtil.java`（**唯一入口，但当前只服务喷气**）：

```java
public static int available(Player player)          // player.totalExperience
public static boolean hasFuel(Player player, JetpackConfig cfg)
public static int consume(Player player, int amount) // player.giveExperiencePoints(-take)
```

调用点仅 3 处：`JetpackServerLogic.burnXp()`（2 处）、`FlyCommand.status()`（1 处只读）。
**→ 抽象为统一 API 时风险可控，喷气行为可保持不变。**

### 4.2 配置入口

- 声明：`config/JetpackConfig.java`（POJO）
- Forge/NeoForge：`neoforge/NeoConfig.java` 的 `ModConfigSpec` → Forge 版由 `gen.py:transform_config()` 把 `ModConfigSpec` 文本替换成 `ForgeConfigSpec` 自动得到
- Fabric：`FabricConfig.java` 用 **Gson 反射序列化 POJO**，**新增字段无需改任何代码**

### 4.3 网络入口

- 1.20.5+：`InputPayload` / `StatePayload`（`CustomPacketPayload` + `StreamCodec`）
- ≤1.20.4：裸 `ResourceLocation` 通道 + 手写 `FriendlyByteBuf` 编解码
- 分流集中在 `logic/PacketCodec.java`

**本次 4 个功能全部是「服务端自动运行 + 玩家自己开关」，不需要新增 C2S 包**（指令走原版指令系统即可），最多需要扩展 S2C 状态包。

### 4.4 指令入口

- 三加载器各自的注册点（`ForgeEvents.onRegisterCommands` / `XpJetpackNeoForge` 构造器 / `XpJetpackFabric.onInitialize`）都调用 `FlyCommand.register(dispatcher)`
- **权限分叉**：`//#if MC>=12111` 用 `.requires(source -> true)`；否则用 `source.hasPermission(n)`
  （1.21.11/26.x 引入了 `PermissionSet`，没有 `hasPermission(int)`）

### 4.5 玩家数据

`JetState` 直接落盘（Gson），新增字段**自动持久化**；读取后 `clampSelf()` 做自愈。
`JetState` 里已有 `transient` 字段的用法（不落盘的运行时字段），可照样扩展。

---

## 5. 版本差异现状（已解决的分叉点，本次要复用同款写法）

| 分叉 | 阈值 | 写法 |
|---|---|---|
| `ResourceLocation` → `Identifier` | `MC>=12111` | `XpJetpack.id()` 内部消化，**业务代码不用管** |
| `CommandSourceStack#hasPermission` | `MC>=12111` | 指令 `requires` 分叉 |
| `Minecraft#screen` 字段 | `MC>=12111` | 客户端判定分叉 |
| `GuiGraphics` → `GuiGraphicsExtractor` | `MC>=26000` | HUD 分叉 |
| NeoForge `ModConfig.Type.COMMON` → `SYNCED` | `MC>=26300` | 配置注册分叉 |
| NeoForge 客户端发包 `PacketDistributor` → `ClientPacketDistributor` | `MC>=12105` | 发包分叉 |
| Fabric `PayloadTypeRegistry.playC2S` → `serverboundPlay` | `MC>=26000` | 注册分叉 |
| Forge `SimpleChannel` 包路径 | `MC>=12002` | 导入分叉 |

生成器支持的指令：`//#if` / `//#elif` / `//#else` / `//#endif` / `//@only MC>=xxxxx`（整文件排除）+ `{{TOKEN}}` 替换。

---

## 6. 现有代码里**没有**的东西（本次要新建）

| 缺失 | 说明 |
|---|---|
| ❌ Feature 接口 / 功能注册表 | 现在只有喷气一个功能，直接写在 `JetpackServerLogic` 里 |
| ❌ 事件分发器 | 无；只有硬编码的 ServerTick |
| ❌ 统一的「按秒扣经验」累加器 | `JetState.burn` 是喷气专用的 1.0→0.0 比例模型 |
| ❌ 聊天提示工具（可限频） | 现在只有 `player.sendSystemMessage(...)` 直调 |
| ❌ 伤害事件 / 挖掘事件挂钩 | 完全没有 |
| ❌ 矿石标签 / 配方查询工具 | 完全没有 |
| ❌ 多指令根 | 只有 `/fly` |

---

## 7. 压缩空气残留代码检查

按要求核查「压缩空气」是否已彻底清除：

```bash
rg -n "compressed|compressedAir|airPerXp|maxAir|airCost|state\.air" templates/ out/
→ 无匹配（仅 README 历史说明与本次无关）
```

**结论：无残留。** 当前唯一燃料模型是「直接消耗经验点」，`JetState.burn` 只是**单点经验的分数进度**（1.0→0.0），不是独立资源。

---

## 8. 不清晰项 / 需要主代理裁决的点

1. **`JetpackConfig` 既是喷气配置又是全局配置**：新功能的 `global.*` 键要不要塞进同一个类？
   → 建议：塞同一个类（简单、Fabric Gson 免改），但用注释分段标记 `// ==== XP Survival Suite ====`。
2. **S2C 状态包要不要带上四个功能的开关**：HUD 目前只显示经验值。
   → 本轮先不加（4 个功能是服务端自动跑，客户端不需要知道），保持网络层零改动。
3. **`/fly status` 是否合并进 `/xps status`**：保持 `/fly` 原样不动，`/xps status` 汇总展示全部（含喷气）。

---

**报告结论：仓库结构清晰、扩展点明确（4 个入口 + 生成器自动带文件），本次增量开发不需要重写任何喷气代码；抽象统一经验 API 的风险面仅 3 个调用点。可以进入接口约定阶段。**
