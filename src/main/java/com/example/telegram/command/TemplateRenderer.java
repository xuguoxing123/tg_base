package com.example.telegram.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 消息模板变量渲染引擎。
 * <p>
 * 将文本中的 {@code {{variable}}} 占位符替换为运行时动态值。
 * 支持的变量：
 * <ul>
 *   <li>{@code {{username}}} — 用户 @username（无则用 firstName）</li>
 *   <li>{@code {{first_name}}} — 用户名字</li>
 *   <li>{@code {{chat_title}}} — 群聊标题（私聊时为 "私聊"）</li>
 *   <li>{@code {{date}}} — 当前日期 yyyy-MM-dd</li>
 *   <li>{@code {{time}}} — 当前时间 HH:mm:ss</li>
 *   <li>{@code {{random:min-max}}} — 范围内随机整数</li>
 * </ul>
 * <p>
 * 同时兼容旧的单花括号语法（{@code {user}}、{@code {chat}}），用于入群欢迎语。
 */
public final class TemplateRenderer {

    private static final Logger log = LoggerFactory.getLogger(TemplateRenderer.class);

    /** 匹配 {{variable}} 或 {{random:min-max}} */
    private static final Pattern PATTERN = Pattern.compile("\\{\\{(?<key>[a-zA-Z_]+)(?::(?<arg>[^}]*))?}}");

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private TemplateRenderer() {} // 工具类不可实例化

    /**
     * 渲染模板：将所有 {{xxx}} 占位符替换为实际值。
     *
     * @param template  原始模板文本
     * @param context   命令上下文（含 userId、arguments 等）
     * @return 渲染后的文本；template 为 null 时返回 null
     */
    public static String render(String template, CommandContext context) {
        return render(template, context, null);
    }

    /**
     * 渲染模板（带额外变量）。
     *
     * @param template   原始模板文本
     * @param context    命令上下文
     * @param extraVars  额外键值对（优先级高于内置变量），可为 null
     * @return 渲染后的文本
     */
    public static String render(String template, CommandContext context, java.util.Map<String, String> extraVars) {
        if (template == null || template.isEmpty()) return template;
        if (!template.contains("{{")) return template; // 快速路径：无模板变量则直接返回

        Matcher matcher = PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group("key");
            String arg = matcher.group("arg");
            String replacement = resolveVariable(key, arg, context, extraVars);
            // 对 $ 和 \ 进行转义，避免 appendReplacement 误解析
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 渲染欢迎语文本（不依赖 CommandContext，仅使用基本参数）。
     * 兼容旧语法 {user} / {chat} 以及新语法 {{username}} / {{chat_title}} 等。
     */
    public static String renderWelcome(String template, String userDisplay, String chatTitle) {
        if (template == null || template.isEmpty()) return template;

        String result = template;
        // 旧语法兼容
        result = result.replace("{user}", userDisplay != null ? userDisplay : "");
        result = result.replace("{chat}", chatTitle != null ? chatTitle : "");

        // 新语法（无需 CommandContext，构建一个临时的）
        if (result.contains("{{")) {
            CommandContext fakeCtx = new CommandContext(0, 0, "", "USER", false);
            java.util.Map<String, String> extras = new java.util.HashMap<>();
            extras.put("username", userDisplay);
            extras.put("first_name", userDisplay);
            extras.put("chat_title", chatTitle);
            result = render(result, fakeCtx, extras);
        }
        return result;
    }

    private static String resolveVariable(String key, String arg, CommandContext ctx, java.util.Map<String, String> extras) {
        // 额外变量优先
        if (extras != null && extras.containsKey(key)) {
            return extras.get(key);
        }

        switch (key) {
            case "username":
                // 从 arguments 中无法获取 username，暂用 userId 代替
                return "用户" + ctx.userId();
            case "first_name":
                return "用户" + ctx.userId();
            case "chat_title":
                return "会话" + ctx.chatId();
            case "date":
                return LocalDate.now().format(DATE_FMT);
            case "time":
                return LocalTime.now().format(TIME_FMT);
            case "random":
                return resolveRandom(arg);
            default:
                log.debug("未知模板变量: {{{}}}", key);
                return "{{" + key + (arg != null ? ":" + arg : "") + "}}";
        }
    }

    private static String resolveRandom(String arg) {
        if (arg == null || arg.isBlank()) {
            return String.valueOf(ThreadLocalRandom.current().nextInt(0, 10000));
        }
        try {
            String[] parts = arg.split("-");
            int min = Integer.parseInt(parts[0].trim());
            int max = Integer.parseInt(parts[1].trim());
            if (min > max) { int tmp = min; min = max; max = tmp; }
            return String.valueOf(ThreadLocalRandom.current().nextInt(min, max + 1));
        } catch (Exception e) {
            log.warn("random 参数解析失败: arg='{}'", arg);
            return "{{random:" + arg + "}}";
        }
    }
}
