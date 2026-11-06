package com.example.telegram.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 userId 维护最近 N 轮对话历史，为 LLM 提供多轮上下文。
 * <p>
 * 使用内存双端队列实现（单机部署足够），后续分布式改造时可替换为 Redis 存储。
 */
@Component
public class ChatMemoryManager {

    private static final Logger log = LoggerFactory.getLogger(ChatMemoryManager.class);

    /** 每个用户保留的最大对话轮数（1 轮 = 1 user + 1 assistant） */
    private final int maxRounds;

    /** userId → 对话消息队列（按时间正序） */
    private final Map<Long, Deque<Message>> histories = new ConcurrentHashMap<>();

    /**
     * @param maxRounds 每个用户保留最近多少轮对话，默认 10
     */
    public ChatMemoryManager(@Value("${telegram.ai.chat-memory.max-rounds:10}") int maxRounds) {
        this.maxRounds = Math.max(1, maxRounds);
        log.info("ChatMemoryManager 初始化: maxRounds={}", this.maxRounds);
    }

    /**
     * 添加一对用户消息和助手回复到历史。
     */
    public void addRound(long userId, String userText, String assistantText) {
        Deque<Message> deque = histories.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(new Message(Role.USER, userText));
            deque.addLast(new Message(Role.ASSISTANT, assistantText));
            // 超出容量时移除最早的一对
            while (deque.size() > maxRounds * 2) {
                deque.removeFirst();
            }
        }
    }

    /**
     * 获取某用户的对话历史快照（用于构造 LLM prompt）。
     */
    public Message[] getHistory(long userId) {
        Deque<Message> deque = histories.get(userId);
        if (deque == null) return new Message[0];
        synchronized (deque) {
            return deque.toArray(new Message[0]);
        }
    }

    /**
     * 清除某用户的对话历史（可用于 /reset 命令）。
     */
    public void clear(long userId) {
        histories.remove(userId);
    }

    /** 当前缓存的用户数量 */
    public int activeUsers() {
        return histories.size();
    }

    // --- 内部类型 ---

    public enum Role { USER, ASSISTANT }

    public record Message(Role role, String content) {}
}
