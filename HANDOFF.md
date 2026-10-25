# 🤖 项目交接文档（v1.3.1）

> 最后更新：2026-10-25 · 插件 v1.3.1 · K10 固件 v3.3.2

## 1️⃣ 这是什么

**EssentialsX-Clone**：Minecraft 服务器管理插件（Spigot 1.21 / Java 21 / Maven），独家与 **行空板 K10（ESP32-S3）** 做数字孪生联动：服务器状态、数字城市统计、环境数据实时推送到设备屏幕。

| 端 | 技术 | 文件 | 运行环境 |
|---|---|---|---|
| 插件端 | Java 21 / Spigot API 1.21 | `src/main/java/org/ljcode/myPlugin/` | MC 服务器 |
| 设备端 | Arduino/C++（ArduinoJson v6） | `mn.txt` | 行空板 K10 |

通信：插件 `POST http://<k10.host>:<port>/mc_event`；K10 是 HTTP Server，插件是 Client。方向是“推送 + 回执”，不是设备反向控制。

## 2️⃣ 关键文件地图

```text
MyPlugin.java                 主类：装配、命令/监听器注册、reloadAll()
managers/
  K10TCPManager.java          队列/节流/重试/健康/Gson回执
  DigitalCityManager.java     城市统计与 CITY_* 推送（30s 仪表盘）
  EnvironmentDataScheduler.java 环境聚合报告（60s 一条）
  EnvironmentDataCalculator.java 温度/湿度/光照/风速/群系
  EnvironmentDataReceiver.java  回执处理（ACK/error/未知）
  HouseholdManager.java       住户扫描（床+门=一户，只统计床头）
  TeleportManager.java        统一传送入口（冷却/安全/eback）
  EconomyManager/BankManager  钱包/银行（金额有限性校验）
  ShopManager/HomeManager/WarpManager/AnnouncementManager/AnnouncementScheduler
listeners/
  K10DigitalTwinListener.java 玩家事件 → K10
  DigitalCityListener.java    事件 → 城市统计（受 block/chat 开关控制）
  ChatFormatListener.java     聊天格式（% 转义、ActionBar 回主线程）
  ChatHintListener.java       表情/提示（先于 ChatFormat）
  ProfanityFilterListener.java 脏话过滤（UUID 计数、退出清理）
  BlockBreakListener.java     自动拾取（HIGHEST + ignoreCancelled）
  ShopListener/MenuListener   只处理 GUI 顶层点击
  UpdateCheckerListener.java  GitHub Releases 更新检查
commands/                     20 个命令类
gui/ShopGUI.java              9 格商店
web/WebServer.java            NanoHTTPD 8080 + /api/config
resources/config.yml          唯一配置真相源（v1.3.0 已清理死键）
resources/web/index.html      Schema 驱动的配置面板
mn.txt                        K10 固件（v3.3.2）
```

三层架构：`commands/` → `managers/` → `listeners/`；GUI 在 `gui/`，Web 在 `web/`，测试在 `src/test/java`。

## 3️⃣ 通信协议速查（两端字段必须一致）

| 消息 | 构造处 | 固件处理 | 关键字段 |
|---|---|---|---|
| 传统事件 | K10DigitalTwinListener | handleMcEvent | `event`(player_join/quit/death/custom_msg) + `player`/`message` |
| 环境聚合 | EnvironmentDataScheduler | handleEnvironmentSummary | `event=environment_summary` + `request_id`/`player_count`/`players[]` |
| 城市初始化 | DigitalCityManager.sendCityInitialization | processCityInitData | `event_type=CITY_INIT` + `city_name`/`founded_date` |
| 城市仪表盘 | sendCityDashboard | processCityDashboardData | `event_type=CITY_DASHBOARD` + 四组嵌套统计（**无 recent_events**） |
| 城市事件 | addCityEvent | processCityEventData | `event_type=CITY_EVENT` + `event{type,source,description,color}` |
| 详细统计 | collectDetailedStatistics | 只打印日志 | `event_type=DETAILED_STATISTICS` |

**必须记住**：

- 固件 `getJsonValue()` 只支持**一层**点号嵌套；数组要显式迭代。
- `CITY_DASHBOARD` 固件单次解析（4KB 缓冲）；保持单帧 < 2KB 余量。不要再加 `recent_events` 这类大数组。
- `environment_summary` 最多 20 行；插件 `max-players-in-report` 不要超过 20。
- 城市/传统事件固件回 `{"status":"ok"}`；插件把它当正常 ACK，不再告警。环境报告回执带 `request_id`，按它路由。
- 数字经 ArduinoJson `float` 中转；`founded_date`/`timestamp` 会丢精度，固件不用于逻辑。
- `CITY_INIT` 只发一次；设备 WiFi 连接阶段（最多 30 秒）会丢，城市名保持默认。
- 事件白名单：`PLAYER_JOIN/QUIT/DEATH/STATUS_CHANGE/MILESTONE/HOUSING_CHANGE/REDSTONE_SURGE`。
- AP 配网模式没有 `/mc_event`、`/reset`；配网完成重启后才注册业务路由。

## 4️⃣ 当前状态（v1.3.1）

- ✅ `mvn clean package` 成功；产物 `target/myplugin-1.3.1.jar`（约 596KB shaded）
- ✅ 4 个 JUnit 5 测试通过（金额校验、版本比较）
- ✅ 修复全新数据目录启动失败（`warps.yml`/`teleport.yml`）
- ✅ 修复 K10 回执告警刷屏、仪表盘大帧静默丢弃、队列丢唤醒
- ✅ 修复命令/经济/商店/菜单/方块破坏/聊天/传送等多处逻辑漏洞
- ✅ Web 配置面板重写，支持 K10/环境/数字城市配置
- ✅ 文档同步：README/CHANGELOG/HANDOFF/DIGITAL_CITY_GUIDE
- ⚠️ 未在真实 MC 服务器 + 真实 K10 上做端到端联调；发布前建议按第 6 节验证

## 5️⃣ 已知遗留（非 Bug 或需环境验证）

| 事项 | 位置 | 说明 |
|---|---|---|
| 城市状态阈值硬编码 | `DigitalCityManager.checkCityStatus()` | 文档已注明；如加配置需同步文档 |
| `founded_date` 精度 | `mn.txt processCityInitData` | 毫秒时间戳经 float 丢精度，赋值后未使用 |
| `total_volume` 单位是“分” | `DigitalCityManager.recordEconomyTransaction` | K10 不显示金额；要显示需 ÷100 |
| `DETAILED_STATISTICS` 未消费 | `mn.txt handleCityEvent` | 固件只打印串口日志 |
| 玩家等级为随机演示值 | `ChatFormatListener` | 未持久化，仅用于显示 |
| 聊天历史仅内存 | `ChatHintListener` | 退出清理，不持久化 |
| K10 无断线重连 | `mn.txt setup()` | 断网后需重启设备 |

## 6️⃣ 构建 / 测试 / 烧录 / 发布

```powershell
# 编译 + 测试 + shaded JAR（推荐）
& "E:\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" `
  -f "E:\minecraft dev\my plugin\pom.xml" clean package

# 仅测试
mvn test

# 产物
target/myplugin-1.3.1.jar
```

- 本机 JDK 26 编译 target 21 会有模块路径警告，不影响。
- 首次跑测试需要 Maven 能访问仓库（JUnit 5 / Surefire provider）。
- K10 固件：Arduino IDE 打开 `mn.txt`，板子选 **ESP32-S3-N8R2**，上传。
- 发布：`git tag v1.3.1 && git push origin main --tags`，再 `gh release create v1.3.1 target/myplugin-1.3.1.jar`。

## 7️⃣ 环境坑位（重要）

1. **换 JAR 必须重启服务器**；`/reload` 不会重启环境/城市调度器，也不会换 Web 端口。
2. 插件日志出现“未知响应类型”时，先确认版本；v1.3.0 对裸 `{"status":"ok"}` 已不再告警。
3. **改 K10 相关代码必须同步 `mn.txt`**：字段名、嵌套层级、单帧大小、回执形状、16KB/20 行上限。
4. **`config.yml` 是唯一配置真相源**；新增键必须三处同步：`config.yml` + 代码读取 + 文档/Web schema。
5. **Mind+ 编译 C++ 陷阱**：默认参数只能写在声明或定义其中一处；当前 `getJsonValue`/`extractJsonObject` 默认参数在前置声明。
6. PowerShell 5.1 的 `Set-Content` 默认按 ANSI 写文件，会破坏中文 UTF-8；改文件请用编辑器/UTF-8 工具。
7. 本机 Maven 访问 central 可能超时，但 `curl` 可下载；测试依赖已缓存到本地 `.m2`。
8. `mn.md` 已移除，只维护 `mn.txt`。
9. 高危命令（`/enuke`、`/eall*`、`/eflameblade`、`/e12503`、`/holo`）均有权限门禁；不要为了方便去掉。
10. Web 保存通过 `callSyncMethod` 回主线程重载；不要在 NanoHTTPD 线程直接调用 Bukkit 调度器。

## 8️⃣ 接手后第一件事

```bash
git log --oneline -10          # 看最近提交
# 读本文件 → 读 config.yml → 读 K10TCPManager.java → 读 mn.txt handleMcEvent()
# 改动前先跑一次 mvn clean package 确认基线
```
