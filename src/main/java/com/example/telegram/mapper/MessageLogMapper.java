package com.example.telegram.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.telegram.entity.MessageLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface MessageLogMapper extends BaseMapper<MessageLogEntity> {

    /**
     * 命令使用频率 Top N（最近 7 天）。
     */
    @Select("SELECT command_name AS name, COUNT(*) AS count " +
            "FROM cs_bot_message_log " +
            "WHERE gmt_create >= DATE_SUB(NOW(), INTERVAL 7 DAY) AND command_name IS NOT NULL " +
            "GROUP BY command_name ORDER BY count DESC LIMIT #{limit}")
    List<Map<String, Object>> topCommands(int limit);

    /**
     * 日活用户趋势（最近 N 天，每天独立 userId 数）。
     */
    @Select("SELECT DATE(gmt_create) AS date, COUNT(DISTINCT user_id) AS activeUsers " +
            "FROM cs_bot_message_log " +
            "WHERE gmt_create >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(gmt_create) ORDER BY date")
    List<Map<String, Object>> dailyActiveTrend(int days);

    /**
     * 每小时消息量分布（今天 0-23 时）。
     */
    @Select("SELECT HOUR(gmt_create) AS hour, COUNT(*) AS count " +
            "FROM cs_bot_message_log " +
            "WHERE DATE(gmt_create) = CURDATE() " +
            "GROUP BY HOUR(gmt_create) ORDER BY hour")
    List<Map<String, Object>> hourlyVolume();

    /**
     * 广播任务执行成功率。
     */
    @Select("SELECT handler_type AS type, " +
            "SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) AS ok, " +
            "COUNT(*) AS total " +
            "FROM cs_bot_message_log " +
            "WHERE handler_type = 'BroadcastTaskRunner' " +
            "GROUP BY handler_type")
    Map<String, Object> broadcastStats();
}
