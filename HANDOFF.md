# 🤖 项目交接文档 (AI/开发者 Handoff)

> **目的**：让下一个接手者（人类或 AI）在 5 分钟内掌握项目全貌、现状与坑位。
> **最后更新**: 2026-08-17 · 对应版本 v1.1.0 · 提交 033c4a6

---

## 1️⃣ 这是什么项目

**EssentialsX-Clone**：Minecraft 服务器管理插件（Bukkit/Spigot 1.21 + Java 21 + Maven），仿 EssentialsX，
**独家功能是与行空板 K10 硬件的双向数字孪生联动**——服务器数据实时推送到 K10 屏幕显示。

**双端架构**（改一端必须核对另一端协议）：

| 端 | 语言/框架 | 文件 | 运行环境 |
|---|---|---|---|
| 插件端 | Java 21 / Spigot API 1.21 | `src/main/java/org/ljcode/myPlugin/` | MC 服务器 |
| 设备端 | C++ / Arduino (ESP32-S3) | `mn.txt`（标准缩进版）/ `mn.md`（原始备份，内容一致） | 行空板 K10 |

**通信**：插件 → `POST http://{k10.host}:{k10.port}/mc_event`（单向推送 + K10 同步回响应）。K10 是 HTTP Server，插件是 Client。

---

## 2️⃣ 关键文件地图

```
MyPlugin.java                  主类：装配所有 Manager/Listener，K10响应回调路由(88-91行)
managers/
  K10TCPManager.java           ★核心：HTTP发送/重试/节流500ms/健康判定/Gson解析K10响应
  DigitalCityManager.java      城市统计采集与 CITY_* 事件推送（含CityEvent内部类, color格式#RRGGBB）
  EnvironmentDataScheduler.java 环境数据轮询计算（同步任务！间隔动态调整5-30秒）
  EnvironmentDataCalculator.java 环境数据算法（温度/湿度/光照/风速/天气/群系）
  EnvironmentDataReceiver.java  K10环境响应处理
  BankManager.java             银行（独立bank.yml，与钱包economy.yml分离）
  EconomyManager.java          钱包/交易（5分钟自动保存，异步内runTask跳回主线程）
  HomeManager/WarpManager/TeleportManager.java  传送三件套
listeners/
  K10DigitalTwinListener.java  玩家join/quit/death/chat → 传统事件推送
  MenuListener/ShopListener/PlayerListener...   各GUI与玩家事件
web/WebServer.java             NanoHTTPD配置台(8080)，/api/*有IP白名单+可选token
resources/config.yml           全部真实配置键（文档示例均已对齐此文件）
mn.txt                         K10固件源码（Arduino IDE烧录，板子ESP32-S3-N8R2）
```

三层架构：`commands/`（命令入口）→ `managers/`（业务）→ `listeners/`（事件）。GUI 在 `gui/`，测试在 `src/test/java/`。

---

## 3️⃣ 通信协议速查（两端字段名必须严格一致）

K10 端入口 `handleMcEvent()` 按字段分发，两套约定并存：

| 消息类型 | 插件构造处 | K10处理函数 | 关键字段 |
|---|---|---|---|
| 传统事件 | K10DigitalTwinListener | handleMcEvent | `event`(player_join/quit/death/custom_msg) + `player`/`message` |
| 环境数据 | EnvironmentDataScheduler.buildJsonMessage | handleEnvironmentData | `event`="environment_data" + player_name/request_id/temperature/humidity/light/wind_speed/weather/biome |
| 城市初始化 | DigitalCityManager.sendCityInitialization | processCityInitData | `event_type`="CITY_INIT" + city_name/founded_date |
| 城市仪表盘 | sendCityDashboard(30秒/次,可配) | processCityDashboardData | `event_type`="CITY_DASHBOARD" + 嵌套 basic_stats./population_stats./economy_stats./activity_stats. |
| 城市事件 | addCityEvent | processCityEventData | `event_type`="CITY_EVENT" + 嵌套`event`{type,source,description,color:"#RRGGBB"} |

**注意**：
- K10 端 `getJsonValue()` 只支持**一层**嵌套（`basic_stats.tps` 可以，三层不行）
- K10 用 ArduinoJson：数字会被转成 `"5.00"` 字符串再 toInt()/toFloat()，正常工作
- K10 环境响应含 `request_id`，MyPlugin 88 行据此路由到调度器；普通事件响应 `{"status":"ok"}` 无此字段不会误路由
- 插件端节流按 `event_type`/`event` 字段区分类型，同类型 500ms 内去重

---

## 4️⃣ 当前状态（v1.1.0 已完成）

- ✅ 12 个逻辑级 Bug 全部修复（详见 CHANGELOG.md 1.1.0 条目）
- ✅ K10 代码已格式化为标准缩进（mn.txt），并补 economy_stats 联动（银行账户上屏）
- ✅ Web API 安全加固（IP 白名单 + 可选 token，默认仅本机）
- ✅ target/ 已移出 git 跟踪；web/index.html 冗余文件已删；测试迁至 src/test/java
- ✅ 文档（README/CHANGELOG/DIGITAL_CITY_GUIDE）已同步 v1.1.0
- ✅ 编译验证：`mvn clean package` 通过

---

## 5️⃣ 已知遗留（非 Bug，接手后可考虑）

| 事项 | 位置 | 说明 |
|---|---|---|
| 城市状态阈值硬编码 | DigitalCityManager.checkCityStatus() | 文档已注明"暂无配置项"；如加配置需同步改文档 |
| founded_date 溢出 | mn.txt processCityInitData | 毫秒时间戳超出32位 toInt()，但 cityFoundedTime 赋值后从未使用，无实际影响 |
| total_volume 单位是"分" | DigitalCityManager.recordEconomyTransaction | `amount*100` 存储；K10 端目前不显示金额，如要显示需 ÷100 |
| DETAILED_STATISTICS 未消费 | mn.txt handleCityEvent | 插件发送详细统计（生物群系/热力图/玩家位置），K10 仅打印日志未展示 |
| K10 文件名 | mn.md / mn.txt | mn.md 为乱缩进原始备份；**后续改 K10 代码一律改 mn.txt**，别动 mn.md |

---

## 6️⃣ 构建 / 测试 / 烧录

```powershell
# 编译（本机无独立 mvn，用 IntelliJ 自带）：
& "E:\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd" -f "e:\minecraft dev\my plugin\pom.xml" clean package
# 产物: target/myplugin-1.0-SNAPSHOT.jar (shaded, ~580KB)

# 测试: mvn test（BankSystemTest 在 src/test/java）
# K10固件: Arduino IDE 打开 mn.txt，板子选 ESP32-S3-N8R2，上传
```

---

## 7️⃣ 环境坑位提醒（重要！）

1. **git push 会被终端沙箱静默拦截**（无输出+假exit 0）。网络命令必须在真实终端执行（requires_approval=true），push 后必须 `git status -sb` 验证无 ahead。
2. **PowerShell 执行策略**阻止脚本/HEREDOC：git commit 多行信息用多个 `-m` 参数，别用 bash 语法。
3. git 身份已配置在**仓库级**（sunday-lil），全局未配置，新环境直接 commit 会报 Author identity unknown。
4. K10 固件无法本地编译验证，只能静态核对协议字段——**改插件 K10 相关代码时，务必对照 mn.txt 的 handleMcEvent/processCityDashboardData 同步核对**。
5. config.yml 是唯一配置真相源；文档中的 YAML 示例曾出现过死键（status-thresholds、bank.enabled），已全部清理，新增配置务必三处同步（config.yml + 代码读取 + 文档）。

---

## 8️⃣ 接手后第一件事建议

```bash
git log --oneline -10          # 看最近提交
# 读本文件 → 读 config.yml → 读 K10TCPManager.java → 读 mn.txt 的 handleMcEvent()
# 改动前先跑一次 mvn clean package 确认基线可编译
```
