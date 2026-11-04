# Telegram Bot

基于 Spring Boot 3 的 Telegram 机器人，支持动态命令配置、内联按钮回调和运营管理后台。

## Features

- 🤖 长轮询接收 Telegram 消息，自动分片保证会话有序
- ⚡ 动态命令系统 — 运营通过后台增删改命令，无需重启服务
- 🔘 内联按钮回调处理（确认/取消等交互）
- 🛡️ 滑动窗口限流防刷，有界队列防止 OOM
- 🗄️ MySQL 持久化 + MyBatis-Plus 逻辑删除
- 🖥️ 自带管理后台页面（admin.html）

## Tech Stack

| Category | Technology | Version |
|----------|-----------|---------|
| Language | Java | 17 |
| Framework | Spring Boot | 3.5.0 |
| ORM | MyBatis-Plus | 3.5.9 |
| Database | MySQL | 8.0 |
| Bot SDK | telegrambots | 6.8.0 |
| Build | Maven | - |

## Quick Start

### Prerequisites

- JDK 17+
- Maven 3.6+ (or use the bundled `mvnw`)
- MySQL 5.7+ / 8.0
- HTTP 代理（国内访问 Telegram 必需）

### 1. 配置环境变量

```powershell
$env:TELEGRAM_BOT_TOKEN = "你的BotToken"        # 从 @BotFather 获取
$env:TELEGRAM_PROXY_PORT = "7890"               # 本地代理端口
$env:MYSQL_PASSWORD = "你的MySQL密码"
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
│   │   └── RateLimiter.java
│   ├── callback/                         # 按钮回调处理器
│   │   ├── CallbackQueryHandler.java
│   │   ├── YesCallbackHandler.java
│   │   └── NoCallbackHandler.java
│   ├── command/                          # 命令系统核心
│   │   ├── BotCommandHandler.java        # 处理器接口
│   │   ├── CommandContext.java           # 上下文
│   │   ├── CommandDefinition.java        # 动态命令定义
│   │   ├── CommandRegistry.java          # 注册表（内置+DB合并）
│   │   ├── DynamicCommandHandler.java    # 通用动态处理器
│   │   └── impl/                         # 内置命令实现
│   ├── config/                           # 配置类
│   ├── controller/                       # REST API
│   ├── dto/                              # 请求/响应对象
│   ├── entity/                           # 数据库实体
│   ├── mapper/                           # MyBatis Mapper
│   └── service/                          # 业务 & API 封装
├── src/main/resources/
│   ├── application.properties
│   ├── logback-spring.xml
│   ├── db/schema.sql
│   └── static/admin.html
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
      ├── 文本消息 → dispatchMessage()
      │                 ├── "/" 开头 → CommandRegistry.lookup()
      │                 │                  ├── 内置处理器
      │                 │                  ├── DB 动态命令
      │                 │                  └── fallback 兜底
      │                 └── 普通文本 → fallback
      │
      └── 按钮回调 → dispatchCallback()
                        └── CallbackQueryHandler
```

**分层职责：**

| Layer | Responsibility | Key Classes |
|-------|---------------|-------------|
| Bot 入口层 | Update 解析、限流、线程调度 | `TelegramBot`, `RateLimiter` |
| 命令分发层 | 命令查找与路由 | `CommandRegistry`, `DynamicCommandHandler` |
| 处理器层 | 业务逻辑实现 | `*CommandHandler`, `*CallbackHandler` |
| 服务封装层 | Telegram API 统一出口 | `TelegramApiService` |
| 管理后台层 | 命令 CRUD + 热刷新 | `BotCommandController`, `BotCommandService` |

## Dynamic Commands

运营人员可通过管理后台添加命令，存储在 `cs_bot_command` 表中，`CommandRegistry` 每 30 秒自动刷新。

**支持的回复类型：**

| replyType | 说明 | replyContent | replyMarkup |
|-----------|------|-------------|-------------|
| `text` | 纯文本 | 文本内容 | - |
| `photo` | 图片 | 图片 URL | - |
| `buttons` | 带按钮文本 | 文本内容 | JSON 按钮配置 |

**按钮 JSON 示例：**

```json
[
  [
    {"text": "确认", "callbackData": "cmd_yes"},
    {"text": "取消", "callbackData": "cmd_no"}
  ]
]
```

> 同名命令中 DB 动态命令会覆盖内置处理器，无需改代码即可替换 `/help` 等行为。

## Built-in Commands

| Command | Handler | Description |
|---------|---------|-------------|
| `/start` | `StartCommandHandler` | 欢迎消息 |
| `/help` | `HelpCommandHandler` | 帮助信息 |
| `/echo` | `EchoCommandHandler` | 回显输入 |
| `/photo` | `PhotoCommandHandler` | 发送图片 |
| `/buttons` | `ButtonsCommandHandler` | 带按钮消息 |

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

## Configuration

所有配置项位于 `application.properties`，敏感值通过环境变量注入：

| Variable | Default | Description |
|----------|---------|-------------|
| `TELEGRAM_BOT_TOKEN` | *(空)* | Bot Token (**必填**) |
| `TELEGRAM_BOT_USERNAME` | `xgx_test_private_bot` | Bot 用户名 |
| `TELEGRAM_PROXY_HOST` | `127.0.0.1` | HTTP 代理地址 |
| `TELEGRAM_PROXY_PORT` | `7890` | 代理端口（设 0 禁用） |
| `TELEGRAM_LONG_POLL_TIMEOUT` | `25` | 长轮询超时秒数 |
| `DATABASE_HOST` | `localhost` | MySQL 主机 |
| `MYSQL_PASSWORD` | *(空)* | MySQL 密码 |

其他可调参数：

```properties
telegram.worker-pool-size=4              # 工作线程数
telegram.worker-queue-capacity=200       # 每分片队列容量
telegram.rate-limit.max-requests=20      # 限流：最大请求数
telegram.rate-limit.window-millis=10000  # 限流：窗口大小(ms)
server.port=8080                         # Web 端口
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

## License

Internal project — all rights reserved.
