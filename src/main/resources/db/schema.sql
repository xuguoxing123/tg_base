-- Phase 1: 动态命令配置表
-- 在 MySQL 中先手动执行此脚本建库建表

CREATE DATABASE IF NOT EXISTS telegram_bot DEFAULT CHARSET utf8mb4;
USE telegram_bot;

CREATE TABLE IF NOT EXISTS bot_command (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(64) NOT NULL UNIQUE COMMENT '命令名(不带/)',
    display_name    VARCHAR(128) COMMENT '菜单显示名',
    description     VARCHAR(512) COMMENT '说明文字',
    reply_type      VARCHAR(16) NOT NULL DEFAULT 'text' COMMENT 'text/photo/buttons',
    reply_content   TEXT COMMENT '文本内容或图片URL',
    reply_markup    JSON COMMENT '按钮配置二维数组',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    visible_user_ids JSON COMMENT '可见用户ID白名单,null=全部',
    sort_order      INT DEFAULT 0,
    gmt_create      DATETIME DEFAULT CURRENT_TIMESTAMP,
    gmt_modified    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
