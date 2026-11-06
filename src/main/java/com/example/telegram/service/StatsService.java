package com.example.telegram.service;

import com.example.telegram.entity.MessageLogEntity;
import com.example.telegram.mapper.MessageLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 数据统计服务：异步写入消息日志 + 提供聚合查询。
 */
@Service
public class StatsService {

    private static final Logger log = LoggerFactory.getLogger(StatsService.class);

    private final MessageLogMapper mapper;

    public StatsService(MessageLogMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 异步记录一条消息日志，不阻塞主处理流程。
     */
    @Async
    public void logMessage(long userId, long chatId, String commandName, String handlerType, boolean success) {
        try {
            MessageLogEntity entity = new MessageLogEntity();
            entity.setUserId(userId);
            entity.setChatId(chatId);
            entity.setCommandName(commandName);
            entity.setHandlerType(handlerType);
            entity.setSuccess(success ? 1 : 0);
            mapper.insert(entity);
        } catch (Exception e) {
            log.warn("消息日志写入失败（不影响业务）: {}", e.getMessage());
        }
    }

    /** 命令使用频率 Top N */
    public List<Map<String, Object>> getTopCommands(int limit) {
        return mapper.topCommands(limit);
    }

    /** 日活用户趋势 */
    public List<Map<String, Object>> getDailyActiveTrend(int days) {
        return mapper.dailyActiveTrend(days);
    }

    /** 每小时消息量分布 */
    public List<Map<String, Object>> getHourlyVolume() {
        return mapper.hourlyVolume();
    }

    /** 广播任务执行成功率 */
    public Map<String, Object> getBroadcastStats() {
        return mapper.broadcastStats();
    }
}
