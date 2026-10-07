package com.example.telegram.command;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.telegram.entity.BotCommandEntity;
import com.example.telegram.mapper.BotCommandMapper;
import com.example.telegram.service.TelegramApiService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 命令注册表：合并内置处理器与 DB 动态命令，volatile 快照保证线程安全。
 * 定时从 DB 刷新 + Admin API 可主动触发 reload。
 */
@Component
public class CommandRegistry {

    private static final Logger log = LoggerFactory.getLogger(CommandRegistry.class);

    private final List<BotCommandHandler> builtInHandlers;
    private final TelegramApiService apiService;
    private final BotCommandMapper commandMapper;

    /** volatile 快照：读侧无锁，写侧整体替换 */
    private volatile Map<String, BotCommandHandler> snapshot = Map.of();
    private volatile BotCommandHandler fallback;

    public CommandRegistry(List<BotCommandHandler> builtInHandlers,
                           TelegramApiService apiService,
                           BotCommandMapper commandMapper) {
        this.builtInHandlers = builtInHandlers;
        this.apiService = apiService;
        this.commandMapper = commandMapper;
    }

    @PostConstruct
    public void init() {
        refresh();
    }

    /**
     * 每 30 秒自动从 DB 刷新动态命令配置。
     */
    @Scheduled(fixedDelayString = "${telegram.command-refresh-interval:30000}")
    public void scheduledRefresh() {
        refresh();
    }

    /**
     * 重建命令快照：先放内置（排除空 name 的 fallback），再用 DB 动态命令覆盖同名项。
     */
    public synchronized void refresh() {
        try {
            Map<String, BotCommandHandler> next = new HashMap<>();

            // 1. 内置处理器
            for (BotCommandHandler h : builtInHandlers) {
                if (h.name().isEmpty()) {
                    this.fallback = h;
                } else {
                    next.put(h.name(), h);
                }
            }

            // 2. DB 动态命令（同名覆盖内置 → 运营可以不改代码就替换 /help 等行为）
            List<BotCommandEntity> entities = commandMapper.selectList(
                    new LambdaQueryWrapper<BotCommandEntity>()
                            .eq(BotCommandEntity::getStatus, 1));
            for (BotCommandEntity e : entities) {
                CommandDefinition def = new CommandDefinition(
                        e.getName(),
                        e.getReplyType(),
                        e.getReplyContent(),
                        e.getReplyMarkup());
                next.put(def.name(), new DynamicCommandHandler(apiService, def));
            }

            this.snapshot = Map.copyOf(next);
            log.debug("CommandRegistry 刷新完成: {} 条命令 {}", snapshot.size(), snapshot.keySet());
        } catch (Exception e) {
            log.error("CommandRegistry 刷新失败，保持旧快照不变", e);
        }
    }

    public Optional<BotCommandHandler> lookup(String name) {
        return Optional.ofNullable(snapshot.get(name));
    }

    public BotCommandHandler getFallback() {
        return fallback;
    }

    public int size() {
        return snapshot.size();
    }
}
