# Telegram Bot

基于 Spring Boot 3 的 Telegram 机器人，支持动态命令配置、内联按钮回调和运营管理后台。

## Features

- 🤖 长轮询接收 Telegram 消息，自动分片保证会话有序
- ⚡ 动态命令系统 — 运营通过后台增删改命令，无需重启服务
- 📝 Markdown / HTML 格式化回复（parseMode 可选）
- 🔘 内联按钮回调处理（确认/取消等交互）
- ⌨️ ReplyKeyboard 自定义键盘（点击即发送文字，可路由到任意命令）
- 🔑 关键词自动回复（非命令文本包含触发词时自动响应）
- 🎉 入群欢迎语（监听新成员加入事件，支持 `{user}` / `{chat}` 占位符）
- 🧠 AI 对话兜底 — 接入通义千问 LLM，支持多轮对话上下文 + 自动降级静态文本
- 👥 用户管理与权限体系 — 自动记录活跃用户，ADMIN/USER 分级，黑名单封禁
- 🛡️ 群组管理命令 — /pin /ban /kick /setdesc 等，含管理员权限校验
- 📐 消息模板变量引擎 — 回复内容支持 {{date}}/{{time}}/{{random:min-max}} 等动态替换
- ⏰ 定时任务推送 — 运营配置“目标群 + cron + 内容”，调度器按周期自动发送
- 🛡️ 滑动窗口限流防刷，有界队列防止 OOM
- 🗄️ MySQL 持久化 + MyBatis-Plus 逻辑删除
- 📊 数据统计面板 — ECharts 可视化：命令频率 Top10、日活趋势、小时分布、广播成功率
- 🖥️ 自带管理后台页面（admin.html，含指令/任务/用户/数据面板四个 Tab）

## Tech Stack

| Category | Technology | Version |
|----------|-----------|----------|
| Language | Java | 17 |
| Framework | Spring Boot | 3.5.0 |
| AI Framework | Spring AI Alibaba | 1.0.0.2 |
| LLM | DashScope (通义千问) | qwen-plus |
| ORM | MyBatis-Plus | 3.5.9 |
| Database | MySQL | 8.0 |
| Bot SDK | telegrambots | 6.8.0 |
| Chart | ECharts (CDN) | 5.x |
| Build | Maven | - |

## Quick Start

### Prerequisites

- JDK 17+
- Maven 3.6+ (or use the bundled `mvnw`)
- MySQL 5.7+ / 8.0
- HTTP 代理（国内访问 Telegram 必需）
- DashScope API Key（可选，用于 AI 对话兜底）

### 1. 配置环境变量

```powershell
$env:TELEGRAM_BOT_TOKEN = "你的BotToken"        # 从 @BotFather 获取
$env:TELEGRAM_PROXY_PORT = "7890"               # 本地代理端口
$env:MYSQL_PASSWORD = "你的MySQL密码"
$env:DASHSCOPE_API_KEY = "你的通义千问APIKey"   # 可选，从 https://dashscope.console.aliyun.com/ 获取
```

### 2. 初始化数据库

在 MySQL 中执行建表脚本：

```sql
source src/main/resources/db/schema.sql
```

确保 `cs_system` 数据库下存在 `cs_bot_command` 表。

### 3. 编译并启动

```powershell
.\mvnw.cmd spring-boot:run
```

### 4. 访问管理后台

打开浏览器访问 [http://localhost:8080/admin.html](http://localhost:8080/admin.html)

## Project Structure

```
telegram/
├── src/main/java/com/example/telegram/
│   ├── TelegramApplication.java          # 启动类
│   ├── bot/                              # 机器人入口 & 限流
│   │   ├── TelegramBot.java
│   │   ├── BroadcastTaskRunner.java      # 定时广播任务调度器
│   │   └── RateLimiter.java
│   ├── callback/                         # 按钮回调处理器
│   │   ├── CallbackQueryHandler.java
│   │   ├── YesCallbackHandler.java
│   │   └── NoCallbackHandler.java
│   ├── command/                          # 命令系统核心
│   │   ├── BotCommandHandler.java        # 处理器接口
│   │   ├── CommandContext.java           # 上下文（含 role/blocked）
│   │   ├── CommandDefinition.java        # 动态命令定义（含 systemPrompt）
│   │   ├── CommandRegistry.java          # 注册表（内置+DB合并）
│   │   ├── DynamicCommandHandler.java    # 通用动态处理器
│   │   ├── TemplateRenderer.java         # 消息模板变量渲染引擎
│   │   └── impl/                         # 内置命令实现
│   │       └── group/                    # 群管理命令（/pin /ban /kick 等）
│   │           ├── GroupAdminCommand.java        # 命令名枚举
│   │           ├── AbstractGroupAdminHandler.java # 基类（参数解析+权限校验）
│   │           └── *CommandHandler.java          # Pin/Unpin/Ban/Unban/Kick/SetDesc/SetTitle
│   ├── config/                           # 配置类
│   ├── controller/                       # REST API
│   │   ├── BotCommandController.java     # 指令管理
│   │   ├── BroadcastTaskController.java  # 广播任务
│   │   ├── TelegramUserController.java   # 用户管理
│   │   └── StatsController.java          # 数据统计 API
│   ├── dto/                              # 请求/响应对象
│   ├── entity/                           # 数据库实体
│   │   ├── BotCommandEntity.java
│   │   ├── BroadcastTaskEntity.java
│   │   ├── TelegramUserEntity.java       # 用户实体
│   │   └── MessageLogEntity.java         # 消息日志实体
│   ├── mapper/                           # MyBatis Mapper
│   │   ├── BotCommandMapper.java
│   │   ├── BroadcastTaskMapper.java
│   │   ├── TelegramUserMapper.java       # 用户 UPSERT
│   │   └── MessageLogMapper.java         # 消息日志 + 聚合查询
│   └── service/                          # 业务 & API 封装
│       ├── TelegramUserService.java      # 用户管理服务
│       ├── StatsService.java             # 数据统计服务 (@Async 写入)
│       └── ai/                           # AI 对话补全服务
│           ├── ChatCompletionService.java        # AI 能力抽象接口
│           ├── DefaultChatCompletionService.java # 默认实现（静态文本降级）
│           ├── QwenChatCompletionService.java    # 通义千问 LLM 实现 (@Primary)
│           └── ChatMemoryManager.java            # 多轮对话上下文管理
├── src/main/resources/
│   ├── application.properties
│   ├── logback-spring.xml
│   ├── db/schema.sql
│   └── static/admin.html
├── docs/
│   └── ROADMAP.md                        # 扩展路线图
└── pom.xml
```

## Architecture

```
Telegram Server
      │
      ▼ (Long Polling)
TelegramBot.onUpdateReceived()
      │
      ├──► RateLimiter ──► 超限丢弃
      ├──► chatId 哈希分片 ──► 工作线程池
      │
      ▼
processUpdate()
      ├── new_chat_members → dispatchWelcome()  (入群欢迎语 + TemplateRenderer)
      │
      ├── 文本消息 → dispatchMessage()
      │                 ├── userService.recordInteraction()  (用户 upsert)
      │                 ├── userService.isBlocked()?         (黑名单拦截)
      │                 ├── userService.getRole()            (角色注入)
      │                 │
      │                 ├── "/" 开头 → CommandRegistry.lookup()
      │                 │                  ├── 内置处理器
      │                 │                  ├── DB 动态命令
      │                 │                  │    ├── text (+ parseMode + TemplateRenderer)
      │                 │                  │    ├── photo
      │                 │                  │    ├── buttons (InlineKeyboardMarkup)
      │                 │                  │    └── keyboard (ReplyKeyboardMarkup)
      │                 │                  ├── 群管命令 (/pin /ban /kick…) → 管理员权限校验
      │                 │                  └── fallback 兜底
      │                 └── 普通文本 → 关键词匹配 → AI Fallback
      │                       ├── triggerKeywords 命中 → TemplateRenderer.render() → 自动回复
      │                       └── 未命中 → AiFallbackCommandHandler
      │                              └── QwenChatCompletionService (@Primary)
      │                                   ├── ChatClient.call() → 通义千问 LLM
      │                                   ├── ChatMemoryManager → 多轮上下文
      │                                   └── 失败降级 → DefaultChatCompletionService
      │
      ├── statsService.logMessage()  (异步记录消息日志)
      │
      └── 按钮回调 → dispatchCallback()
                        └── CallbackQueryHandler
```

**分层职责：**

| Layer | Responsibility | Key Classes |
|-------|---------------|-------------|
| Bot 入口层 | Update 解析、限流、线程调度、用户记录、日志埋点 | `TelegramBot`, `RateLimiter` |
| 命令分发层 | 命令查找与路由、黑名单拦截、权限检查 | `CommandRegistry`, `DynamicCommandHandler` |
| 处理器层 | 业务逻辑实现 | `*CommandHandler`, `*CallbackHandler` |
| 群管命令层 | 群置顶/封禁/移出/改资料，管理员权限校验 | `AbstractGroupAdminHandler`, `GroupAdminCommand` |
| AI 服务层 | LLM 对话补全、多轮上下文、降级策略 | `ChatCompletionService`, `QwenChatCompletionService`, `ChatMemoryManager` |
| 模板引擎层 | 消息变量替换渲染 | `TemplateRenderer` |
| 用户管理层 | 用户 upsert、角色分级、封禁 | `TelegramUserService`, `TelegramUserController` |
| 数据统计层 | 异步消息日志写入、聚合查询、图表数据源 | `StatsService`, `StatsController`, `MessageLogMapper` |
| 服务封装层 | Telegram API 统一出口 | `TelegramApiService` |
| 管理后台层 | 命令 CRUD + 热刷新 + 用户管理 + Dashboard | `BotCommandController`, `BotCommandService` |

## Dynamic Commands

运营人员可通过管理后台添加命令，存储在 `cs_bot_command` 表中，`CommandRegistry` 每 30 秒自动刷新。

**支持的回复类型：**

| replyType | 说明 | replyContent | replyMarkup |
|-----------|------|-------------|-------------|
| `text` | 纯文本 | 文本内容 | - |
| `photo` | 图片 | 图片 URL | - |
| `buttons` | 内联按钮（InlineKeyboardMarkup） | 文本内容 | JSON 对象数组 |
| `keyboard` | 自定义键盘（ReplyKeyboardMarkup） | 文本内容 | JSON 字符串数组 |

**格式化模式（parseMode）：**

每条命令可选择以下格式化方式之一：

| parseMode | 说明 |
|-----------|------|
| *(空)* | 纯文本（默认） |
| `MarkdownV2` | Telegram MarkdownV2 语法（加粗、斜体、链接等） |
| `HTML` | HTML 标签（`<b>`、`<i>`、`<a>` 等） |

**内联按钮 JSON 示例（buttons）：**

```json
[
  [
    {"text": "确认", "callbackData": "cmd_yes"},
    {"text": "取消", "callbackData": "cmd_no"}
  ]
]
```

**自定义键盘 JSON 示例（keyboard）：**

```json
[
  ["北京", "上海"],
  ["广州", "深圳"]
]
```

> 用户点击键盘按钮后，按钮文字会作为普通消息发送给机器人，由 `CommandRegistry` 自动路由。

**关键词自动回复：**

每条动态命令可以配置 `triggerKeywords`（逗号分隔），当用户发送的非命令文本包含其中任一关键词时，Bot 按该命令的 `replyType` 和 `content` 自动回复。匹配忽略大小写，取第一条命中的规则。

```
triggerKeywords = "价格,报价,多少钱"
→ 用户发送 "请问价格是多少？" → 命中 "价格" → 回复 content
```

**入群欢迎语：**

设置 `welcomeText` 字段后，当有人加入群聊时 Bot 自动发送欢迎消息。支持两个占位符：

| 占位符 | 替换为 |
|--------|------|
| `{user}` | 新成员名称（如 `张三(@username)`） |
| `{chat}` | 群聊标题 |

```
welcomeText = "欢迎 {user} 加入 {chat}！请先看 /help"
→ "欢迎 张三(@zhangsan) 加入 我的群！请先看 /help"
```

> 同名命令中 DB 动态命令会覆盖内置处理器，无需改代码即可替换 `/help` 等行为。

## Broadcast Tasks（定时广播任务）

运营可在后台「定时任务」Tab 配置“目标群 + cron 表达式 + 回复内容”，调度器 `BroadcastTaskRunner` 每分钟扫描一次，命中则自动推送到指定 chat。

**工作机制：**

- 采用「数据库 + 分钟轮询」而非动态注册 `@Scheduled`，好处是改配置即时生效、无需重启（与 `CommandRegistry` 轮询刷新思路一致）
- 使用 Spring `CronExpression` 解析 6 位表达式（秒 分 时 日 月 周）；创建/更新时校验合法性
- 命中判断：以 `[now-1min, now]` 为窗口，若区间内存在触发点则执行
- 发送逻辑复用 replyType 体系（text/photo/buttons/keyboard），与动态命令一致

**Cron 示例：**

| 表达式 | 含义 |
|--------|------|
| `0 0 9 * * ?` | 每天 09:00 |
| `0 */30 * * * ?` | 每 30 分钟 |
| `0 0 10 * * 1` | 每周一 10:00 |

> 目标群 `chat_id` 可从 Telegram 消息日志获取（群聊 ID 通常为负数）。需确保 Bot 已被拉入目标群且有发言权限。

## AI LLM 对话 (通义千问)

通过 Spring AI Alibaba 接入 DashScope 通义千问大模型，作为 AI 兜底的核心引擎。

**工作流程：**
1. 用户发送非命令文本且关键词未命中时，触发 `AiFallbackCommandHandler`
2. `QwenChatCompletionService` (@Primary) 调用 `ChatClient` → 通义千问 API
3. `ChatMemoryManager` 按 userId 维护最近 N 轮对话历史，提供多轮上下文
4. LLM 不可用或调用失败时，自动降级到 `DefaultChatCompletionService` 静态文本

**每个命令可配置独立 System Prompt：**
在 `cs_bot_command` 表的 `system_prompt` 字段设置后，该命令触发的 AI 回复会使用自定义提示词而非全局默认。

**环境变量：**
```powershell
$env:DASHSCOPE_API_KEY = "你的通义千问 API Key"   # 从 https://dashscope.console.aliyun.com/ 获取
```

> 未配置 API Key 时 Bot 仍可正常启动，所有 AI 兜底自动走静态文本降级。

## User Management（用户管理与权限体系）

Bot 自动记录与其交互的所有 Telegram 用户，支持角色分级和黑名单封禁。

**核心机制：**
- 每次消息交互自动 UPSERT 用户信息（username、first_name、累计消息数、最后活跃时间）
- 黑名单用户（status=0）的消息直接忽略，不进入命令分发
- `CommandContext` 携带 role 和 blocked 字段，处理器可做细粒度权限控制

**角色说明：**

| 角色 | 权限 |
|------|------|
| `ADMIN` | 可使用管理类命令（如群管操作） |
| `USER` | 普通用户，仅可使用公共命令 |

**API：**

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/users?page=1&size=50` | 分页查询用户列表 |
| PATCH | `/api/users/{id}/role` | 切换角色 (ADMIN/USER) |
| PATCH | `/api/users/{id}/status` | 封禁/解封 (status: 1/0) |

## Template Variables（消息模板变量引擎）

运营配置的回复内容支持动态变量替换，发送前由 `TemplateRenderer` 渲染。

**支持的变量：**

| 变量 | 替换为 | 示例输出 |
|------|-------|----------|
| `{{date}}` | 当前日期 | `2026-11-06` |
| `{{time}}` | 当前时间 | `14:30:00` |
| `{{random:min-max}}` | 范围内随机整数 | `{{random:1000-9999}}` → `4521` |
| `{{username}}` | 用户 @username | `zhangsan` |
| `{{chat_title}}` | 群聊标题 | `我的群` |

**使用示例：**
```
📅 今天是 {{date}}，当前时间 {{time}}
🎲 幸运号码：{{random:1000-9999}}
```

入群欢迎语同时兼容旧语法 `{user}` / `{chat}` 和新模板变量语法。

> 模板变量在所有回复类型中生效：text、photo、buttons、keyboard，以及关键词自动回复。

## Built-in Commands

| Command | Handler | Description |
|---------|---------|-------------|
| `/start` | `StartCommandHandler` | 欢迎消息 |
| `/help` | `HelpCommandHandler` | 帮助信息 |
| `/echo` | `EchoCommandHandler` | 回显输入 |
| `/photo` | `PhotoCommandHandler` | 发送图片 |
| `/buttons` | `ButtonsCommandHandler` | 带按钮消息 |
| `/pin` `/ban` `/kick` … | `command/impl/group/*` | 群管理命令（见下节） |
| *(兜底)* | `AiFallbackCommandHandler` | AI 对话兜底（非命令且关键词未命中时触发） |

## Group Administration（群组管理命令）

Bot 被设为群管理员后，提供一组内置的群管命令（不走 DB 动态配置，因涉及权限校验）。

**可用命令：**

| 命令 | 功能 | Telegram API |
|------|------|-------------|
| `/pin [message_id]` | 置顶消息（缺省作用于触发命令的那条） | `PinChatMessage` |
| `/unpin [message_id]` | 取消置顶（缺省取消当前置顶的那条） | `UnpinChatMessage` |
| `/ban <user_id> [天数]` | 封禁成员（可选时长，缺省永久） | `BanChatMember` |
| `/unban <user_id>` | 解封成员 | `UnbanChatMember` |
| `/kick <user_id>` | 移出/禁言成员（禁用全部发言权限） | `RestrictChatMember` |
| `/setdesc <text>` | 修改群描述 | `SetChatDescription` |
| `/settitle <text>` | 修改群名称 | `SetChatTitle` |

**权限校验（双重保险）：**
1. 优先调用 Telegram 官方 `GetChatMember` 获取发送者在群中的真实状态，仅 `creator` / `administrator` 放行
2. 查询失败时降级到本地 `cs_telegram_user.role`（`ADMIN` 才放行）
3. 非群聊环境（chatId > 0）直接拒绝执行

**实现结构：**
- `GroupAdminCommand` 枚举集中定义命令名，消除字符串魔法值
- `AbstractGroupAdminHandler` 基类统一处理参数解析 + 群聊/权限校验，子类只需实现 `doHandle()`
- 各处理器 `@Component` 注册，`CommandRegistry` 启动时自动收集

> 使用前提：需先在群内将 Bot 设为管理员，并授予置顶、封禁、修改资料等对应权限，否则调用会返回失败提示。

## Admin API

基础路径：`/api/commands`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/commands?page=1&size=50` | 分页查询 |
| POST | `/api/commands` | 创建命令 |
| PUT | `/api/commands/{id}` | 更新命令 |
| PATCH | `/api/commands/{id}/status` | 启用/禁用 |
| DELETE | `/api/commands/{id}` | 删除（逻辑删除） |
| POST | `/api/commands/reload` | 手动刷新注册表 |

广播任务 API，基础路径：`/api/broadcast-tasks`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/broadcast-tasks?page=1&size=50` | 分页查询 |
| POST | `/api/broadcast-tasks` | 创建广播任务 |
| PUT | `/api/broadcast-tasks/{id}` | 更新任务 |
| PATCH | `/api/broadcast-tasks/{id}/status` | 启用/禁用 |
| DELETE | `/api/broadcast-tasks/{id}` | 删除（逻辑删除） |

数据统计 API，基础路径：`/api/stats`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/stats/commands?limit=10` | 命令使用频率 Top N（最近 7 天） |
| GET | `/api/stats/daily-active?days=30` | 日活用户趋势 |
| GET | `/api/stats/hourly-volume` | 今日每小时消息量分布 |
| GET | `/api/stats/broadcast` | 广播任务执行成功率 |

## Dashboard（数据统计面板）

访问 `admin.html` 的「数据面板」Tab，基于 ECharts 可视化展示：

| 图表 | 数据源 | 说明 |
|------|--------|------|
| 命令频率柱状图 | `cs_bot_message_log` | 最近 7 天 Top 10 命令使用次数 |
| 日活趋势折线图 | `cs_bot_message_log` | 最近 30 天每日独立用户数 |
| 小时分布柱状图 | `cs_bot_message_log` | 今日 0~23 时消息量分布 |
| 广播成功率饼图 | `cs_bot_message_log` | BroadcastTaskRunner 成功/失败比例 |

**埋点机制：**
- 命令处理、关键词命中、广播任务发送均通过 `StatsService.logMessage()` 异步写入日志表
- `@Async` + `@EnableAsync` 保证不阻塞主流程，异常仅 warn 不影响业务

## Configuration

所有配置项位于 `application.properties`，敏感值通过环境变量注入：

| Variable | Default | Description |
|----------|---------|-------------|
| `TELEGRAM_BOT_TOKEN` | *(空)* | Bot Token (**必填**) |
| `TELEGRAM_BOT_USERNAME` | `xgx_test_private_bot` | Bot 用户名 |
| `TELEGRAM_PROXY_HOST` | `127.0.0.1` | HTTP 代理地址 |
| `TELEGRAM_PROXY_PORT` | `7890` | 代理端口（设 0 禁用） |
| `TELEGRAM_LONG_POLL_TIMEOUT` | `25` | 长轮询超时秒数 |
| `DASHSCOPE_API_KEY` | *(空)* | 通义千问 API Key（选填，未配置则走静态降级） |
| `DATABASE_HOST` | `localhost` | MySQL 主机 |
| `MYSQL_PASSWORD` | *(空)* | MySQL 密码 |

其他可调参数：

```properties
telegram.worker-pool-size=4              # 工作线程数
telegram.worker-queue-capacity=200       # 每分片队列容量
telegram.rate-limit.max-requests=20      # 限流：最大请求数
telegram.rate-limit.window-millis=10000  # 限流：窗口大小(ms)
server.port=8080                         # Web 端口

# ===== AI Fallback =====
telegram.ai.fallback.enabled=true        # 启用开关
telegram.ai.fallback.reply-text=抱歉…    # LLM 不可用时的降级文本
telegram.ai.fallback.parse-mode=         # 回复格式化模式（空/MarkdownV2/HTML）

# ===== DashScope LLM =====
spring.ai.dashscope.api-key=${DASHSCOPE_API_KEY:}
spring.ai.dashscope.chat.options.model=qwen-plus                # 模型选择
telegram.ai.chat-memory.max-rounds=10                           # 多轮对话保留轮数

# ===== Broadcast Tasks =====
telegram.broadcast-task.scan-cron=0 * * * * *   # 调度器扫描周期（默认每分钟）
```

## Logging

日志由 `logback-spring.xml` 独立配置，输出到 `logs/` 目录：

- `telegram-bot.log` — 全量日志
- `telegram-bot-error.log` — ERROR 级别
- 按日期自动滚动

## Extension Guide

**新增内置命令：**
1. 在 `command/impl/` 下实现 `BotCommandHandler` 接口
2. 加 `@Component` 注解注册为 Spring Bean
3. `CommandRegistry` 启动时自动收集

**新增按钮回调：**
1. 在 `callback/` 下实现 `CallbackQueryHandler` 接口
2. 加 `@Component` 注解
3. `TelegramBot` 构造时自动按 `data()` 建立映射

**通过后台添加动态命令：**
直接在 `admin.html` 页面操作，保存后立即生效。

**新增群管理命令：**
1. 在 `GroupAdminCommand` 枚举中添加一个常量（带命令名与注释）
2. 在 `command/impl/group/` 下新建处理器继承 `AbstractGroupAdminHandler`，实现 `name()` 返回枚举值 + `doHandle()` 具体动作
3. 加 `@Component`，权限校验由基类自动完成

**新增回复类型分支：**
1. 在 `DynamicCommandHandler.handle()` 的 `switch` 中添加 `case "yourType" ->`
2. 实现对应的处理方法（参考 `handleKeyboard`）
3. 在 `admin.html` 下拉选项中增加类型
4. 如需特殊 UI 字段，扩展 `toggleFields()` 函数

**扩展关键词/欢迎语逻辑：**
- 关键词匹配规则在 `CommandRegistry.lookupKeyword()` 中（当前为包含匹配，可改为精确/正则）
- 欢迎语渲染在 `TelegramBot.dispatchWelcome()` 中（可增加更多占位符）
- 两者均从数据库 `cs_bot_command` 表读取，刷新周期同命令注册表（默认 30s）

**接入真实 LLM 替换 AI 兜底：**
只需新建一个类实现 `ChatCompletionService` 接口并标注 `@Primary`，无需修改 Handler 代码：
```java
@Primary
@Service
public class QwenChatCompletionService implements ChatCompletionService {
    @Override
    public String complete(long userId, long chatId, String userText) {
        // 调用通义千问 / OpenAI / 本地模型 API
        return llmClient.chat(userId, userText);
    }
}
```
`DefaultChatCompletionService` 会自动退居二线（Spring 优先注入 `@Primary` Bean）。

## Database Schema

核心表 `cs_bot_command`：

| Column | Type | Description |
|--------|------|-------------|
| `command_name` | VARCHAR(64) | 命令名（不含 `/`），唯一 |
| `description` | VARCHAR(255) | 功能描述 |
| `reply_type` | VARCHAR(16) | `text` / `photo` / `buttons` / `keyboard` |
| `reply_content` | TEXT | 回复内容或图片 URL |
| `reply_markup` | TEXT | 按钮/键盘 JSON 配置 |
| `parse_mode` | VARCHAR(16) | `MarkdownV2` / `HTML` / 空 |
| `trigger_keywords` | VARCHAR(512) | 逗号分隔触发关键词 |
| `welcome_text` | TEXT | 入群欢迎语模板 |
| `system_prompt` | TEXT | AI 兜底时该命令的系统提示词（覆盖全局配置） |
| `enabled` | TINYINT | 1=启用 0=禁用 |
| `deleted` | TINYINT | 逻辑删除标记 |

广播任务表 `cs_bot_broadcast_task`：

| Column | Type | Description |
|--------|------|-------------|
| `name` | VARCHAR(64) | 任务唯一标识 |
| `chat_id` | BIGINT | 目标会话/群 ID（推送至此） |
| `cron_expression` | VARCHAR(64) | Spring cron 表达式（6 位） |
| `reply_type` | VARCHAR(16) | `text` / `photo` / `buttons` / `keyboard` |
| `reply_content` | TEXT | 回复内容或图片 URL |
| `reply_markup` | TEXT | 按钮/键盘 JSON 配置 |
| `parse_mode` | VARCHAR(16) | `MarkdownV2` / `HTML` / 空 |
| `status` | TINYINT | 1=启用 0=禁用 |
| `last_exec_time` | DATETIME | 上次执行时间 |
| `next_exec_time` | DATETIME | 下次预计执行时间 |

用户表 `cs_telegram_user`：

| Column | Type | Description |
|--------|------|-------------|
| `user_id` | BIGINT | Telegram 用户 ID（唯一索引，UPSERT 键） |
| `username` | VARCHAR(64) | @username |
| `first_name` | VARCHAR(64) | 名字 |
| `last_name` | VARCHAR(64) | 姓氏 |
| `role` | VARCHAR(16) | ADMIN / USER |
| `status` | TINYINT | 1=正常 0=黑名单 |
| `first_seen_time` | DATETIME | 首次交互时间 |
| `last_active_time` | DATETIME | 最近活跃时间 |
| `message_count` | INT | 累计消息数 |

消息日志表 `cs_bot_message_log`：

| Column | Type | Description |
|--------|------|-------------|
| `user_id` | BIGINT | Telegram 用户 ID |
| `chat_id` | BIGINT | 会话/群 ID |
| `command_name` | VARCHAR(64) | 命令名或关键词（非命令为 NULL） |
| `handler_type` | VARCHAR(32) | 处理器类型简名 |
| `success` | TINYINT | 1=成功 0=失败 |
| `gmt_create` | DATETIME | 消息时间戳 |

索引：`idx_user_date(user_id, gmt_create)`、`idx_command(command_name)`、`idx_hourly(gmt_create)`

## Roadmap

详细扩展计划见 [docs/ROADMAP.md](docs/ROADMAP.md)，涵盖三个阶段：

- **Phase 1** — 核心能力补全 ✅（LLM 接入、用户权限体系、模板变量引擎）
- **Phase 2** — 体验提升 ✅ 数据面板、群管命令已完成；多媒体、Webhook 待做
- **Phase 3** — 产品化扩展（Mini App、深度链接、RSS 聚合、分布式改造）

## License

Internal project — all rights reserved.
