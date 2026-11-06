# Telegram Bot 扩展路线图

> 基于当前已实现功能梳理的下一步规划，按阶段和优先级排列，方便后续逐项落地。

## 已完成功能基线

| # | 功能 | 关键类/文件 |
|---|------|------------|
| ✅ | 长轮询消息接收 + chatId 分片有序 | `TelegramBot` |
| ✅ | 动态命令系统（DB 配置，30s 刷新） | `CommandRegistry`, `DynamicCommandHandler` |
| ✅ | Markdown / HTML 格式化回复 | parseMode 字段 |
| ✅ | 内联按钮回调 (InlineKeyboardMarkup) | `CallbackQueryHandler`, `YesCallbackHandler` |
| ✅ | ReplyKeyboard 自定义键盘 | `handleKeyboard()` |
| ✅ | 关键词自动回复 | `CommandRegistry.lookupKeyword()` |
| ✅ | 入群欢迎语 | `dispatchWelcome()` |
| ✅ | AI 对话兜底（接口预留） | `ChatCompletionService` |
| ✅ | 定时广播任务 | `BroadcastTaskRunner` |
| ✅ | 滑动窗口限流防刷 | `RateLimiter` |
| ✅ | MySQL 持久化 + MyBatis-Plus 逻辑删除 | `BotCommandEntity`, `BroadcastTaskEntity` |
| ✅ | 运营管理后台 | `admin.html` |

---

## Phase 1 — 核心能力补全（建议优先实施）

### 1.1 接入真实 LLM（通义千问 / OpenAI / DeepSeek）

**目标：** 将 `DefaultChatCompletionService` 替换为真正的 AI 对话能力。

**技术方案：**
```java
@Primary
@Service
public class QwenChatCompletionService implements ChatCompletionService {
    // 调用通义千问 DashScope API 或 Spring AI Alibaba ChatClient
}
```

**具体任务：**
- [x] 引入 `spring-ai-alibaba-starter` 依赖（已在父工程）
- [x] 在 `application.properties` 中增加 API Key 配置项：`spring.ai.dashscope.api-key=${DASHSCOPE_API_KEY}`
- [x] 新建 `QwenChatCompletionService`，注入 `ChatClient`，实现 `complete()` 方法
- [x] 支持多轮对话上下文：按 `userId` 维护最近 N 条消息历史（可用 `ConcurrentHashMap<Long, List<Message>>` 或 Redis）
- [x] 每个动态命令可配置独立的 System Prompt（`cs_bot_command` 表新增 `system_prompt` 字段）
- [x] 超时与降级：LLM 调用失败时回退到 `DefaultChatCompletionService` 静态文本

**涉及文件：**
- `service/ai/ChatCompletionService.java` — 接口不变 ✅
- `service/ai/DefaultChatCompletionService.java` — 保留为降级 Bean ✅
- 新建 `service/ai/QwenChatCompletionService.java` ✅
- 新建 `service/ai/ChatMemoryManager.java` ✅ (多轮上下文管理)
- `entity/BotCommandEntity.java` — 新增 `systemPrompt` 字段 ✅
- `dto/CommandCreateRequest.java` / `CommandUpdateRequest.java` / `CommandVO.java` — 加字段 ✅
- `command/CommandDefinition.java` — record 加 `systemPrompt` 参数 ✅
- `command/CommandRegistry.java` — 传递 systemPrompt ✅
- `resources/db/schema.sql` — 加列 ✅
- `resources/application.properties` — DashScope 配置 ✅
- `pom.xml` — BOM + starter-dashscope 依赖 ✅
- `resources/static/admin.html` — AI 系统提示词输入框 ✅

---

### 1.2 用户管理与权限体系

**目标：** 记录活跃用户，区分管理员/普通用户，支持黑名单封禁。

**数据模型：**
```sql
CREATE TABLE cs_telegram_user (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE COMMENT 'Telegram 用户ID',
    username        VARCHAR(64) COMMENT '用户名',
    first_name      VARCHAR(64),
    last_name       VARCHAR(64),
    role            VARCHAR(16) DEFAULT 'USER' COMMENT 'ADMIN / USER',
    status          TINYINT DEFAULT 1 COMMENT '1=正常 0=黑名单',
    first_seen_time DATETIME,
    last_active_time DATETIME,
    message_count   INT DEFAULT 0 COMMENT '累计消息数',
    deleted         TINYINT DEFAULT 0,
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**具体任务：**
- [x] 建表 `cs_telegram_user`
- [x] 新建 `TelegramUserEntity`、`TelegramUserMapper`、`TelegramUserService`
- [x] 在 `TelegramBot.processUpdate()` 中每次交互自动 upsert 用户信息
- [x] `CommandContext` 增加 `role` / `isBlocked` 字段
- [x] `CommandRegistry` 分发前检查用户状态：黑名单用户直接忽略
- [x] admin.html 新增「用户管理」Tab：列表查询、角色切换、封禁/解封
- [x] REST API `/api/users` 分页查询 + PATCH 修改角色/状态

**涉及文件：**
- 新建 `entity/TelegramUserEntity.java` ✅
- 新建 `mapper/TelegramUserMapper.java` ✅ (含 UPSERT SQL)
- 新建 `service/TelegramUserService.java` ✅
- 新建 `controller/TelegramUserController.java` ✅
- 新建 `dto/TelegramUserVO.java` ✅
- 修改 `bot/TelegramBot.java` — 用户记录 + 黑名单检查 ✅
- 修改 `command/CommandContext.java` — 增加 role/blocked 字段 ✅
- 修改 `resources/db/schema.sql` — 新增 cs_telegram_user 表 ✅
- 修改 `resources/static/admin.html` — 用户管理 Tab ✅

---

### 1.3 消息模板变量引擎

**目标：** 让运营配置的回复内容支持动态变量替换，提升灵活性。

**支持的变量：**

| 变量 | 替换为 |
|------|-------|
| `{{username}}` | 用户 @username |
| `{{first_name}}` | 用户名字 |
| `{{chat_title}}` | 群聊标题 |
| `{{date}}` | 当前日期 yyyy-MM-dd |
| `{{time}}` | 当前时间 HH:mm:ss |
| `{{random:min-max}}` | 范围内随机整数 |
| `{{count}}` | 该命令被触发总次数 |

**具体任务：**
- [x] 新建 `TemplateRenderer` 工具类，用正则匹配 `{{xxx}}` 并替换
- [x] 在 `DynamicCommandHandler.handle()` 发送前调用渲染
- [x] 在 `TelegramBot.dispatchWelcome()` 欢迎语中也支持此语法
- [x] 关键词回复也支持模板变量渲染
- [x] admin.html 帮助面板补充变量说明文档

**涉及文件：**
- 新建 `command/TemplateRenderer.java` ✅
- 修改 `command/DynamicCommandHandler.java` ✅ (handle/handleButtons/handleKeyboard)
- 修改 `bot/TelegramBot.java` ✅ (dispatchWelcome + handleKeywordReply)

---

## Phase 2 — 体验与运营提升

### ✅ 2.1 数据统计面板

**目标：** 在 admin.html 增加 Dashboard，可视化展示运营数据。

**图表内容：**
- 命令使用频率 Top 10（柱状图）
- 日活用户趋势（折线图）
- 每小时消息量分布（热力图/面积图）
- 广播任务执行成功率（饼图）

**具体任务：**
- [ ] 新建 `cs_bot_message_log` 表记录每条消息（command_name, user_id, chat_id, timestamp）
- [ ] 新建统计 API：`/api/stats/commands`、`/api/stats/daily-active`、`/api/stats/hourly-volume`
- [ ] admin.html 新增 Dashboard Tab，引入 ECharts CDN
- [ ] 定时聚合任务（可选）：每天凌晨汇总前一天数据到统计表

**涉及文件：**
- 新建 `entity/MessageLogEntity.java`
- 新建 `mapper/MessageLogMapper.java`
- 新建 `service/StatsService.java`
- 新建 `controller/StatsController.java`
- 修改 `resources/static/admin.html` — Dashboard Tab

---

### 2.2 多媒体消息处理

**目标：** 支持收发图片、文件、位置、联系人等类型。

**具体任务：**
- [ ] 接收用户发来的图片/文件 → 通过 `file_id` 下载保存到本地或 OSS
- [ ] `/location` 命令 — 发送地理位置（`sendLocation`）
- [ ] `/contact` 命令 — 发送联系人卡片（`sendContact`）
- [ ] `/sticker` 命令 — 发送贴纸（`sendSticker`）
- [ ] `/document` 命令 — 发送文件/PDF（`sendDocument`）
- [ ] replyType 新增 `location`、`contact`、`voice` 分支

**涉及文件：**
- 修改 `service/TelegramApiService.java` — 新增 API 封装方法
- 修改 `command/DynamicCommandHandler.java` — switch 新增 case
- 修改 `resources/static/admin.html` — 下拉选项 & 字段

---

### ✅ 2.3 群组管理命令

**目标：** Bot 作为群管理员时，提供实用的管理工具。

**已实现命令（内置处理器，含群聊环境 + 管理员权限校验）：**

| 命令 | 功能 | Telegram API |
|------|------|-------------|
| `/pin [message_id]` | 置顶消息（缺省作用于触发消息） | `PinChatMessage` |
| `/unpin [message_id]` | 取消置顶 | `UnpinChatMessage` |
| `/ban <user_id> [天数]` | 封禁成员 | `BanChatMember` |
| `/unban <user_id>` | 解封成员 | `UnbanChatMember` |
| `/kick <user_id>` | 移出/禁言成员 | `RestrictChatMember`(全禁用权限) |
| `/setdesc <text>` | 修改群描述 | `SetChatDescription` |
| `/settitle <text>` | 修改群名称 | `SetChatTitle` |

**具体任务：**
- [x] 新建 `command/impl/group/` 子包存放群管命令处理器
- [x] 群管命令校验：操作者必须是群管理员/创建者（`GetChatMember` 优先，DB 角色降级）
- [x] `TelegramApiService` 封装对应 API 调用 + `executeBool` 布尔结果处理
- [x] 内置处理器注册（不走 DB 动态配置，涉及权限校验）
- [x] `CommandContext` 新增 `messageId` 字段供 `/pin` 默认使用

**涉及文件：**
- 新建 `command/impl/group/AbstractGroupAdminHandler.java` ✅ (基类：参数解析+权限校验)
- 新建 `command/impl/group/{Pin,Unpin,Ban,Unban,Kick,SetDesc,SetTitle}CommandHandler.java` ✅
- 修改 `service/TelegramApiService.java` ✅ (群管 API 封装 + getChatMemberStatus)
- 修改 `command/CommandContext.java` ✅ (新增 messageId)
- 修改 `bot/TelegramBot.java` ✅ (传递 messageId)

---

### 2.4 Webhook 模式（生产环境优化）

**目标：** 用 Webhook 替代长轮询，降低延迟、提升吞吐。

**技术方案：**
```
Telegram Server ──HTTPS POST──► /telegram/webhook ──► TelegramBot.processUpdate()
```

**具体任务：**
- [ ] 新建 `WebhookController`，`@PostMapping("/telegram/webhook")` 接收 Update JSON
- [ ] 配置 `webhookUrl`（需 HTTPS 域名或内网穿透 frp/ngrok）
- [ ] 启动时调用 `setWebhook`，关闭时调用 `deleteWebhook`
- [ ] 添加 `secretToken` 验证请求来源合法性
- [ ] 保留长轮询作为 fallback（配置开关 `telegram.mode=webhook|polling`）

**涉及文件：**
- 新建 `controller/WebhookController.java`
- 修改 `config/TelegramBotConfig.java` — 条件化启动 LongPolling vs Webhook
- 修改 `application.properties` — 新增 mode / webhook-url / secret-token 配置

---

## Phase 3 — 产品化扩展

### 3.1 Telegram Mini App (WebApp)

**目标：** 在 Bot 对话中嵌入 WebView 页面，构建复杂交互界面。

**适用场景：**
- 表单填写（报名、问卷）
- 商品浏览与下单
- 数据可视化大屏
- 会员积分商城

**技术要点：**
- 前端使用 React/Vue + Telegram Web Apps SDK
- 按钮 `callbackData` 改为 `webApp` 类型打开页面
- 通过 `WebAppData` 回传数据给 Bot 后端
- 复用现有 Controller API 做数据交互

**具体任务：**
- [ ] 新建前端项目 `telegram-webapp/`（Vite + Vue3 或 React）
- [ ] `replyMarkup` JSON 支持 `{"text": "打开商城", "webApp": {"url": "https://..."}}`
- [ ] 后端新增 `/api/webapp/initData` 验证签名
- [ ] 部署前端到 CDN 或 Nginx

---

### 3.2 深度链接 & 邀请追踪

**目标：** 生成带参数的分享链接，追踪用户来源渠道。

**格式：** `t.me/yourbot?start=ref_abc123`

**具体任务：**
- [ ] `StartCommandHandler` 解析 `start` 参数（`ref_xxx`、`channel_xxx`）
- [ ] 新建 `cs_bot_invitation_log` 表记录邀请关系
- [ ] 用户首次 `/start` 时绑定邀请人，可设置奖励机制
- [ ] admin.html 生成推广链接 + 查看邀请排行榜

---

### 3.3 RSS / 爬虫推送聚合

**目标：** 定时抓取外部信息源，推送到指定群组。

**具体任务：**
- [ ] 集成 Rome RSS 解析库或使用 JSOUP 爬取
- [ ] 新建 `cs_bot_feed_source` 表（URL、类型、目标群、抓取频率）
- [ ] `BroadcastTaskRunner` 扩展支持 feed 类型任务
- [ ] 去重：已推送的新闻 URL 不重复发送（Redis Set 或 BloomFilter）

---

### 3.4 分布式高可用改造

**目标：** 支持多实例部署，应对大流量场景。

**具体任务：**
- [ ] 限流组件迁移到 Redis（`INCR + EXPIRE` 滑动窗口）
- [ ] 命令注册表刷新改用 Redis Pub/Sub 通知各实例
- [ ] 广播任务调度引入分布式锁（ShedLock），防止多实例重复执行
- [ ] 消息处理引入 RabbitMQ/Kafka 削峰填谷
- [ ] 用户会话上下文存 Redis（支持多轮对话跨实例共享）

---

## 快速索引：按文件查找待办

| 文件 | 涉及任务编号 |
|------|------------|
| `service/ai/` | 1.1 |
| `entity/TelegramUserEntity.java` (新建) | 1.2 |
| `command/TemplateRenderer.java` (新建) | 1.3 |
| `controller/StatsController.java` (新建) | 2.1 |
| `service/TelegramApiService.java` | 2.2, 2.3 |
| `command/impl/group/` (新建) | 2.3 |
| `controller/WebhookController.java` (新建) | 2.4 |
| `resources/static/admin.html` | 1.2, 1.3, 2.1, 3.2 |
| `resources/db/schema.sql` | 1.1, 1.2, 2.1, 3.2, 3.3 |
| `application.properties` | 1.1, 2.4 |
