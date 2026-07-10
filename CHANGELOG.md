# 📝 变更日志 (Changelog)

本文件记录了 EssentialsX-Clone 项目的重要变更。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

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