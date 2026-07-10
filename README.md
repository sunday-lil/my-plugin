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
  enabled: true
  initial-balance: 1000.0    # 初始银行余额
  min-deposit: 1.0            # 最小存款额
  max-deposit: 1000000.0      # 最大存款额
  transfer-fee: 0.0           # 转账手续费（0=免费）
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

#### 环境数据子系统 ⭐🔥 v2.0 重构升级

除了事件推送，还能将 Minecraft 世界数据实时同步给 K10。

**✨ 最新优化（2026-07-10）：**

1. **温度计算算法重构** 🌡️
   - ✅ **修复严重 Bug**：解决玩家在岩浆中显示异常低温的问题（原：17°C → 现：65-70°C）
   - ✅ 新增**直接接触检测**：优先检测玩家身体位置是否接触热源/冷源
   - ✅ 提高热源权重系数：岩浆距离衰减从 8.0 提升至 15.0（+87%）
   - ✅ 扩大影响值范围：原 -10~+15 → 新 -20~+30

2. **数据质量兜底机制** 🛡️
   - ✅ **三层验证体系**：
     - 数据完整性校验（必需字段检查）
     - 数值合理性验证（温度/湿度/光照范围检查）
     - 异常捕获机制（try-catch 包裹计算流程）
   - ✅ 无效数据自动丢弃，不发送至 K10
   - ✅ 计算耗时记录与调试日志输出

3. **发送间隔优化** ⏱️
   - ✅ 基础间隔延长：5秒 → **10秒**（确保计算充分完成）
   - ✅ 最小间隔翻倍：2.5秒 → **5秒**（防止过载）
   - ✅ 最大间隔调整：25秒 → **30秒**（极端情况兜底）
   - ✅ 调试模式默认开启，便于监控数据质量

**温度检测示例：**

| 场景 | 修复前 | ✅ 修复后 |
|------|--------|-----------|
| 站在岩浆中 | 17°C ❌ | **65-70°C** ✅ |
| 头部在岩浆中 | 20°C ❌ | **55-60°C** ✅ |
| 站在火焰中 | 25°C ❌ | **50-55°C** ✅ |
| 站在岩浆块上 | 22°C ❌ | **45-50°C** ✅ |
| 靠近岩浆（1格） | 30°C | **40-45°C** |
| 正常环境 | 15-25°C | **15-25°C** |

**配置示例：**
```yaml
environment:
  enabled: true
  scan-radius: 10              # 扫描半径（格）
  height-range: 5              # 高度扫描范围
  base-interval: 200           # ★ 基础间隔：10秒（原5秒）
  min-interval: 100            # ★ 最小间隔：5秒（原2.5秒）
  max-interval: 600            # ★ 最大间隔：30秒（原25秒）
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

### 9️⃣ 🌐 Web 配置管理系统

告别手动编辑 YAML 文件的痛苦时代！

#### 访问方式

启动插件后，打开浏览器访问：
- **本地访问**: http://localhost:8080
- **局域网访问**: http://你的 IP:8080

#### 功能特性

- ✅ **可视化编辑** - 表单式配置修改，所见即所得
- ✅ **分类清晰** - 按功能模块分组显示
- ✅ **实时生效** - 修改后立即应用到服务器（部分需重载）
- ✅ **多端适配** - 支持 PC、平板、手机浏览器
- ✅ **安全防护** - 基于 NanoHTTPD 的轻量级 Web 服务器

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
**A:** 经济数据存储在 `plugins/MyPlugin/economy.yml`，定期备份此文件即可。
插件也会每 30 分钟自动保存一次。

### Q4: Web 配置页面打不开？
**A:**
1. 确认端口 8080 未被占用
2. 尝试访问 `http://localhost:8080`
3. 如果是远程服务器，确认防火墙放行了 8080 端口
4. 查看控制台是否有 "Web配置服务器已在端口 XXXX 启动" 的提示

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
