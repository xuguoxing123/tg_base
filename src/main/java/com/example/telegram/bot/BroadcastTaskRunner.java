package com.example.telegram.bot;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.telegram.entity.BroadcastTaskEntity;
import com.example.telegram.mapper.BroadcastTaskMapper;
import com.example.telegram.service.TelegramApiService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 广播任务调度器：每分钟扫描启用的任务，若 cron 表达式在上一分钟窗口内命中则触发推送。
 * <p>
 * 采用「数据库 + 分钟轮询」而非动态注册 @Scheduled，好处是运营改配置即时生效、
 * 无需重启也无需为每条任务维护 Trigger，与现有 CommandRegistry 的轮询刷新思路一致。
 */
@Component
public class BroadcastTaskRunner {

    private static final Logger log = LoggerFactory.getLogger(BroadcastTaskRunner.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 轮询窗口：cron 命中点落在 [now-1min, now) 内即视为本周期应执行 */
    private static final long POLL_WINDOW_MINUTES = 1;

    private final BroadcastTaskMapper taskMapper;
    private final TelegramApiService apiService;

    public BroadcastTaskRunner(BroadcastTaskMapper taskMapper, TelegramApiService apiService) {
        this.taskMapper = taskMapper;
        this.apiService = apiService;
    }

    /**
     * 每分钟的第 0 秒执行一次扫描。
     */
    @Scheduled(cron = "${telegram.scheduled-task.scan-cron:0 * * * * *}")
    public void scan() {
        List<BroadcastTaskEntity> tasks;
        try {
            tasks = taskMapper.selectList(
                    new LambdaQueryWrapper<BroadcastTaskEntity>()
                            .eq(BroadcastTaskEntity::getStatus, 1));
        } catch (Exception e) {
            log.error("广播任务扫描失败：读取 DB 异常", e);
            return;
        }

        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        for (BroadcastTaskEntity task : tasks) {
            try {
                if (shouldFire(task.getCronExpression(), now)) {
                    fire(task, now);
                }
            } catch (Exception e) {
                log.error("广播任务执行异常: id={}, name={}", task.getId(), task.getName(), e);
            }
        }
    }

    /**
     * 判断 cron 是否在当前分钟窗口内有命中点。
     * 取「now 往前推一个窗口」作为基准，计算下一个触发时间，若落在 (base, now] 内则命中。
     */
    private boolean shouldFire(String cron, LocalDateTime now) {
        CronExpression expression;
        try {
            expression = CronExpression.parse(cron);
        } catch (IllegalArgumentException e) {
            log.warn("非法 cron 表达式，跳过: {}", cron);
            return false;
        }
        LocalDateTime base = now.minusMinutes(POLL_WINDOW_MINUTES);
        LocalDateTime next = expression.next(base);
        // next 存在且不晚于当前整分钟 → 本窗口应触发
        return next != null && !next.isAfter(now);
    }

    /**
     * 触发一次任务推送并更新执行时间戳。
     */
    private void fire(BroadcastTaskEntity task, LocalDateTime now) {
        log.info("广播任务触发: id={}, name={}, chatId={}, type={}",
                task.getId(), task.getName(), task.getChatId(), task.getReplyType());
        boolean success = dispatch(task);
        task.setLastExecTime(success ? now : task.getLastExecTime());
        task.setNextExecTime(computeNext(task.getCronExpression(), now));
        try {
            taskMapper.updateById(task);
        } catch (Exception e) {
            log.error("更新广播任务执行状态失败: id={}", task.getId(), e);
        }
    }

    /**
     * 按 replyType 分派发送，逻辑与 DynamicCommandHandler 保持一致。
     */
    private boolean dispatch(BroadcastTaskEntity task) {
        long chatId = task.getChatId();
        String parseMode = task.getParseMode();
        try {
            return switch (task.getReplyType() == null ? "text" : task.getReplyType()) {
                case "photo" -> apiService.sendPhoto(chatId, task.getReplyContent(), task.getReplyContent()).isPresent();
                case "buttons" -> apiService.sendText(chatId, task.getReplyContent(), parseMode,
                        parseInlineKeyboard(task.getReplyMarkup())).isPresent();
                case "keyboard" -> apiService.sendTextWithKeyboard(chatId, task.getReplyContent(), parseMode,
                        parseReplyKeyboard(task.getReplyMarkup())).isPresent();
                default -> apiService.sendText(chatId, task.getReplyContent(), parseMode).isPresent();
            };
        } catch (Exception e) {
            log.error("广播任务发送失败: id={}, name={}", task.getId(), task.getName(), e);
            return false;
        }
    }

    private LocalDateTime computeNext(String cron, LocalDateTime from) {
        try {
            return CronExpression.parse(cron).next(from);
        } catch (Exception e) {
            return null;
        }
    }

    private InlineKeyboardMarkup parseInlineKeyboard(String json) throws Exception {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("replyMarkup is empty for buttons type");
        }
        List<List<Map<String, String>>> rows = MAPPER.readValue(json, new TypeReference<>() {});
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        for (List<Map<String, String>> rowDef : rows) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (Map<String, String> btnDef : rowDef) {
                InlineKeyboardButton btn = new InlineKeyboardButton();
                btn.setText(btnDef.getOrDefault("text", "???"));
                if (btnDef.containsKey("callbackData")) btn.setCallbackData(btnDef.get("callbackData"));
                if (btnDef.containsKey("url")) btn.setUrl(btnDef.get("url"));
                row.add(btn);
            }
            keyboard.add(row);
        }
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        return markup;
    }

    private ReplyKeyboardMarkup parseReplyKeyboard(String json) throws Exception {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("replyMarkup is empty for keyboard type");
        }
        List<List<String>> rows = MAPPER.readValue(json, new TypeReference<>() {});
        List<KeyboardRow> keyboard = new ArrayList<>();
        for (List<String> rowDef : rows) {
            KeyboardRow row = new KeyboardRow();
            for (String btnText : rowDef) {
                KeyboardButton btn = new KeyboardButton();
                btn.setText(btnText);
                row.add(btn);
            }
            keyboard.add(row);
        }
        ReplyKeyboardMarkup rkm = new ReplyKeyboardMarkup();
        rkm.setKeyboard(keyboard);
        rkm.setResizeKeyboard(true);
        rkm.setOneTimeKeyboard(false);
        return rkm;
    }
}
