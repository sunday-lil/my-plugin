# EssentialsX-Clone · Minecraft × 行空板 K10 数字孪生

[![Version](https://img.shields.io/badge/version-1.3.1-brightgreen)](CHANGELOG.md)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21-blue)](https://www.spigotmc.org/)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net/)

基于 **Spigot 1.21 / Java 21 / Maven** 的服务器管理插件。除 EssentialsX 风格的传送、家园、经济、银行、商店、公告、聊天与菜单外，还与 **行空板 K10（ESP32-S3）** 联动，把服务器状态、数字城市统计和环境数据实时推送到设备屏幕。

> 当前版本：**v1.3.1** · 设备固件：**mn.txt v3.3.2** · 最后更新：2026-10-25

---

## 功能概览

| 模块 | 说明 |
|---|---|
| 传送 | `/etp`、`/etpa`、`/etpahere`、`/etpaccept`、`/etpdeny`、`/eback`，统一冷却与安全检查 |
| 家园/传送点/出生点 | 多家园、公共传送点、`/espawn`、`/esetspawn` |
| 经济/银行 | 钱包、银行存取款、转账、手续费、余额榜、NaN/Infinity 防护 |
| 商店 | 9 格 GUI，买卖与全服库存 |
| 公告 | 聊天、动作栏、标题、BossBar，定时轮播 |
| 聊天 | 自定义格式、表情转换、命令建议、脏话过滤、进服欢迎/指南 |
| 菜单 | `/menu` 超级控制中心，5 个子菜单 |
| 特殊物品 | 火焰刀、满附魔套装、核弹法阵（均需权限） |
| 悬浮文字 | 静态/多行/在线人数/跟随，退出自动清理 |
| 数字孪生 | K10 城市大屏、环境表格、事件通知、LED 反馈 |
| 数字城市 | 人口/经济/活动/红石/住户统计，30 秒仪表盘 |
| 环境数据 | 每 60 秒聚合全玩家温湿度/光照/风速/天气/群系 |
| Web 面板 | 可视化编辑配置，覆盖 K10/环境/城市/经济/聊天等主要配置 |

---

## 系统架构

```text
Bukkit 事件 / 命令 / 定时任务
        │
        ├─ commands/ ──► managers/ ──► data/*.yml
        │
        ├─ listeners/ ──► DigitalCityManager ──┐
        │                                      ├─ Gson JSON
        └─ EnvironmentDataScheduler ───────────┘
                                               │
                                               ▼
                                   K10TCPManager（单线程队列）
                                               │ HTTP POST /mc_event
                                               ▼
                                     K10 固件（WebServer:80）
                                               │ 200 + JSON 回执
                                               ▼
                         responseCallback ──► EnvironmentDataScheduler（有 request_id）
                                          └──► EnvironmentDataReceiver（普通 ACK）
```

- **插件是 HTTP 客户端，K10 是 HTTP 服务端**：方向是“插件推送 + 设备回执”，不是设备反向控制服务器。
- 消息队列串行发送；相同类型消息 500ms 节流；设备离线时最多重试 3 次。
- Bukkit API 全部在主线程执行；HTTP 与环境/住户的纯计算在异步线程。

---

## 快速开始

### 环境要求

- Minecraft 服务器 **1.21+**（Spigot/Paper/Purpur）
- **Java 21+**
- Maven 3.8+（或 IntelliJ IDEA 自带 Maven）
- 可选：PlaceholderAPI 2.11.6+
- 硬件：行空板 K10（ESP32-S3），固件 `mn.txt` v3.3.2

### 构建

```powershell
mvn clean package
# 产物：target/myplugin-1.3.1.jar（shaded，约 596KB）
```

本机没有独立 Maven 时可用 IntelliJ 自带：

```powershell
& "E:\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" `
  -f "E:\minecraft dev\my plugin\pom.xml" clean package
```

### 安装

1. 将 `target/myplugin-1.3.1.jar` 放入服务器 `plugins/`。
2. **重启服务器**（不要只用 `/reload`，调度周期和 Web 端口不会重新初始化）。
3. 首次启动会生成 `plugins/EssentialsX-Clone/config.yml` 与数据文件。
4. 执行 `/version EssentialsX-Clone` 确认版本为 `1.3.1`。

### K10 固件

1. 用 Arduino IDE 打开 `mn.txt`。
2. 开发板选择 **ESP32-S3-N8R2**。
3. 首次配网：连接热点 `K10-MC-Setup`，浏览器打开 `http://192.168.4.1` 保存 WiFi。
4. 烧录后确认设备 IP 与 `config.yml` 的 `k10.host` 一致。

> 固件只维护 `mn.txt`（`mn.md` 已移除）。改插件协议时必须同步核对固件解析函数。

---

## K10 通信协议

### 插件 → K10

统一入口：`POST http://<host>:<port>/mc_event`，`Content-Type: application/json`。

| 消息 | 周期/触发 | 主要字段 | 固件处理 |
|---|---|---|---|
| `CITY_INIT` | 启动后 5 秒，仅一次 | `city_name`、`founded_date` | 设置城市名 |
| `CITY_DASHBOARD` | 10 秒后首次，之后 30 秒 | `basic_stats`、`population_stats`、`economy_stats`、`activity_stats` | 刷新城市大屏 |
| `CITY_EVENT` | 加入/退出/死亡/天气/里程碑/住户/红石激增 | `event{type,source,description,color}` | 4 秒全屏通知 + LED |
| `DETAILED_STATISTICS` | 30 秒后首次，之后 5 分钟 | 群系/热力图/玩家位置 | 仅串口日志 |
| `environment_summary` | 5 秒后首次，之后 60 秒 | `players[]{name,temperature,humidity,light,wind_speed,weather,biome}` | 刷新环境表格（≤20 行） |
| `player_join/quit/death/custom_msg` | 事件触发 | `player`、`message` 等 | 通知 + LED |

### K10 → 插件

- 环境聚合报告回执：`{"response_type":"acknowledgment","status":"success","request_id":"..."}`，插件按 `request_id` 路由到环境调度器。
- 城市/传统事件回执：`{"status":"ok"}`，属于正常 ACK，**没有 `request_id` 是正常的**；只有 `environment_summary` 使用 `request_id`。
- JSON 解析失败：HTTP 200 + `{"response_type":"error",...}`，插件日志会记录错误。
- K10 在 HTTP handler 内同步刷屏，首次仪表盘可能超过 3 秒；`k10.connection-timeout` 只用于连接，`k10.read-timeout`（默认 8000ms）用于等待响应。

### 固件约束（改协议前必读）

- `getJsonValue()` 只支持**一层点号嵌套**（`a.b`）；数组必须显式迭代。
- `CITY_DASHBOARD` 单次解析（4KB 缓冲）；请保持单帧 < 2KB 的余量（当前约 0.7KB）。
- `environment_summary` 最多 20 行；插件 `max-players-in-report` 不要超过 20。
- 数字经 ArduinoJson `float` 中转，13 位时间戳会丢精度；固件不使用这些字段做逻辑。
- 事件类型白名单：`PLAYER_JOIN/QUIT/DEATH/STATUS_CHANGE/MILESTONE/HOUSING_CHANGE/REDSTONE_SURGE`。
- AP 配网模式不注册 `/mc_event` 与 `/reset`；WiFi 连接阶段最多阻塞 30 秒。
- `CITY_INIT` 只发一次，不会补发；设备启动慢时城市名会保持默认值。

---

## 命令与权限

完整列表见 [`plugin.yml`](src/main/resources/plugin.yml)。

### 传送/家园/传送点

| 命令 | 权限 | 说明 |
|---|---|---|
| `/etp <玩家>` | `essentialsx.tp.others`（他人）/ `essentialsx.tp.self`（自己） | 传送到玩家 |
| `/etpa <玩家>` / `/etpahere <玩家>` | `essentialsx.tp` | 发送传送请求 |
| `/etpaccept` / `/etpdeny` | — | 接受/拒绝请求 |
| `/ehome [名称]` | `essentialsx.home.teleport` | 回家 |
| `/esethome [名称]` | `essentialsx.home.set` | 设置家 |
| `/ewarp [名称]` | `essentialsx.warp.teleport` | 传送到传送点/列出 |
| `/esetwarp <名称>` | `essentialsx.warp.set` | 设置传送点 |
| `/espawn` / `/esetspawn` | — / `essentialsx.admin` | 出生点 |
| `/eback` | `essentialsx.back` | 返回上一位置 |

传送统一受 `teleport.cooldown`、`teleport.safety-check` 控制；`essentialsx.teleport.bypass` 可绕过。

### 经济/银行/商店

| 命令 | 权限 | 说明 |
|---|---|---|
| `/emoney [玩家]` | `essentialsx.money.see` / `.others` | 查余额 |
| `/epay <玩家> <金额>` | `essentialsx.money.pay` | 转账 |
| `/ebalancetop` | `essentialsx.money.baltop` | 余额榜 |
| `/eco set <玩家> <金额>` | `essentialsx.admin` | 设置余额 |
| `/bank balance/deposit/withdraw/transfer` | `bank.use` + 子权限 | 银行 |
| `/shop` | `essentialsx.shop` | 商店 GUI |

### 玩家管理/公告/特殊

| 命令 | 权限 | 说明 |
|---|---|---|
| `/efly [玩家]` | `essentialsx.fly` / `.others` | 飞行 |
| `/egod [玩家]` | `essentialsx.god` / `.others` | 无敌 |
| `/eheal [玩家]` / `/efeed [玩家]` | `essentialsx.heal` / `.others`、`essentialsx.feed` / `.others` | 治疗/喂食 |
| `/egm <0-3> [玩家]`（别名 `/gamemode`） | `essentialsx.gm` / `.others` | 游戏模式 |
| `/ean`、`/eanactionbar`、`/eantitle`、`/eanbossbar` | `chatannouncements.send.*` | 公告 |
| `/eanreload` | `chatannouncements.reload` | 热重载 |
| `/menu` | — | 控制中心 |
| `/ehelp` | — | 功能指南 |
| `/edebug` | `essentialsx.debug` | 调试信息 |
| `/e12503` / `/eflameblade` | `essentialsx.admin` | 特殊攻击/火焰刀 |
| `/eall66` / `/eall22` | `myplugin.eall66` / `myplugin.eall22` | 满附魔装备 |
| `/enuke` | `myplugin.enuke` | 核弹法阵（60 秒冷却） |
| `/holo ...` | `essentialsx.holo`（`clearall/stats` 需 `essentialsx.admin`） | 悬浮文字 |

---

## 配置说明

完整默认值见 [`config.yml`](src/main/resources/config.yml)。主要分组：

```yaml
k10:                 # K10 地址、端口、重试、事件开关、debug-mode
environment:         # 扫描半径、报告周期（ticks）、单条玩家上限
digital-city:        # 城市名、仪表盘/统计周期、红石、住户扫描
economy:             # 是否启用、初始资金
bank:                # 开户余额、存取款限额、手续费
homes:               # 是否启用、家园上限
warps:               # 是否启用、传送点上限
teleport:            # 是否启用、请求超时、冷却、安全检查
player-commands:     # efly/egod/eheal/efeed/egm 开关
chat:                # 格式、颜色、动作栏、hints 子项
announcements:       # 轮播间隔、随机顺序、BossBar 颜色/样式
join-message:        # 欢迎标题/动作栏/聊天/烟花/指南
profanity-filter:    # 词库、阈值、警告/踢出消息
special-items:       # 火焰刀
gameplay:            # 自动拾取、粒子拖尾
update-check:        # GitHub Release 更新检查
system:              # 自动保存间隔（分钟）
web-server/web:      # Web 端口、API 白名单、token
debug-info:          # 默认 Ping、时间段
```

> 修改配置后执行 `/eanreload` 或使用 Web 面板保存；调度周期会重启任务，Web 端口需重启服务器。

---

## Web 配置面板

启动后访问 `http://localhost:8080`（端口可配置）。

- 打开页面自动 `GET /api/config` 加载当前配置。
- 支持 K10、环境、数字城市、经济、银行、家园、传送、聊天、公告、加入消息、Web、玩法等分组。
- 保存时 `POST /api/config` 合并提交的键，不会清空未提交的配置。
- `/api/*` 受 IP 白名单（`web.allowed-ips`）与可选 Token（`web.api-token`）保护。
- 如果配置了 Token，在页面右上角填写；Token 会保存在浏览器 localStorage。

> 安全提示：不要把 8080 端口直接暴露到公网；默认只允许本机与内网网段。

---

## 数据文件

| 文件 | 内容 | 保存时机 |
|---|---|---|
| `config.yml` | 主配置 | Web 保存 / `/eanreload`（内存） |
| `economy.yml` | 钱包余额 | 定时（`system.auto-save-interval`）+ 关服 |
| `bank.yml` | 银行账户 | 关服 |
| `homes.yml` | 家园 | 关服 |
| `warps.yml` | 传送点 | 关服 |
| `teleport.yml` | `/back` 位置 | 关服 |
| `spawn.yml` | 出生点 | `/esetspawn` 时立即保存 |

> `warps.yml`、`teleport.yml` 已随 JAR 提供；即使缺失也会安全创建，不再导致首次启动失败。

---

## 构建与测试

```powershell
mvn clean package          # 编译 + 运行测试 + shaded JAR
mvn test                   # 仅测试
mvn -DskipTests package    # 跳过测试
```

测试位于 `src/test/java`，当前包含：

- `BankUtilsTest`：金额解析、0/负数/NaN/Infinity 拒绝
- `VersionComparisonTest`：语义版本比较

需要 JUnit 5 与可访问的 Maven 仓库。构建成功后生成 `target/myplugin-1.3.1.jar`。

---

## 故障排除

### 插件启动失败 / 全新服务器

- 确认 JAR 完整（约 596KB），且 `warps.yml`/`teleport.yml` 已随 JAR 提供（v1.3.0 已修复）。
- 检查控制台是否有 `IllegalArgumentException` 或 `UnsupportedClassVersionError`（需要 Java 21+）。

### K10 收不到数据

1. `ping <k10.host>`，确认同一局域网。
2. 检查防火墙 80 端口。
3. 打开 `k10.debug-mode: true`，观察 `[K10数字孪生]` 日志。
4. 确认固件为 v3.3.2，设备 IP 与配置一致。
5. 设备 AP 配网模式下没有 `/mc_event`，必须先完成 WiFi 配网。

### 日志出现“未知响应类型”

v1.3.1 起，城市/传统事件的裸 `{"status":"ok"}` 会被识别为正常 ACK，不再输出“处理响应类型: 空 / 无 request_id”的 INFO 日志。若仍出现：

- 检查是否有旧版 JAR 残留（`/version EssentialsX-Clone`）。
- 检查固件是否返回了非预期 JSON（开启 `environment.debug-mode`）。

### 城市大屏不更新

- `CITY_DASHBOARD` 单帧过大或固件解析失败会导致静默跳过；v3.3.2 已改为单次 16KB 解析。
- 确认 `digital-city.debug-mode: true` 时日志有“数据已加入 K10 发送队列”。
- 检查 K10 是否处于配网/连接页（`networkReady` 前不显示业务界面）。

### 环境表格为空

- 必须有玩家在线；无人在线时插件不发报告。
- 等待一个 `environment.report-interval`（默认 60 秒）。
- 确认 `max-players-in-report` ≤ 20（固件上限）。

### Web 面板打不开 / 保存失败

- 确认 8080 未被占用；端口修改需重启。
- 若配置了 `web.api-token`，在页面右上角填写。
- 保存失败会返回具体 HTTP 状态；查看服务器日志中的 `[Web]` 记录。

---

## v1.3.1 变更

- 新增 `k10.read-timeout`（默认 8000ms），与 `k10.connection-timeout`（连接 3000ms）分离，避免 K10 首次绘制仪表盘超过 3 秒被误判为发送失败。
- 超时日志区分“连接超时”和“读取超时”，并说明会自动重试。
- `CITY_INIT`/`CITY_DASHBOARD`/`CITY_EVENT` 的裸 ACK 不再输出“处理响应类型: 空 / 请求成功确认: 无 request_id”；这类回执没有 `request_id` 是正常的。
- Web 面板 K10 分组新增“读取超时”字段。

---

## v1.3.0 变更

完整修复列表见 [CHANGELOG.md](CHANGELOG.md)。重点：

- 修复全新数据目录首次启动因缺失 `warps.yml`/`teleport.yml` 失败。
- 修复 K10 城市回执被误报“未知响应类型”的日志刷屏。
- 修复仪表盘超过 2KB 时固件静默丢弃（改为单次解析，4KB/8KB 缓冲；移除未消费的 `recent_events`）。
- 修复 `/egm` 参数解析、`/eco` 识别、`/esetspawn` 双实例缓存、`/back` 方向、NaN 金额注入。
- 修复菜单/商店点击自己背包物品触发功能、方块破坏复制/保护绕过、火焰刀岩浆残留。
- 修复聊天管线：脏话消息不再发往 K10、表情转换真正生效、异步线程不再发包、欢迎指南不再重复。
- 修复 Web 面板 JSON POST 保存 500、自动加载配置、布尔值误判、K10/环境/城市配置缺失。
- 优化：环境扫描去掉未使用的资源统计并复用湿度；住户只统计床头；K10 消息日志降噪；核弹粒子/TNT 限流；统一传送冷却/安全检查。
- 测试：新增真实 JUnit 5 测试（金额校验、版本比较）。

### 不兼容变更

- `chat.hints.hints-enabled` 更名为 `chat.hints.enabled`（旧键仍作为 fallback 读取）。
- 移除无效配置：`economy.currency-symbol`、`economy.decimal-places`、`homes.teleport-cooldown`、`system.debug-mode`、`permissions.*`、`chat-format.actionbar-notification.title-*`。
- 新增权限节点：`essentialsx.holo`、`essentialsx.debug`、`essentialsx.back`、`essentialsx.shop`、`essentialsx.teleport.bypass`、`essentialsx.*.others`、`myplugin.*`。

---

## 文档

- [CHANGELOG.md](CHANGELOG.md) — 版本变更
- [DIGITAL_CITY_GUIDE.md](DIGITAL_CITY_GUIDE.md) — K10 数字城市完整指南
- [HANDOFF.md](HANDOFF.md) — 开发/接手说明与坑位
- [CONTRIBUTING.md](CONTRIBUTING.md) — 贡献指南
- [SECURITY.md](SECURITY.md) — 安全策略

---

## 许可

本项目使用仓库中的 [LICENSE](LICENSE)。Minecraft 是 Mojang 的商标，本项目与 Mojang 无关。
