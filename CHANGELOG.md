# 📝 变更日志 (Changelog)

本文件记录了 EssentialsX-Clone 项目的重要变更。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

---

## [1.2.0] - 2026-08-17

### ✨ 新增 (Added)

#### K10 设备端 (固件 v3.2)
- 🖥️ **默认界面改为城市大屏** - 开机即显示仪表盘；任何界面30秒无操作自动返回大屏（原为聊天模式）
- 🌍 **多玩家环境列表** - 新增 `environment_summary` 聚合报告处理：显示全玩家"名字+温度+湿度"列表（最多8行+剩余计数），约8秒后自动返回城市大屏
- 📢 **全屏临时通知** - 玩家加入/阵亡/住户变动/红石激增等事件在任何界面下全屏弹出约5秒
- 🏠 **住户数上屏** - 仪表盘人口统计行显示 `🏠 N户`（替代原"均时长"）
- 🔴 **红石活动上屏** - 活动统计行显示 `🔴RS N`（缩写格式 1.2k/3.4M）
- 🆕 **新事件类型** - `HOUSING_CHANGE`（住户变动）/ `REDSTONE_SURGE`（红石激增）滚动通知 + LED
- 📄 状态页新增住户数、红石活动数据行

#### 插件端
- 🔴 **红石活动统计** - 新增 `RedstoneListener`：统计红石通断翻转（0↔N，忽略中间信号变化），随仪表盘推送 `activity_stats.redstone_changes/redstone_total`；激增超阈值（默认1000次/30秒）推送 `REDSTONE_SURGE` 事件（5分钟冷却）
- 🏠 **住户结构扫描** - 新增 `HouseholdManager`：判定规则"一张床 + 半径6格内至少一扇门 = 一户"；每5分钟扫描在线玩家周围64格已加载区块，ChunkSnapshot + 异步批处理（每批8区块）不卡服；户数变化推送 `HOUSING_CHANGE` 事件；结果推送 `population_stats.households`

### 🔨 变更 (Changed)

- 🌍 **环境数据改为聚合报告模式** - `EnvironmentDataScheduler` 重写：废弃逐玩家轮询与动态间隔（base/min/max-interval 移除），改为固定周期（`report-interval`，默认60秒，下限10秒）将全部在线玩家汇总为一条 `environment_summary` 消息（上限 `max-players-in-report`=20）；无人在线不发送
  - 动机：旧模式5-30秒一条导致 K10 频繁切屏，城市大屏被环境页刷掉
- 🏦 K10 仪表盘的银行账户数显示让位给住户数（移至设备状态页）
- 📦 plugin.yml 版本号 1.0.0 → 1.2.0（与 CHANGELOG 对齐）

### 🔗 配套要求
- 插件 v1.2.0 需配合 K10 固件 v3.2（mn.txt）烧录；旧固件可正常通信但不显示聚合列表/住户/红石

---

## [1.1.0] - 2026-08-17

### 🔧 修复 (Fixed)

#### K10 数字孪生系统
- 🐛 **K10TCPManager** - 修复非 200 HTTP 响应导致无限重试（现在计入重试上限后退出）
- 🐛 **K10TCPManager** - 修复健康状态误判（改为基于最后成功时间超 30 秒阈值判定）
- 🐛 **K10TCPManager** - 手动 JSON 解析替换为 Gson（正确处理值内逗号与嵌套结构）
- 🐛 **EnvironmentDataScheduler** - Bukkit API 调用从异步任务全部改为同步任务，彻底解决线程安全问题（含间隔调整时任务重建的遗漏点）

#### 数字城市管理系统
- 🐛 **DigitalCityManager** - 仪表盘/统计间隔与城市名称改为读取配置（消除硬编码）
- 🐛 **DigitalCityManager** - 银行账户数改用 BankManager 真实活跃账户数（原误用在线玩家数）

#### Web 配置系统
- 🐛 **WebServer** - 保存配置后同步 K10 配置引用（reload 立即生效，无需重启）

#### 配置与权限
- 🐛 起始资金 / 家园上限 / 传送请求超时改为读取配置（消除配置死键）
- 🐛 **plugin.yml** - 移除重复的 `essentialsx.home` 权限，补充 `essentialsx.money.baltop` 与 `bank.use`

#### 菜单系统
- 🐛 移除超级控制中心菜单中 12 个无功能的死按钮及对应处理逻辑

### ✨ 新增 (Added)

#### 安全加固
- 🔒 **WebServer** - `/api/*` 接口新增 IP 白名单 + 可选 Token 认证（默认仅本机访问）

#### K10 设备联动
- 🏦 **K10 固件** - 新增 `economy_stats` 解析：银行账户数、总交易、总流水、服务器财富
- 🏦 **K10 固件** - 城市仪表盘活动统计行新增银行账户数显示
- 🌐 **K10 固件** - 设备状态页新增银行账户与总交易数据
- 📄 **mn.txt** - K10 设备代码标准缩进版（由 mn.md 格式化，剔除空白后字符流 100% 一致；mn.md 保留为原始备份）

### 🔨 结构优化 (Changed)
- 🗑️ `target/` 构建产物移出 git 跟踪（由 .gitignore 忽略）
- 🗑️ 删除冗余 `web/index.html`（WebServer 内嵌资源已覆盖）
- 📁 `BankSystemTest` 迁移至 `src/test/java` 标准测试目录

---

## [1.0.0] - 2026-07-10

### 🎉 新增 (Added)

#### 核心系统
- ✨ 初始版本发布
- ✨ 基于 Spigot API 1.21 的完整 EssentialsX 克隆实现
- ✨ 模块化架构设计，支持独立启用/禁用各功能模块

#### 传送系统 (Teleport System)
- ✨ 玩家间传送 (`/etp`)
- ✨ 传送请求机制 (`/etpa`, `/etpahere`)
- ✨ 传送接受/拒绝 (`/etpaccept`, `/etpdeny`)
- ✨ 位置回溯 (`/eback`)

#### 家园系统 (Home System)
- ✨ 设置家园 (`/esethome`)
- ✨ 传送到家园 (`/ehome`)
- ✨ 多家园支持（普通玩家 5 个，VIP 10 个）
- ✨ 自定义家园名称

#### 传送点系统 (Warp System)
- ✨ 设置公共传送点 (`/esetwarp`)
- ✨ 使用传送点 (`/ewarp`)
- ✨ 出生点管理 (`/espawn`, `/esetspawn`)

#### 经济系统 (Economy System)
- ✨ 内置轻量级经济系统
- ✨ 余额查询 (`/emoney`)
- ✨ 玩家转账 (`/epay`)
- ✨ 财富排行榜 (`/ebalancetop`)
- ✨ 管理员经济命令 (`/eco`)

#### 银行系统 (Bank System) ⭐
- ✨ 银行余额查询 (`/bank balance`)
- ✨ 存款功能 (`/bank deposit`)
- ✨ 取款功能 (`/bank withdraw`)
- ✨ 跨玩家银行转账 (`/bank transfer`)
- ✨ 存取款金额限制
- ✨ 可配置的转账手续费
- ✨ **100% 单元测试覆盖**

#### 公告系统 (Announcement System)
- ✨ 聊天框公告 (`/ean`)
- ✨ 动作栏公告 (`/eanactionbar`)
- ✨ 标题公告 (`/eantitle`)
- ✨ Boss 栏公告 (`/eanbossbar`)
- ✨ 定时轮播功能
- ✨ 占位符支持 (`%player%`, `%online%` 等)
- ✨ 配置重载 (`/eanreload`)

#### 玩家管理系统 (Player Management)
- ✨ 飞行模式 (`/efly`)
- ✨ 上帝模式 (`/egod`)
- ✨ 治疗 (`/eheal`)
- ✨ 喂食 (`/efeed`)
- ✨ 游戏模式切换 (`/egm`)

#### K10 数字孪生系统 (Digital Twin) 🔥
- ✨ 与行空板 K10 硬件的实时数据交互
- ✨ HTTP/TCP 双协议支持
- ✨ 玩家事件监听（加入/退出/聊天）
- ✨ 异步消息队列处理
- ✨ 消息节流机制
- ✅ 自动重连机制（最多 3 次）
- ✅ 健康状态监控统计
- ✅ 环境数据计算与推送
- ✅ 输入验证与 JSON 注入防护

#### Web 管理系统
- ✨ 基于 NanoHTTPD 的轻量级 Web 服务器
- ✨ 可视化配置界面（浏览器访问 http://localhost:8080）
- ✅ RESTful API 接口
- ✅ 分类清晰的配置面板
- ✅ 多端适配（PC/平板/手机）

#### GUI 商店系统
- ✨ 图形化商店界面 (`/shop`)
- ✅ 物品图标展示
- ✅ 点击购买/出售操作
- ✅ 实时库存更新

#### 高级聊天系统
- ✨ 自定义聊天格式（支持丰富占位符）
- ✨ 文字表情转换（`:smile:` → 😊）
- ✨ 脏话过滤系统（中英文敏感词库）
- ✨ 违规次数累计与自动踢出
- ✨ 智能提示与命令补全

#### 悬浮文字系统 (Hologram)
- ✨ 创建单行悬浮文字 (`/holo create`)
- ✨ 多行悬浮文字 (`/holo multiline`)
- ✨ 动态在线人数显示 (`/holo online`)
- ✨ 跟随玩家的悬浮文字 (`/holo follow`)
- ✨ 悬浮文字管理与清除

#### 控制中心菜单
- ✨ GUI 集成所有常用管理功能 (`/menu`)
- ✅ 快速传送入口
- ✅ 游戏模式切换
- ✅ 玩家管理快捷方式
- ✅ 服务器状态监控

#### 特殊武器与趣味功能
- ✨ 火焰刀 (`/eflameblade`) - 全附魔火焰剑
- ✨ 核弹法阵 (`/enuke`) - 大范围破坏特效
- ✨ 满附魔装备 (`/eall66`, `/eall22`)
- ✨ 调试信息显示 (`/edebug`, `/e12503`)
- ✨ 粒子拖尾效果

#### 登录/退出消息增强
- ✨ 大屏幕标题动画（淡入/停留/淡出）
- ✨ 烟花欢迎特效
- ✨ 10+ 种随机欢迎消息模板
- ✨ 丰富的变量支持

### 🎨 设计与架构
- ✨ 模块化设计，14 大功能模块独立运行
- ✨ 观察者模式的事件监听机制
- ✨ 工厂模式的 Manager 统一创建
- ✨ 命令模式的 Command 解耦
- ✨ 异步处理保障服务器性能
- ✨ 完善的配置文件系统（YAML）

### 📚 文档
- ✨ 详细的 README.md（中文）
- ✨ 完整的功能说明与配置示例
- ✨ 项目架构图与技术栈说明
- ✨ 安装与开发指南
- ✨ API 文档框架

### 🧪 测试
- ✨ 银行系统完整单元测试（100% 覆盖率）
- ✨ 边界条件测试
- ✅ 异常输入测试

### 🔧 技术特性
- ✨ Java 21 LTS 支持
- ✨ Maven Shade 插件打包依赖
- ✨ PlaceholderAPI 集成
- ✨ Gson JSON 处理
- ✨ 完整的错误处理与日志记录

---

## [Unreleased]

### 🔄 计划中 (Planned)
- [ ] 多语言支持（i18n）
- [ ] 数据库存储支持（MySQL/SQLite）
- [ ] 更多 GUI 界面优化
- [ ] 权限系统完善
- [ ] 性能监控仪表板
- [ ] 插件自动更新检测
- [ ] Discord/Telegram 通知集成
- [ ] 备份与恢复系统

---

## 版本说明

### 版本号规则
- **主版本号 (MAJOR)**：不兼容的 API 修改
- **次版本号 (MINOR)**：向下兼容的功能性新增
- **修订号 (PATCH)**：向下兼容的问题修正

### 发布周期
- **稳定版**：功能完整且经过充分测试
- **预发布版 (RC)**：候选版本，接近正式发布
- **开发版 (SNAPSHOT)**：最新开发进度，可能不稳定

---

**完整版本历史查看：** [GitHub Releases](https://github.com/sunday-lil/my-plugin/releases)

**当前版本**: v1.0.0 (2026-07-10)