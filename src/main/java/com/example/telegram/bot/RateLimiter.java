package com.example.telegram.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 每用户滑动窗口限流器（纯内存实现，适用于单实例部署）。
 * 以 userId 为维度统计最近 windowMillis 内的请求数，超过 maxRequests 则拒绝。
 * 多实例/需要持久化的场景应替换为 Redis + Lua 令牌桶。
 */
@Component
public class RateLimiter {

    private final int maxRequests;
    private final long windowMillis;

    /** userId -> 该用户最近请求的时间戳队列（毫秒，单调递增） */
    private final Map<Long, Deque<Long>> windows = new ConcurrentHashMap<>();

    public RateLimiter(
            @Value("${telegram.rate-limit.max-requests:20}") int maxRequests,
            @Value("${telegram.rate-limit.window-millis:10000}") long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    /**
     * 尝试为指定用户放行一次请求。
     *
     * @return true 表示未超限可继续处理；false 表示已触发限流应丢弃
     */
    public boolean tryAcquire(long userId) {
        long now = System.currentTimeMillis();
        Deque<Long> deque = windows.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (deque) {
            // 移除滑动窗口之外的过期时间戳
            long earliest = now - windowMillis;
            while (!deque.isEmpty() && deque.peekFirst() < earliest) {
                deque.pollFirst();
            }
            if (deque.size() >= maxRequests) {
                return false;
            }
            deque.offerLast(now);
            return true;
        }
    }

    /**
     * 清理长时间不活跃的用户窗口，避免内存无限增长。可由定时任务调用。
     */
    public void evictIdle() {
        long threshold = System.currentTimeMillis() - windowMillis * 10;
        windows.entrySet().removeIf(entry -> {
            Deque<Long> deque = entry.getValue();
            synchronized (deque) {
                return deque.isEmpty() || deque.peekLast() < threshold;
            }
        });
    }
}
