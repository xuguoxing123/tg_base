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
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    visible_user_ids JSON COMMENT '可见用户ID白名单,null=全部',
    sort_order      INT DEFAULT 0,
    gmt_create      DATETIME DEFAULT CURRENT_TIMESTAMP,
    gmt_modified    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0
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
