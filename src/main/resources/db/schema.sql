-- Phase 1: 动态命令配置表
-- 在 MySQL 中先手动执行此脚本建库建表

CREATE DATABASE IF NOT EXISTS telegram_bot DEFAULT CHARSET utf8mb4;
USE telegram_bot;

CREATE TABLE IF NOT EXISTS cs_bot_command (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(64) NOT NULL UNIQUE COMMENT '命令名(不带/)',
    display_name    VARCHAR(128) COMMENT '菜单显示名',
    description     VARCHAR(512) COMMENT '说明文字',
    reply_type      VARCHAR(16) NOT NULL DEFAULT 'text' COMMENT 'text/photo/buttons',
    reply_content   TEXT COMMENT '文本内容或图片URL',
    reply_markup    JSON COMMENT '按钮配置二维数组',
    parse_mode      VARCHAR(16) DEFAULT NULL COMMENT '文本格式化: MarkdownV2/HTML/null(纯文本)',
    trigger_keywords VARCHAR(512) DEFAULT NULL COMMENT '逗号分隔触发关键词，非命令文本匹配时自动回复',
    welcome_text    TEXT DEFAULT NULL COMMENT '入群欢迎语（仅群聊生效）',
    system_prompt   TEXT DEFAULT NULL COMMENT 'AI 兜底时该命令对应的系统提示词（覆盖全局配置）',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    visible_user_ids JSON COMMENT '可见用户ID白名单,null=全部',
    sort_order      INT DEFAULT 0,
    gmt_create      DATETIME DEFAULT CURRENT_TIMESTAMP,
    gmt_modified    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Phase 3: 用户管理表
-- 记录与 Bot 交互过的 Telegram 用户，支持角色分级 + 黑名单封禁
CREATE TABLE IF NOT EXISTS cs_telegram_user (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id          BIGINT NOT NULL UNIQUE COMMENT 'Telegram 用户ID（全局唯一且永不变）',
    username         VARCHAR(64) COMMENT '@username',
    first_name       VARCHAR(64),
    last_name        VARCHAR(64),
    role             VARCHAR(16) DEFAULT 'USER' COMMENT 'ADMIN / USER',
    status           TINYINT DEFAULT 1 COMMENT '1=正常 0=黑名单',
    first_seen_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '首次交互时间',
    last_active_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近活跃时间',
    message_count    INT DEFAULT 0 COMMENT '累计消息数',
    deleted          TINYINT DEFAULT 0,
    gmt_create       DATETIME DEFAULT CURRENT_TIMESTAMP,
    gmt_modified     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Phase 2: 定时任务配置表
-- 运营配置“目标群 + cron 表达式 + 回复内容”，调度器按周期自动推送
CREATE TABLE IF NOT EXISTS cs_bot_broadcast_task (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(64) NOT NULL COMMENT '任务唯一标识',
    description     VARCHAR(512) COMMENT '任务说明',
    chat_id         BIGINT NOT NULL COMMENT '目标会话/群 ID（定时推送至此）',
    cron_expression VARCHAR(64) NOT NULL COMMENT 'Spring cron 表达式（6 位：秒 分 时 日 月 周）',
    reply_type      VARCHAR(16) NOT NULL DEFAULT 'text' COMMENT 'text/photo/buttons/keyboard',
    reply_content   TEXT COMMENT '文本内容或图片 URL',
    reply_markup    TEXT COMMENT '按钮/键盘 JSON 配置',
    parse_mode      VARCHAR(16) DEFAULT NULL COMMENT 'MarkdownV2/HTML/null',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    last_exec_time  DATETIME DEFAULT NULL COMMENT '上次执行时间',
    next_exec_time  DATETIME DEFAULT NULL COMMENT '下次预计执行时间',
    gmt_create      DATETIME DEFAULT CURRENT_TIMESTAMP,
    gmt_modified    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Phase 2.1: 消息日志表（数据统计用）
-- 记录每条经过命令分发的消息，用于统计面板图表渲染
CREATE TABLE IF NOT EXISTS cs_bot_message_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL COMMENT 'Telegram 用户ID',
    chat_id         BIGINT NOT NULL COMMENT '会话/群 ID',
    command_name    VARCHAR(64) DEFAULT NULL COMMENT '命令名或关键词，非命令消息为 NULL',
    handler_type    VARCHAR(32) DEFAULT NULL COMMENT '处理器类型简名',
    success         TINYINT DEFAULT 1 COMMENT '处理结果 1=成功 0=失败',
    gmt_create      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '消息时间戳',
    INDEX idx_user_date (user_id, gmt_create),
    INDEX idx_command (command_name),
    INDEX idx_hourly (gmt_create)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
