package com.example.telegram.command;

import com.example.telegram.service.TelegramApiService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 通用动态命令处理器：根据 CommandDefinition 的 replyType 分派不同的发送逻辑。
 * 运营在管理后台新增/修改指令后由 CommandRegistry 刷新实例化，无需编写 Java 代码。
 */
public class DynamicCommandHandler implements BotCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(DynamicCommandHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TelegramApiService apiService;
    private final CommandDefinition definition;

    public DynamicCommandHandler(TelegramApiService apiService, CommandDefinition definition) {
        this.apiService = apiService;
        this.definition = definition;
    }

    @Override
    public String name() {
        return definition.name();
    }

    @Override
    public boolean handle(CommandContext context) {
        String parseMode = definition.parseMode();
        // 渲染模板变量（{{date}}、{{time}}、{{random:1-100}} 等）
        String content = TemplateRenderer.render(definition.replyContent(), context);
        return switch (definition.replyType()) {
            case "text" -> apiService.sendText(context.chatId(), content, parseMode).isPresent();
            case "photo" -> apiService.sendPhoto(context.chatId(), content, content).isPresent();
            case "buttons" -> handleButtons(context, parseMode, content);
            case "keyboard" -> handleKeyboard(context, parseMode, content);
            default -> {
                log.warn("未知的 replyType: {}, command: {}", definition.replyType(), definition.name());
                yield false;
            }
        };
    }

    private boolean handleButtons(CommandContext context, String parseMode, String content) {
        try {
            InlineKeyboardMarkup markup = parseMarkup(definition.replyMarkup());
            return apiService.sendText(context.chatId(), content, parseMode, markup).isPresent();
        } catch (Exception e) {
            log.error("解析按钮配置失败, command: {}", definition.name(), e);
            return false;
        }
    }

    /**
     * 处理 ReplyKeyboard 自定义键盘类型。
     * 用户点击按钮后，按钮文字会作为普通消息发给机器人（可被 CommandRegistry 路由）。
     *
     * JSON 格式: [["北京","上海"],["广州","深圳"]]
     */
    private boolean handleKeyboard(CommandContext context, String parseMode, String content) {
        try {
            ReplyKeyboardMarkup keyboard = parseReplyKeyboard(definition.replyMarkup());
            return apiService.sendTextWithKeyboard(context.chatId(), content, parseMode, keyboard).isPresent();
        } catch (Exception e) {
            log.error("解析键盘配置失败, command: {}", definition.name(), e);
            return false;
        }
    }

    /**
     * 将 JSON 字符串解析为 ReplyKeyboard。
     * 格式: [["按钮1","按钮2"],["按钮3","按钮4"]]
     */
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

    /**
     * 将 JSON 字符串解析为 InlineKeyboardMarkup。
     * 格式: [[{"text":"确认","callbackData":"cmd_yes"},{"text":"取消","callbackData":"cmd_no"}]]
     */
    private InlineKeyboardMarkup parseMarkup(String json) throws Exception {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("replyMarkup is empty for buttons type");
        }
        List<List<Map<String, String>>> rows = MAPPER.readValue(json,
                new TypeReference<>() {});
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        for (List<Map<String, String>> rowDef : rows) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (Map<String, String> btnDef : rowDef) {
                InlineKeyboardButton btn = new InlineKeyboardButton();
                btn.setText(btnDef.getOrDefault("text", "???"));
                if (btnDef.containsKey("callbackData")) {
                    btn.setCallbackData(btnDef.get("callbackData"));
                }
                if (btnDef.containsKey("url")) {
                    btn.setUrl(btnDef.get("url"));
                }
                row.add(btn);
            }
            keyboard.add(row);
        }
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);
        return markup;
    }
}
