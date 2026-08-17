# 🔧 EssentialsX-Clone - 全功能 Minecraft 服务器管理插件

<p align="center">
  <strong>一个功能丰富的 Minecraft EssentialsX 克隆插件，专为中文服务器打造</strong><br>
  <em>基于 Spigot API 1.21 | Java 21 | 模块化架构设计</em>
</p>

<p align="center">
  <a href="https://github.com/sunday-lil/my-plugin/releases/latest"><img src="https://img.shields.io/github/v/release/sunday-lil/my-plugin?color=blue&label=最新版本" alt="Latest Release"></a>
  <a href="https://github.com/sunday-lil/my-plugin/blob/main/LICENSE"><img src="https://img.shields.io/github/license/sunday-lil/my-plugin?color=green" alt="License"></a>
  <a href="https://github.com/sunday-lil/my-plugin/issues"><img src="https://img.shields.io/github/issues/sunday-lil/my-plugin?color=orange" alt="Issues"></a>
  <a href="https://github.com/sunday-lil/my-plugin/pulls"><img src="https://img.shields.io/github/issues-pr/sunday-lil/my-plugin?color=purple" alt="Pull Requests"></a>
  <br>
  <img src="https://img.shields.io/badge/Java-21-orange" alt="Java Version">
  <img src="https://img.shields.io/badge/Minecraft-1.21+-green" alt="Minecraft Version">
  <img src="https://img.shields.io/badge/Spigot-API-1.21-blue" alt="Spigot API">
  <img src="https://img.shields.io/badge/Build-Maven%203.6+-red" alt="Build Tool">
</p>

<p align="center">
  <a href="#项目简介">📖 简介</a> •
  <a href="#安装指南">📦 安装</a> •
  <a href="#功能模块详解">✨ 功能</a> •
  <a href="#项目架构与技术细节">🏗️ 架构</a> •
  <a href="#开发指南">🛠️ 开发</a> •
  <a href="#文档导航">📚 文档</a> •
  <a href="#贡献指南">🤝 贡献</a> •
  <a href="#许可证">📄 许可证</a>
</p>

---

## ✨ 项目简介

EssentialsX-Clone 是一个**企业级 Minecraft 服务器管理插件**，采用模块化架构设计，集成了 14 大核心功能模块。不仅完整复刻了 EssentialsX 的经典功能，还创新性地加入了 **K10 数字孪生硬件联动系统** 和 **Web 可视化配置界面**，是中文 Minecraft 服务器的理想选择。

### 🎯 核心优势

- ✅ **开箱即用** - 一键安装，自动生成配置文件
- ✅ **高度可配置** - 所有功能均可通过 YAML 配置文件精细调整
- ✅ **性能优化** - 异步处理、消息队列、连接池等技术保障服务器流畅运行
- ✅ **硬件联动** ⭐ - 支持与行空板 K10 实现实时数据互通
- ✅ **Web 管理** - 浏览器即可完成所有配置，无需手动编辑文件
- ✅ **完整测试** - 核心功能单元测试覆盖，质量有保障

---

## 📦 安装指南

### 系统要求

| 项目 | 要求 |
|------|------|
| Minecraft 版本 | **1.21+** |
| Java 版本 | **21+** |
| 服务器核心 | **Spigot** 或其分支（Paper/Purpur 等）|
| 构建工具 | Maven 3.6+ |

### 快速安装（3步搞定）

```bash
# 1️⃣ 构建插件（或直接下载已编译的 JAR）
mvn clean package

# 2️⃣ 将生成的 JAR 文件放入服务器的 plugins 文件夹
#    target/myplugin-1.0-SNAPSHOT-shaded.jar → plugins/

# 3️⃣ 重启服务器或执行 /reload 命令
```

### 首次启动后

插件会自动生成以下配置文件：
```
plugins/MyPlugin/
├── config.yml          # 主配置文件（包含所有功能开关和参数）
├── economy.yml         # 经济系统数据
├── homes.yml           # 家园位置数据
└── web/                # Web 配置界面资源文件
    └── index.html      # 配置面板页面
```

---

## 🚀 功能模块详解

### 1️⃣ 📍 传送系统（Teleport System）

完整的玩家传送解决方案，包含 5 种传送方式。

**核心命令：**
```
/etp <玩家>              # 直接传送到指定玩家
/etpa <玩家>             # 请求传送到其他玩家（需对方同意）
/etpahere <玩家>         # 请求玩家传送到你这里
/etpaccept               # 接受传送请求
/etpdeny                 # 拒绝传送请求
/eback                   # 返回上一个位置
```

**特色功能：**
- ✅ 传送请求超时机制（默认 60 秒）
- ✅ 传送冷却时间（防止滥用）
- ✅ 安全检查（防止传送到危险区域）
- ✅ 传送历史记录（支持 /eback 返回）

**配置示例：**
```yaml
teleport:
  enabled: true
  request-timeout: 60     # 请求超时（秒）
  cooldown: 5             # 冷却时间（秒）
  safety-check: true      # 启用安全检查
```

---

### 2️⃣ 🏠 家园系统（Home System）

让每位玩家都能拥有自己的"安全屋"。

**核心命令：**
```
/esethome [名称]         # 设置当前位置为家园（默认名: home）
/ehome [名称]            # 传送到指定家园
```

**特色功能：**
- ✅ 多家园支持（普通玩家 5 个，VIP 玩家 10 个）
- ✅ 自定义家园名称
- ✅ 权限控制（可通过权限节点扩展数量）
- ✅ 传送冷却机制

**配置示例：**
```yaml
homes:
  enabled: true
  max-homes: 5                    # 默认最大家园数
  max-homes-with-permission: 10   # 有权限玩家的最大数
  teleport-cooldown: 5            # 传送冷却（秒）
```

---

### 3️⃣ 🌀 传送点系统（Warp System）

管理员设置公共传送点，方便所有玩家快速到达关键地点。

**核心命令：**
```
/esetwarp <名称>         # 设置公共传送点（仅限 OP）
/ewarp <名称>            # 使用传送点
```

**适用场景：**
- 🏙️ 主城传送
- 🏗️ 建筑区入口
- ⛏️ 矿区入口
- 🎮 游戏竞技场

**配置示例：**
```yaml
warps:
  enabled: true
  max-warps: 5            # 最大传送点数量限制
```

---

### 4️⃣ 💰 经济系统（Economy System）

内置轻量级经济系统，无需依赖 Vault 或其他经济插件。

**核心命令：**
```
/emoney [玩家]            # 查看余额（不指定则查看自己）
/epay <玩家> <金额>       # 向其他玩家转账
/ebalancetop             # 查看财富排行榜 TOP 10
/eco set <玩家> <金额>   # 管理员设置余额（OP 专用）
```

**特色功能：**
- ✅ 新玩家初始资金可配置
- ✅ 自定义货币符号（$、金币、钻石💎 等）
- ✅ 小数位数可调（0-4 位）
- ✅ 自动保存机制（每 30 分钟）
- ✅ 财富排行榜实时更新

**配置示例：**
```yaml
economy:
  enabled: true
  starting-balance: 100.0   # 初始资金
  currency-symbol: "$"      # 货币符号
  decimal-places: 2         # 小数位数
```

---

### 5️⃣ 🏦 银行系统（Bank System）⭐ 推荐

独立于钱包的银行系统，提供更专业的资金管理体验。

**核心命令：**
```
/bank balance [玩家]      # 查看银行余额
/bank deposit <金额>      # 存款
/bank withdraw <金额>     # 取款
/bank transfer <玩家> <金额>  # 跨玩家银行转账
/bank help                # 查看帮助信息
```

**特色功能：**
- ✅ 存款/取款金额范围限制
- ✅ 转账手续费可配置
- ✅ 完善的输入验证（防止负数、非法字符）
- ✅ 余额不足检测与提示
- ✅ **数据独立存储** - 银行与钱包分离，持久化于 `bank.yml`，互不干扰
- ✅ **K10 联动** - 真实银行账户数与交易数据实时推送 K10 数字城市仪表盘
- ✅ **完整单元测试覆盖**（测试覆盖率 100%）

**测试框架：**
```java
// 测试用例包括：
✅ 初始余额测试
✅ 正常存款/取款测试
✅ 转账功能测试
✅ 无效输入处理测试
✅ 余额不足边界测试
```

**配置示例：**
```yaml
bank:
  initial-balance: 1000.0    # 新开银行账户初始余额
  min-deposit: 1.0           # 单笔最低存款金额
  max-deposit: 1000000.0     # 单笔最高存款金额
  transfer-fee: 0.0          # 银行内转账手续费（固定金额，0=免费）
```

---

### 6️⃣ 📢 公告系统（Announcement System）

多渠道公告推送，确保重要信息触达每一位玩家。

**核心命令：**
```
/ean <消息>               # 聊天框公告（全员可见）
/eanactionbar <消息>      # 动作栏公告（屏幕底部）
/eantitle <标题>|<副标题>  # 大标题公告（屏幕中央）
/eanbossbar <消息>        # Boss 栏公告（屏幕顶部）
/eanreload                # 重载公告配置
```

**特色功能：**
- ✅ 4 种展示形式（聊天/动作栏/标题/Boss 栏）
- ✅ 定时轮播功能（可设置间隔时间）
- ✅ 支持占位符（%player%、%online% 等）
- ✅ 随机顺序播放或按序播放
- ✅ 支持颜色代码（&a、&b 等）

**配置示例：**
```yaml
announcements:
  enabled: true
  interval: 60                  # 轮播间隔（秒）
  random-order: true            # 随机顺序
  
  messages:
    - "&6[公告] &e欢迎访问我们的服务器！"
    - "&a服务器状态良好，尽情享受游戏！"
  
  bossbar:
    enabled: true
    messages:
      - "&6&l服务器公告 - 欢迎所有玩家！"
```

---

### 7️⃣ 🎮 玩家管理（Player Management）

强大的玩家状态控制命令集合。

**核心命令：**
```
/efly [玩家]              # 切换飞行模式
/egod [玩家]              # 切换上帝模式（无敌）
/eheal [玩家]             # 治疗生命值 + 饱食度
/efeed [玩家]             # 仅恢复饱食度
/egm <0|1|2|3> [玩家]     # 切换游戏模式
                           # 0=生存 1=创造 2=冒险 3=旁观
```

**权限控制：**
- 普通玩家：只能对自己使用 `/efly`、`/egod`（如果开放权限）
- OP 管理员：可以对任意玩家使用所有命令

**配置示例：**
```yaml
player-commands:
  fly-enabled: true        # 是否启用飞行命令
  god-enabled: true        # 是否启用上帝模式
  heal-enabled: true       # 是否启用治疗命令
  feed-enabled: true       # 是否启用喂食命令
  gamemode-enabled: true   # 是否启用游戏模式命令
```

---

### 8️⃣ 🤖 K10 数字孪生系统（Digital Twin）⭐🔥 独家功能

**这是本插件的杀手锏功能！** 实现 Minecraft 游戏世界与真实硬件设备的双向数据交互。

#### 系统架构

```
Minecraft 服务器 ←→ K10TCPManager ←→ 行空板 K10 硬件
     ↓                              ↓
  游戏事件                      物理设备响应
（玩家加入/退出/聊天）          （LED 显示/传感器数据）
```

#### 支持的事件类型

| 事件类型 | 数据格式 | 说明 |
|---------|---------|------|
| 玩家加入 | `PLAYER:JOIN:玩家名` | 玩家登录游戏时触发 |
| 玩家退出 | `PLAYER:QUIT:玩家名` | 玩家离开游戏时触发 |
| 聊天消息 | `CHAT:玩家名:内容` | 玩家发送聊天时触发 |
| 红石信号 | `REDSTONE:强度值` | 红石信号变化时触发 |

#### 技术特性

- ✅ **HTTP/TCP 双协议支持** - 灵活选择通信方式
- ✅ **异步消息队列** - 不阻塞主线程，保证 TPS 稳定
- ✅ **消息节流机制** - 防止消息洪泛（同类消息最小间隔 500ms）
- ✅ **自动重连机制** - 连接断开后自动尝试重连（最多 3 次）
- ✅ **健康状态监控** - 实时统计成功率、响应时间
- ✅ **环境数据计算** - 扫描周围方块、生物群系等数据并发送给 K10
- ✅ **安全性增强** - 输入验证、JSON 注入防护

#### 环境数据子系统 ⭐🔥 v1.2.0 聚合报告模式

除了事件推送，还能将 Minecraft 世界数据实时同步给 K10。

**✨ v1.2.0 变更（2026-08-17）：多玩家聚合报告**

- ✅ **聚合发送**：不再逐玩家轮询（旧模式 5-30 秒/条会频繁刷掉 K10 城市大屏），改为**全玩家汇总成一条** `environment_summary` 消息，默认每 **60 秒**一条
- ✅ **K10 端配套**（固件 v3.3）：环境表格页展示全玩家列表（名字+温度+湿度，最多20行，A/B滚动）；报告到达**只刷新表格+LED提示，不打断当前页面**
- ✅ **单条容量上限**：`max-players-in-report`（默认20），防止超大服务器 JSON 爆体积
- ✅ 无人在线时不发送，K10 保持城市大屏
- ✅ 保留三层验证体系：完整性校验、数值范围验证、异常捕获；无效数据自动丢弃

**历史优化（2026-07-10）：**

1. **温度计算算法重构** 🌡️
   - ✅ **修复严重 Bug**：解决玩家在岩浆中显示异常低温的问题（原：17°C → 现：65-70°C）
   - ✅ 新增**直接接触检测**：优先检测玩家身体位置是否接触热源/冷源
   - ✅ 提高热源权重系数：岩浆距离衰减从 8.0 提升至 15.0（+87%）
   - ✅ 扩大影响值范围：原 -10~+15 → 新 -20~+30

2. **数据质量兜底机制** 🛡️
   - ✅ 三层验证：完整性校验 / 数值合理性 / 异常捕获，无效数据不发送

**温度检测示例：**

| 场景 | 修复前 | ✅ 修复后 |
|------|--------|-----------|
| 站在岩浆中 | 17°C ❌ | **65-70°C** ✅ |
| 头部在岩浆中 | 20°C ❌ | **55-60°C** ✅ |
| 站在火焰中 | 25°C ❌ | **50-55°C** ✅ |
| 站在岩浆块上 | 22°C ❌ | **45-50°C** ✅ |
| 靠近岩浆（1格） | 30°C | **40-45°C** |
| 正常环境 | 15-25°C | **15-25°C** |

**配置示例（v1.2.0）：**
```yaml
environment:
  enabled: true
  scan-radius: 10              # 扫描半径（格）
  height-range: 5              # 高度扫描范围
  report-interval: 1200        # ★ 聚合报告周期：60秒（下限10秒）
  max-players-in-report: 20    # ★ 单条报告最多玩家数
  response-timeout: 5000       # 响应超时（毫秒）
  debug-mode: true             # ★ 调试模式：开启
```

**技术实现细节：**

```java
// EnvironmentDataCalculator.java - 温度计算核心逻辑
private double calculateThermalSourcesEffect(Location loc, World world) {
    // ★ 最高优先级：检测玩家直接接触
    Block feetBlock = world.getBlockAt(cx, cy - 1, cz);
    if (feetMaterial == Material.LAVA) {
        return 45.0; // 直接接触岩浆：+45度
    }
    
    // 距离衰减计算（提高权重）
    if (type == Material.LAVA) {
        effect += 15.0 * distanceFactor; // 原8.0→15.0
    }
}

// EnvironmentDataScheduler.java - 数据验证机制
private boolean validateEnvironmentData(Map<String, Object> data) {
    // 检查必需字段、数值范围合理性
    // 无效数据返回 false，跳过发送
}
```

**应用场景：**
- 🏠 智能家居模拟 - 游戏内红石信号控制真实 LED 灯
- 📊 数据可视化 - 在 K10 屏幕显示服务器实时温度数据
- 🎮 交互式教学 - 用 Minecraft 教学 IoT 编程概念
- 🌍 数字孪生城市 - 将虚拟城市建设映射到物理模型
- 🔥 工业监控 - 实时监测虚拟环境的危险区域温度

**配置示例：**
```yaml
k10:
  enabled: true                          # 总开关
  host: "192.168.1.235"                 # K10 设备 IP 地址
  port: 80                              # HTTP 服务端口
  connection-timeout: 3000              # 连接超时（毫秒）
  max-retries: 3                        # 最大重试次数
  player-events-enabled: true            # 启用玩家事件监听
  chat-events-enabled: true             # 启用聊天事件监听
```

---

### 8️⃣5️⃣ 🏙️ **数字城市管理系统（Digital City Manager）** ⭐🔥🚀 **v3.0 全新升级**

> **革命性的 Minecraft 智慧城市解决方案！** 将你的服务器变成真正的数字孪生城市管理中心。

#### ✨ 核心亮点

这是一个**企业级城市运营中心**，不仅仅是数据显示，而是完整的城市管理生态系统：

- 🎯 **两大页面+详情子页** - 城市大屏（默认）/ 环境表格（A/B滚动+A+B查看详情）
- 📊 **实时数据采集** - 在线人数、TPS、经济、活动等 15+ 种指标
- 🎨 **智能状态系统** - 自动评估城市健康度（卓越/正常/繁忙/警告/紧急）
- 🔔 **事件推送引擎** - 玩家加入/离开/阵亡/里程碑实时通知
- 💡 **LED 视觉反馈** - 不同事件对应不同颜色闪烁效果

#### 🖥️ 城市仪表盘界面预览

```
🏙️ 数字城市中心
Minecraft智慧城市 [城市]
──────────────────────
👥 在线: 5/20   ⚡ TPS: 19.8   ⏱️ 运行: 2.5时

📊 城市状态: [████████░░] 正常

== 人口统计 ==
总入驻: 150     今日峰值: 8
总阵亡: 12      🏠 12户

== 活动统计 ==
活跃度: 高       🔴RS 1.2k

◆ 智慧城市 v1.2 ◆
按B键 环境总览
```

#### 📈 城市状态等级系统

| 状态 | 颜色 | 含义 | 触发条件 |
|------|------|------|----------|
| 🟢 **卓越** | 绿色 | 城市运行极佳 | TPS>18, 在线玩家>0 |
| 🔵 **正常** | 蓝色 | 正常运行 | 默认状态 |
| 🟡 **繁忙** | 黄色 | 高负载 | 在线人数>80%容量 |
| 🟠 **警告** | 橙色 | 需要关注 | TPS<15 或 死亡>20 |
| 🔴 **紧急** | 红色 | 严重问题 | TPS<10 或 死亡>50 |

#### 🚀 快速启动（3步）

**第一步：编译插件**
```bash
mvn clean package
# 将 target/myplugin-1.0-SNAPSHOT.jar 放入 plugins/
```

**第二步：配置 K10 连接**
```yaml
k10:
  enabled: true
  host: "192.168.1.235"
  port: 80
```

**第三步：烧录 K10 固件**
```bash
# 用 Arduino IDE 打开 mn.txt（标准缩进版设备代码）
# 注：mn.md 为未格式化的原始备份，内容与 mn.txt 一致
# 选择板子: ESP32-S3-N8R2
# 上传到 K10 设备
```

当玩家进入游戏时，K10 屏幕立即显示"🏙️ 数字城市中心"（**v1.2.0 起默认主界面**），每30秒自动刷新！

#### 🎮 操作说明（固件 v3.3）

| 当前页面 | 按键 | 功能 |
|------|------|------|
| 城市大屏 | **B键** 短按 | 进入环境表格 |
| 环境表格 | **A键/B键** 短按 | 选中行上移/下移 |
| 环境表格 | **A+B** 同按 | 确认：玩家行→详情页 / 返回行→大屏 |
| 环境详情 | **A键或B键** 短按 | 返回环境表格 |

**v1.2.1 界面逻辑（固件 v3.3）：**
- 开机默认显示**城市大屏**；WiFi 连接成功前只显示配网/连接页面（不会被业务界面顶掉）
- 环境表格为**融合页**：每玩家一行（名字+温度+湿度，最多20行），A/B 滚动选择
- 环境聚合报告（每60秒）到达时**只刷新表格数据+LED提示**，不打断当前页面
- 玩家加入/阵亡/聊天/住户变动/红石激增等事件**不占消息列表**，全屏弹出通知约 4 秒后恢复
- 原聊天模式与单玩家环境页已删除；A 键长按重置 WiFi 已取消（仅保留网页端 `/reset`）

#### 📡 数据通信协议示例

**environment_summary - 环境聚合报告（v1.2.0，默认每60秒一条，`report-interval` 可调）：**
```json
{
  "event": "environment_summary",
  "request_id": "env_1786930000_1234",
  "timestamp": 1786930000000,
  "player_count": 2,
  "players": [
    {"name": "Steve", "temperature": 24.5, "humidity": 50, "light": 14, "wind_speed": 2.1, "weather": "clear", "biome": "plains"},
    {"name": "Alex", "temperature": 18.2, "humidity": 65, "light": 9, "wind_speed": 0.5, "weather": "rain", "biome": "forest"}
  ]
}
```

**CITY_DASHBOARD - 仪表盘数据（默认每30秒发送，`dashboard-interval` 可调）：**
```json
{
  "event_type": "CITY_DASHBOARD",
  "basic_stats": {
    "online_players": 5,
    "max_players": 20,
    "tps": 19.8,
    "city_status": "NORMAL",
    "uptime_hours": 2.5
  },
  "population_stats": {
    "total_joined": 150,
    "peak_today": 8,
    "total_deaths": 12,
    "avg_session_time": 45,
    "households": 12
  },
  "economy_stats": {
    "total_transactions": 89,
    "total_volume": 45000,
    "active_bank_accounts": 5,
    "server_wealth": 128000.5
  },
  "activity_stats": {
    "messages_sent": 2340,
    "blocks_broken": 5678,
    "blocks_placed": 3456,
    "redstone_changes": 1234,
    "activity_level": "HIGH"
  }
}
```

> 💡 **户数判定规则（v1.2.0）**：一张床 + 半径6格内至少一扇门 = 一户（多床共用一门按多户计）。
> 由 HouseholdManager 每5分钟扫描在线玩家周围64格内的已加载区块（快照+异步批处理，不卡服），
> 户数变化时推送 `HOUSING_CHANGE` 事件。红石活动统计通断翻转（0↔N），激增超阈值（默认1000次/30秒）推送 `REDSTONE_SURGE` 事件。

#### 💡 创意应用场景

- 🏠 **智能家居演示** - 游戏内红石信号控制真实 LED
- 📊 **数据中心大屏** - 教室/办公室展示服务器状态
- 🎮 **电竞比赛监控** - 直播时显示性能数据
- 🌍 **智慧城市课程** - STEM 教育 IoT 编程教学
- 🏆 **社区展示** - 超酷的科技感装饰

#### 🔧 高级配置

```yaml
digital-city:
  enabled: true                          # 启用数字城市管理（需 k10.enabled）
  city-name: "Minecraft智慧城市"          # 城市名称（同步显示到 K10 屏幕）
  dashboard-interval: 600                # 仪表盘刷新间隔（ticks，600=30秒）
  statistics-interval: 6000              # 详细统计收集间隔（ticks，6000=5分钟）
  debug-mode: true                       # 调试日志
  welcome-message-enabled: true          # 玩家加入时推送欢迎事件
  block-tracking-enabled: true           # 方块操作追踪
  chat-tracking-enabled: true            # 聊天消息追踪
  redstone-tracking-enabled: true        # ★ 红石活动统计（通断翻转计数）
  redstone-surge-threshold: 1000         # ★ 红石激增事件阈值（次/30秒周期，0=关闭）
  households:                            # ★ 住户结构扫描（床+门=一户）
    enabled: true
    scan-interval: 6000                  # 扫描周期（ticks，6000=5分钟）
    scan-radius: 64                      # 玩家周围扫描半径（格）
    door-bed-radius: 6                   # 床与门最大判定距离（格）
```

#### 🛠️ 故障排除

**问题：K10 无法连接？**
1. ✅ `ping 192.168.1.235` 测试连通性
2. ✅ 检查防火墙是否放行 80 端口
3. ✅ 确认设备在同一局域网
4. ✅ 查看 `[K10数字孪生]` 日志

**问题：屏幕显示异常？**
1. 按 B 键进入环境表格测试界面切换
2. 浏览器访问 `http://<K10 IP>/reset` 重置设备
3. 重新烧录固件

#### 📊 技术规格

| 项目 | 规格 |
|------|------|
| **新增代码量** | ~1500+ 行 |
| **文件数量** | 4 个核心文件 |
| **界面模式** | 2 页面 + 1 详情子页 |
| **数据类型** | 15+ 种统计数据 |
| **事件类型** | 10+ 种城市事件 |
| **刷新频率** | 30秒（可配置） |
| **内存占用** | ~5MB |
| **CPU影响** | <1% TPS |

#### 🎯 相关文件

- [DigitalCityManager.java](src/main/java/org/ljcode/myPlugin/managers/DigitalCityManager.java) - 城市管理核心逻辑
- [DigitalCityListener.java](src/main/java/org/ljcode/myPlugin/listeners/DigitalCityListener.java) - 事件监听器
- [mn.md](mn.md) - K10 接收端固件（已升级至 v3.0）
- [DIGITAL_CITY_GUIDE.md](DIGITAL_CITY_GUIDE.md) - 完整使用指南

> **💡 提示**: 完整的数字城市使用指南请查看 [DIGITAL_CITY_GUIDE.md](DIGITAL_CITY_GUIDE.md)，包含详细的协议说明、配置选项和扩展计划！

---

### 9️⃣ 🌐 Web 配置管理系统

告别手动编辑 YAML 文件的痛苦时代！

#### 访问方式

启动插件后，打开浏览器访问：
- **本地访问**: http://localhost:8080
- **局域网访问**: http://你的 IP:8080（API 默认仅放行本机与内网网段，见下方安全设置）

#### 安全设置 ⭐ v1.1.0 新增

API 接口（`/api/*`）已启用访问控制，配置位于 `config.yml`：

```yaml
web:
  # IP 白名单（支持 * 结尾的通配前缀；未配置时默认仅允许本机）
  allowed-ips:
    - "127.0.0.1"
    - "::1"
    - "192.168.*"
    - "10.*"
  # 可选 API 令牌：设为非空值后，请求必须携带 ?token=xxx 或 X-API-Token 请求头
  api-token: ""
```

#### 功能特性

- ✅ **可视化编辑** - 表单式配置修改，所见即所得
- ✅ **分类清晰** - 按功能模块分组显示
- ✅ **实时生效** - 修改后立即应用到服务器（部分需重载）
- ✅ **多端适配** - 支持 PC、平板、手机浏览器
- ✅ **安全防护** - IP 白名单 + 可选 Token 认证，默认仅本机访问

#### 技术实现

```java
// 基于 NanoHTTPD 构建
WebServer extends NanoHTTPD {
    // 提供 RESTful API 接口
    // GET  /api/config     - 获取当前配置
    // POST /api/config     - 更新配置项
    // GET  /*              - 返回静态 HTML 页面
}
```

**配置示例：**
```yaml
web-server:
  port: 8080    # Web 服务器端口（可自定义）
```

---

### 🔟 🛒 GUI 商店系统（Shop System）

图形化商店界面，提升交易体验。

**核心命令：**
```
/shop                   # 打开自助商店 GUI 界面
```

**特色功能：**
- ✅ 直观的物品图标展示
- ✅ 点击购买/出售操作
- ✅ 实时库存更新
- ✅ 权限控制（可限制特定商品）
- ✅ 交易历史记录

**相关文件：**
- [ShopManager.java](src/main/java/org/ljcode/myPlugin/managers/ShopManager.java) - 商店逻辑
- [ShopGUI.java](src/main/java/org/ljcode/myPlugin/gui/ShopGUI.java) - GUI 界面
- [ShopListener.java](src/main/java/org/ljcode/myPlugin/listeners/ShopListener.java) - 点击事件处理

---

### 1️⃣1️⃣ 💬 高级聊天系统（Advanced Chat）

不仅仅是聊天，更是社交体验的升级。

#### 子模块列表

##### 📝 聊天格式化（ChatFormatListener）
```yaml
chat:
  format: "&8[&e%time%&8] %player_prefix%%level_color%[Lv.%level%]&r %player%&7: &r%message%"
```

**支持的占位符：**
- `%player%` - 玩家名
- `%displayname%` - 显示名称
- `%message%` - 消息内容
- `%time%` - 当前时间
- `%world%` - 所在世界
- `%level%` - 玩家等级
- `%online%` - 在线人数
- `%ping%` - 延迟值
- ... 更多占位符详见配置文件

##### 😊 表情转换（ChatHintListener）
自动将文本表情转换为 Unicode 符号：

```
输入: "今天天气真好 :smile:"
输出: "今天天气真好 😊"
```

**内置表情库：**
`:smile:` → 😊 | `:laugh:` → 😂 | `:cool:` → 😎 | `:heart:` ❤️ | `:fire:` 🔥

##### 🚫 脏话过滤（ProfanityFilterListener）
自动检测并处理不当言论：

```yaml
profanity-filter:
  enabled: true
  violation-threshold: 3     # 违规 3 次后踢出
  warning-message: "&c检测到不当言论，请注意文明用语！"
  kick-message: "&c由于多次违规，您已被踢出服务器！"
  
  profanity-list:
    chinese: ["草", "操", "妈", "逼", "滚"]  # 中文敏感词
    english: ["fuck", "shit", "damn"]        # 英文敏感词
```

##### 💡 智能提示（ChatHintListener）
- 命令建议补全
- 智能回复提示
- 聊天活跃度统计

---

### 1️⃣2️⃣ 🌟 悬浮文字系统（Hologram System）

在空中创建炫酷的文字展示效果。

**核心命令：**
```
/holo create <内容>              # 创建单行悬浮文字
/holo multiline <行数> <内容>     # 创建多行悬浮文字
/holo online <内容>               # 动态显示在线人数
/holo follow <内容>               # 跟随玩家的悬浮文字
/holo stop                       # 停止跟随效果
/holo clear                      # 清除自己的悬浮文字
/holo clearall                   # 清除所有悬浮文字（OP）
/holo list                       # 列出所有悬浮文字
/holo stats                      # 显示统计信息
```

**应用场景：**
- 🏪 商店招牌
- 📜 服务器规则展示
- 👑 玩家欢迎信息
- 🏆 排行榜展示

---

### 1️⃣3️⃣ 🎛️ 超级控制中心菜单（Control Center Menu）

一个 GUI 集成所有常用管理功能。

**使用方式：**
```
/menu                   # 打开超级控制中心
```

**集成功能：**
- ✅ 快速传送到各个地点
- ✅ 一键切换游戏模式
- ✅ 玩家管理快捷入口
- ✅ 服务器状态监控
- ✅ 常用工具集合

**技术实现：**
- 基于 Bukkit Inventory API
- 自定义点击事件处理（MenuListener）
- 支持动态刷新内容

---

### 1️⃣4️⃣ ⚔️ 特殊武器与趣味功能

为服务器增添更多乐趣的特殊功能。

#### 🔥 火焰刀（Flame Blade）
```
/eflameblade            # 获取一把全附魔火焰剑
```
**特效：**
- 攻击时生成岩浆块（持续 3 秒）
- 额外 100 点伤害
- 全属性附魔（锋利、击退、火焰附加等）

#### 💥 核弹法阵（Nuke）
```
/enuke                  # 在脚下创建核弹爆炸法阵
```
**效果：**
- 大范围方块破坏
- 震屏视觉效果
- 适合清场或娱乐用途

#### 🛡️ 满附魔装备
```
/eall66                # 获取满附魔下界合金盔甲套装（OP 专用）
/eall22                # 获取满附魔下界合金工具套装（OP 专用）
```

#### 📊 调试信息
```
/edebug                 # 显示简化版调试信息（F3 替代方案）
/e12503                 # 设置攻击力为 9999999999999999（测试用）
```

#### ✨ 粒子拖尾效果
玩家移动时自动生成火焰粒子轨迹，增强视觉体验。

---

## 🎨 登录/退出消息系统（JoinMessage Plus）

当玩家加入或离开服务器时，提供沉浸式的欢迎/送别体验。

### 功能特性

#### 🎬 大屏幕标题动画
```yaml
join-message:
  title:
    enabled: true
    text: "&6&l欢迎回来!"
    subtitle: "&e%player% &7- 享受游戏时光!"
    fade-in: 10        # 淡入时间（tick）
    stay: 40           # 停留时间（tick）
    fade-out: 10       # 淡出时间（tick）
```

#### 🎆 烟花特效
```yaml
fireworks:
  enabled: true
  amount: 3                        # 烟花数量
  preferred-colors:                # 颜色偏好
    - RED
    - GREEN
    - BLUE
```

#### 💬 随机欢迎消息
支持 10+ 种随机欢迎模板，包含丰富变量：

```
🎉 热烈欢迎 %player% 加入服务器! 当前在线: %online%/%max_players% 玩家
🌟 新冒险家 %player% 加入了我们的世界! 服务器时间: %time%
🚀 欢迎 %player% 来到 %server_name%! 祝您游戏愉快!
... 更多模板见配置文件
```

**可用变量：**
`%player%` | `%online%` | `%max_players%` | `%time%` | `%world%` | `%x%,%y%,%z%` | `%uuid_short%` | `%ping%` | `%gamemode%`

---

## 🏗️ 项目架构与技术细节

### 目录结构

```
src/main/java/org/ljcode/myPlugin/
├── MyPlugin.java                 # 插件主类（生命周期管理）
│
├── commands/                     # 命令处理器（20 个命令类）
│   ├── TeleportCommand.java      # 传送命令
│   ├── EconomyCommand.java       # 经济命令
│   ├── BankCommand.java          # 银行命令
│   ├── AnnouncementCommand.java  # 公告命令
│   ├── PlayerCommand.java        # 玩家管理命令
│   └── ...                       # 其他命令
│
├── managers/                     # 核心业务逻辑管理器（10 个）
│   ├── TeleportManager.java      # 传送逻辑
│   ├── EconomyManager.java       # 经济逻辑
│   ├── BankManager.java          # 银行逻辑
│   ├── HomeManager.java          # 家园逻辑
│   ├── K10TCPManager.java        # K10 通信管理
│   ├── ShopManager.java          # 商店逻辑
│   └── ...                       # 其他管理器
│
├── listeners/                    # 事件监听器（15 个）
│   ├── PlayerListener.java       # 玩家事件
│   ├── ChatFormatListener.java   # 聊天格式化
│   ├── JoinMessageListener.java  # 加入消息
│   ├── WeaponEffectListener.java # 武器特效
│   ├── ProfanityFilterListener.java  # 脏话过滤
│   └── ...                       # 其他监听器
│
├── gui/                          # 图形用户界面
│   └── ShopGUI.java              # 商店 GUI
│
├── web/                          # Web 服务
│   └── WebServer.java            # HTTP 服务器
│
├── utils/                        # 工具类
│   └── BankUtils.java            # 银行工具
│
└── tests/                        # 单元测试
    └── BankSystemTest.java       # 银行系统测试
```

### 技术栈详情

| 技术 | 版本 | 用途 |
|------|------|------|
| **Spigot API** | 1.21-R0.1-SNAPSHOT | Minecraft 插件核心 API |
| **PlaceholderAPI** | 2.11.6 | 占位符变量支持 |
| **NanoHTTPD** | 2.3.1 | 轻量级 Web 服务器 |
| **Gson** | 2.10.1 | JSON 数据解析 |
| **Java** | 21 | 开发语言（LTS 版本）|
| **Maven** | 3.6+ | 项目构建与依赖管理 |

### 设计模式

- **单例模式** - MyPlugin 全局实例
- **观察者模式** - Event Listener 机制
- **工厂模式** - Manager 类的统一创建
- **策略模式** - 不同公告类型的处理
- **命令模式** - Command Executor 解耦

---

## 🛠️ 开发指南

### 环境搭建

```bash
# 1. 克隆项目
git clone <your-repo-url>
cd my-plugin

# 2. 使用 IntelliJ IDEA 打开项目
#    File → Open → 选择 pom.xml → Open as Project

# 3. 等待 Maven 自动下载依赖（首次约 5-10 分钟）

# 4. 配置运行环境
#    Run → Edit Configurations → Spigot Server
#    设置工作目录为你的测试服务器路径
```

### 构建命令

```bash
# 清理并打包（推荐）
mvn clean package

# 生成包含所有依赖的 fat-jar（用于分发）
mvn clean compile assembly:single

# 运行测试
mvn test

# 跳过测试快速打包
mvn clean package -DskipTests
```

### 添加新功能的步骤

1. **创建命令类** - 继承 `CommandExecutor`
```java
public class MyNewCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // 你的逻辑
        return true;
    }
}
```

2. **注册命令** - 在 `MyPlugin.registerCommands()` 中添加
```java
getCommand("mynewcommand").setExecutor(new MyNewCommand(this));
```

3. **添加权限** - 在 `plugin.yml` 中声明
```yaml
permissions:
  myplugin.mynewcommand:
    description: 我的命令权限
    default: op
```

4. **编写测试** - 在 `tests/` 目录下创建单元测试

---

## 📖 命令速查表

### 传送类
| 命令 | 说明 | 权限 |
|------|------|------|
| `/etp <玩家>` | 传送到玩家 | essentialsx.tp |
| `/etpa <玩家>` | 请求传送 | essentialsx.tp |
| `/etpahere <玩家>` | 召唤玩家 | essentialsx.tp |
| `/etpaccept` | 接受请求 | essentialsx.tp |
| `/etpdeny` | 拒绝请求 | essentialsx.tp |
| `/ehome [名]` | 回家 | essentialsx.home |
| `/esethome [名]` | 设家 | essentialsx.home.set |
| `/ewarp <名>` | 使用传送点 | essentialsx.warp |
| `/esetwarp <名>` | 设传送点 | essentialsx.warp.set |
| `/espawn` | 回出生点 | - |
| `/eback` | 返回上位置 | - |

### 经济类
| 命令 | 说明 | 权限 |
|------|------|------|
| `/emoney [玩家]` | 查余额 | essentialsx.money |
| `/epay <玩家> <金额>` | 转账 | essentialsx.money.pay |
| `/ebalancetop` | 排行榜 | essentialsx.money |
| `/eco set <玩家> <金额>` | 设余额 | essentialsx.admin |
| `/bank balance` | 银行余额 | - |
| `/bank deposit <金额>` | 存款 | - |
| `/bank withdraw <金额>` | 取款 | - |
| `/bank transfer <玩家> <金额>` | 转账 | - |

### 玩家管理类
| 命令 | 说明 | 权限 |
|------|------|------|
| `/efly [玩家]` | 飞行模式 | essentialsx.fly |
| `/egod [玩家]` | 上帝模式 | essentialsx.god |
| `/eheal [玩家]` | 治疗 | essentialsx.heal |
| `/efeed [玩家]` | 喂食 | - |
| `/egm <模式> [玩家]` | 游戏模式 | essentialsx.gm |

### 公告类
| 命令 | 说明 | 权限 |
|------|------|------|
| `/ean <消息>` | 聊天公告 | chatannouncements.send.chat |
| `/eanactionbar <消息>` | 动作栏公告 | chatannouncements.send.actionbar |
| `/eantitle <标题>\|<副标题>` | 标题公告 | chatannouncements.send.title |
| `/eanbossbar <消息>` | Boss 栏公告 | chatannouncements.send.bossbar |
| `/eanreload` | 重载配置 | chatannouncements.reload |

### 特色功能类
| 命令 | 说明 | 权限 |
|------|------|------|
| `/shop` | 打开商店 | - |
| `/menu` | 控制中心 | - |
| `/holo <子命令>` | 悬浮文字 | - |
| `/eflameblade` | 火焰刀 | - |
| `/enuke` | 核弹法阵 | - |
| `/eall66` | 满附魔盔甲 | OP |
| `/eall22` | 满附魔工具 | OP |
| `/edebug` | 调试信息 | - |
| `/ehelp` | 帮助信息 | - |

---

## ⚙️ 配置文件完整参考

### 主配置文件 config.yml（精简版）

查看完整配置请参考：[config.yml](src/main/resources/config.yml)

**主要配置区块：**

1. **K10 数字孪生设置** - 硬件联动配置
2. **环境数据系统** - 数据采集参数
3. **经济系统** - 货币相关设置
4. **银行系统** - 金融业务参数
5. **家园/传送点** - 位置管理配置
6. **聊天系统** - 格式化、过滤、提示
7. **登录消息** - 欢迎特效配置
8. **特殊物品** - 武器属性调整
9. **公告系统** - 轮播内容和频率
10. **Web 服务器** - 端口和访问控制
11. **系统设置** - 调试模式和自动保存

---

## ❓ 常见问题（FAQ）

### Q1: 插件无法启动？
**A:** 检查以下几点：
- Java 版本是否 ≥ 21（`java -version`）
- 服务器版本是否 ≥ 1.21
- 是否安装了 PlaceholderAPI（可选但推荐）
- 查看 `logs/latest.log` 中的错误信息

### Q2: K10 连接失败？
**A:**
1. 确认 K10 设备 IP 地址正确（`ping 192.168.1.235`）
2. 检查防火墙是否阻止了 80 端口
3. 确认 K10 设备上的 HTTP 服务已开启
4. 查看 `[K10数字孪生]` 开头的日志输出

### Q3: 如何备份经济数据？
**A:** 钱包数据存储在 `plugins/MyPlugin/economy.yml`，银行数据独立存储在 `plugins/MyPlugin/bank.yml`，定期备份这两个文件即可。
插件也会每 5 分钟自动保存一次。

### Q4: Web 配置页面打不开？
**A:**
1. 确认端口 8080 未被占用（可在 `config.yml` 的 `web-server.port` 修改）
2. 尝试访问 `http://localhost:8080`
3. 如果是远程服务器，确认防火墙放行了 8080 端口
4. API 默认仅放行白名单 IP（本机 + `192.168.*` / `10.*` 内网段），跨网段访问需在 `web.allowed-ips` 中添加你的 IP
5. 查看控制台是否有 "Web配置服务器已在端口 XXXX 启动" 的提示

### Q5: 如何添加自定义表情？
**A:** 编辑 `config.yml` 中的 `chat.hints.custom-emojis` 部分：
```yaml
custom-emojis:
  ":myemoji": "😎"
```

---

## 📄 许可证与版权

本项目基于 **MIT 许可证** 开源。

**依赖库许可证：**
- Spigot API - LGPL-3.0
- PlaceholderAPI - MIT
- NanoHTTPD - BSD 2-Clause
- Gson - Apache 2.0

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 贡献流程

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 代码规范

- 遵循 Google Java Style Guide
- 所有 public 方法必须添加 Javadoc 注释
- 新功能必须编写对应的单元测试
- 保持 100% 的测试覆盖率（至少针对核心逻辑）

---

## 📊 项目统计

- **总代码行数**: ~15,000+ 行
- **命令数量**: 20+ 个自定义命令
- **监听器数量**: 15+ 个事件监听器
- **管理器数量**: 10+ 个业务管理器
- **配置项数量**: 200+ 个可配置参数
- **测试用例**: 6 个银行系统测试
- **支持的事件类型**: 10+ 种 Minecraft 事件

---

## 📚 文档导航

| 文档 | 说明 | 链接 |
|------|------|------|
| 📋 **变更日志** | 版本更新历史和功能变更记录 | [CHANGELOG.md](./CHANGELOG.md) |
| 🤝 **贡献指南** | 如何参与项目开发和提交代码 | [CONTRIBUTING.md](./CONTRIBUTING.md) |
| 📜 **行为准则** | 社区行为规范和期望 | [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md) |
| 🔒 **安全政策** | 安全漏洞报告流程和最佳实践 | [SECURITY.md](./SECURITY.md) |
| 📄 **开源许可** | MIT 许可证全文 | [LICENSE](./LICENSE) |

### 快速链接

- 🔗 **GitHub 仓库**: https://github.com/sunday-lil/my-plugin
- 🐛 **问题反馈**: [Issues](https://github.com/sunday-lil/my-plugin/issues)
- 💬 **功能讨论**: [Discussions](https://github.com/sunday-lil/my-plugin/discussions)
- 📦 **版本发布**: [Releases](https://github.com/sunday-lil/my-plugin/releases)
- 🔍 **安全公告**: [Security Advisories](https://github.com/sunday-lil/my-plugin/security/advisories)

---

## 🤝 贡献指南

我们欢迎所有形式的贡献！无论是 Bug 修复、新功能、文档改进还是测试用例。

查看完整的贡献指南：[📖 CONTRIBUTING.md](./CONTRIBUTING.md)

### 快速开始

```bash
# 1. Fork 并克隆仓库
git clone https://github.com/你的用户名/my-plugin.git

# 2. 创建功能分支
git checkout -b feature/你的新功能

# 3. 开发并测试
mvn clean package && mvn test

# 4. 提交并推送
git add . && git commit -m "feat: 添加新功能" && git push origin feature/你的新功能

# 5. 创建 Pull Request
```

### 代码规范

- ✅ 使用 4 空格缩进
- ✅ 遵循 Google Java Style Guide
- ✅ 所有 public 方法添加 Javadoc 注释
- ✅ 新功能必须编写单元测试
- ✅ Commit Message 遵循 Conventional Commits 规范

---

## 📄 许可证

本项目基于 **MIT License** 开源协议发布。

查看完整许可证文本：[📜 LICENSE](./LICENSE)

### 核心条款

✅ **允许事项：**
- 商业使用
- 修改和分发
- 私人使用
- 再授权（需包含版权声明）

❌ **免责声明：**
- 本软件按"原样"提供，不提供任何形式的明示或暗示的保证
- 作者不对任何索赔或损害负责

---

## 📞 联系方式与支持

- **问题反馈**: [GitHub Issues](https://github.com/sunday-lil/my-plugin/issues)
- **功能建议**: [GitHub Discussions](https://github.com/sunday-lil/my-plugin/discussions)
- **安全漏洞**: [SECURITY.md](./SECURITY.md)
- **官方邮箱**: sunday-lil@users.noreply.github.com

---

## 🙏 致谢

感谢以下开源项目和社区的支持：

- **SpigotMC** - 提供优秀的 Minecraft 服务端 API
- **PlaceholderAPI** - 强大的占位符解决方案
- **NanoHTTPD** - 轻量级嵌入式 HTTP 服务器
- **Gson** (Google) - 高性能 JSON 处理库
- **Maven Apache** - 项目构建与依赖管理工具
- **IntelliJ IDEA** - 强大的 Java IDE
- **所有贡献者** - 让这个项目变得更好

---

<p align="center">
  <strong>Made with ❤️ by sunday-lil | Powered by Java 21 & Spigot API 1.21</strong><br>
  <em>如果这个项目对你有帮助，请给一个 ⭐ Star 支持一下！</em>
</p>

<p align="center">
  <a href="https://github.com/sunday-lil/my-plugin"><img src="https://img.shields.io/github/stars/sunday-lil/my-plugin?style=social" alt="GitHub Stars"></a>
  <a href="https://github.com/sunday-lil/my-plugin/fork"><img src="https://img.shields.io/github/forks/sunday-lil/my-plugin?style=social" alt="GitHub Forks"></a>
  <a href="https://github.com/sunday-lil/my-plugin/watchers"><img src="https://img.shields.io/github/watchers/sunday-lil/my-plugin?style=social" alt="GitHub Watchers"></a>
</p>
