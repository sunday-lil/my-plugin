# 📝 变更日志 (Changelog)

本文件记录了 EssentialsX-Clone 项目的重要变更。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

---

## [1.3.0] - 2026-10-25

### 🔧 修复 (Fixed)

#### 启动与数据
- 🐛 **全新数据目录首次启动失败** - `WarpManager`/`TeleportManager` 对缺失的 `warps.yml`/`teleport.yml` 调用 `saveResource` 会抛 `IllegalArgumentException`；现改为安全创建，并随 JAR 提供这两个资源
- 🐛 **出生点双实例** - `espawn`/`esetspawn` 共用同一个 `SpawnCommand`，设置后立即生效
- 🐛 **配置死键** - 实现 `player-commands.*`、`economy.enabled`、`homes.enabled`、`warps.enabled/max-warps`、`teleport.enabled/cooldown/safety-check`、`chat.color-enabled`、`system.auto-save-interval` 等读取
- 🐛 **时间分段路径** - `debug-info.time-segments.dawn[0]` 改为合法的 `dawn.0`

#### K10 / 数字城市
- 🐛 **回执误报** - 城市/传统事件的裸 `{"status":"ok"}` 现在按正常 ACK 处理，不再每条回执刷“未知响应类型”
- 🐛 **仪表盘 2KB 静默丢弃** - 固件 `processCityDashboardData` 改为一次 16KB 解析；`handleCityEvent`/`extractJsonObject` 容量参数化；插件侧移除固件未消费的 `recent_events`
- 🐛 **K10 队列丢唤醒** - 处理线程退出后重新检查队列；节流状态加锁，避免异步聊天并发穿透
- 🐛 **日志降噪** - 每条消息的入队/成功日志默认降为 FINE，`k10.debug-mode: true` 时恢复
- 🐛 **城市状态陈旧** - 每帧仪表盘重算状态；当日死亡数/峰值按天重置，避免累计死亡永久 CRITICAL

#### 命令/经济/保护
- 🐛 **/egm 不可用** - 参数改为 `<模式> [玩家]`，支持 `/gamemode` 别名；权限/开关按命令名解析
- 🐛 **/eco 无法识别** - 修复 `eco` 被误去首字母 `e` 的问题
- 🐛 **NaN/Infinity 注入** - 金额解析统一走 `BankUtils.parseAmount`，拒绝非有限值；银行/钱包/商店同步校验
- 🐛 **/back 记错人/方向反** - 接受传送请求时按 `isToPlayer` 正确选择移动者；插件传送统一记录起点
- 🐛 **菜单/商店点击自己背包** - 只处理 GUI 顶层物品，点击玩家背包不再触发治疗/买卖
- 🐛 **方块破坏复制/保护绕过** - 使用 HIGHEST + ignoreCancelled，按工具计算掉落，只掉落放不下的部分，创造模式跳过
- 🐛 **火焰刀岩浆残留** - 只在固体方块上生成，忽略已取消伤害；额外伤害只由物品属性提供一次
- 🐛 **商店扣款丢货** - 背包放不下时退款；Shift 右键只卖背包存储区，不卖盔甲
- 🐛 **悬浮文字泄漏** - 在线人数/跟随任务退出自动清理；改用共享插件实例，取消反射清理与 null 插件风险
- 🐛 **核弹风暴** - 粒子 100→48、TNT 10→6，增加 60 秒冷却

#### 聊天/欢迎
- 🐛 **脏话消息外泄** - K10/数字城市/格式化监听器加 `ignoreCancelled`，被过滤的消息不再发往设备/统计
- 🐛 **表情转换无效** - ChatHint 现在真正 `setMessage`，并先于 ChatFormat 注册；配置键修正为 `chat.hints.*`
- 🐛 **异步发包** - 聊天动作栏统一调度回主线程；欢迎动作栏改用真正的 ActionBar，不再覆盖标题
- 🐛 **欢迎指南重复** - 只保留 HelpMessageListener 一份；`%join_count%`/`%total_players_today%`/`%uptime%` 改为真实统计
- 🐛 **配置热重载** - `/eanreload` 与 Web 保存统一调用 `reloadAll()`，重启调度器、刷新缓存配置

#### Web
- 🐛 **保存 500** - NanoHTTPD 的 JSON body 存在 `files["postData"]`，不再当作临时文件路径读取
- 🐛 **面板重写** - 自动 GET `/api/config`，支持 K10/环境/数字城市等全部主要配置；修复布尔值 `||` 误判、数组编辑、重置按钮
- 🐛 **线程安全** - 保存后通过 `callSyncMethod` 回主线程重载

### ⚡ 优化 (Changed)
- 环境采集：去掉从未上屏的 `resources` 统计，湿度只算一次，减少约 1/3 方块扫描
- 住户扫描：只统计床头、只统计门下半部分，避免一张床算两户
- K10：队列/节流/关服保护，`sendMessageAsync` 返回是否成功入队；环境统计区分“入队/确认”
- 更新检查：从假地址改为 GitHub Releases，支持 `update-check.enabled`
- 配置：移除 6 组无效键；BossBar 颜色/样式、`join-message.prefix`、`gameplay.*` 等落入配置
- 测试：新增 JUnit 5 与 4 个真实测试（金额校验、版本比较）

### ⚠️ 不兼容 (Breaking)
- `chat.hints.hints-enabled` → `chat.hints.enabled`（旧键仍兼容读取）
- 移除 `economy.currency-symbol`、`economy.decimal-places`、`homes.teleport-cooldown`、`system.debug-mode`、`permissions.*`、`chat-format.actionbar-notification.title-*`
- 新增权限节点，详见 `plugin.yml`

### 📌 配套
- 插件 v1.3.0 建议配合固件 **v3.3.2**（`mn.txt`）；旧固件仍可通信，但仪表盘大帧可能受 2KB 限制
- 替换 JAR 后必须重启服务器；`/reload` 不会重新初始化调度器/Web 端口

---

## [1.2.1] - 2026-08-17

### 🔄 变更 (Changed)

#### K10 设备端 (固件 v3.3)
- 🐛 **修复环境聚合报告接收失败**（v3.3.1 热修） - `handleMcEvent`/`handleEnvironmentSummary` 改为单次 16384 字节解析；根因是旧 `getJsonValue` 内部 2048 字节 doc 在 10+ 玩家（body 超 2KB）时 NoMemory，`event` 解析为空匹配失败，K10 回了裸 `{"status":"ok"}`（无 request_id/response_type），插件端报"未知响应类型"
- 🐛 `getJsonValue`/`extractJsonObject` 容量参数化（默认 2048，大消息传大容量）
- 🐛 JSON 解析失败改为回 200 + `response_type=error`（避免触发插件 HTTP 非 200 重试逻辑）
- 🐛 修复 Mind+ 编译报错（ambiguous overload）：C++ 默认参数只能写在前置声明或定义其中一处（getJsonValue 的 `capacity=2048` 现放在前置声明）
- 📌 部署提示：插件日志出现 `{deprecated=true, status=ok}` / "未知响应类型" 说明服务器仍在运行旧版 JAR，需替换 `target/myplugin-1.0-SNAPSHOT.jar`（588KB shaded 版）并重启
- 🗑️ **删除聊天模式与单玩家环境页** - 界面精简为：城市大屏（默认）/ 环境表格 / 玩家环境详情子页
- 📋 **环境表格（融合页）** - 每玩家一行（名字+温度+湿度，最多20行与插件上限对齐）；A键/B键上下移动选中行（高亮+▲▼滚动指示）；列表末尾固定「← 返回城市大屏」行
- 🔍 **玩家环境详情子页** - 表格内 A+B 同按进入选中玩家详情（温度/湿度/光照/风速/天气/群系），按任意键返回列表
- 🔇 **事件不再占用消息列表** - 加入/退出/阵亡/聊天/住户变动/红石激增改为全屏临时通知（约4秒）+ LED 反应后自动恢复原页面；SD 卡日志保留
- 🔕 **环境报告到达不再强制切页** - 只刷新表格数据+LED提示；正在浏览表格/详情页时原地刷新（旧版会临时顶掉大屏8秒）
- 📶 **网络时序修复** - 新增 networkReady 门控：配网/连接中页面在 WiFi 就绪前绝不被城市大屏顶掉
- 🚫 **取消 A 键长按重置 WiFi** - 避免误触清配网，重置仅保留网页端 `/reset` 入口
- 📄 状态页新增环境表格收录计数（已收/总玩家）

#### 插件端
- 无代码变更（协议与 v1.2.0 完全兼容，仅文档同步）

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

**当前版本**: v1.3.0 (2026-10-25)