package com.example.telegram.controller;

import com.example.telegram.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据统计 API，供 admin.html Dashboard Tab 调用。
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /** 命令使用频率 Top N（默认 10） */
    @GetMapping("/commands")
    public ResponseEntity<List<Map<String, Object>>> topCommands(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(statsService.getTopCommands(limit));
    }

    /** 日活用户趋势（默认最近 30 天） */
    @GetMapping("/daily-active")
    public ResponseEntity<List<Map<String, Object>>> dailyActive(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(statsService.getDailyActiveTrend(days));
    }

    /** 每小时消息量分布（今天） */
    @GetMapping("/hourly-volume")
    public ResponseEntity<List<Map<String, Object>>> hourlyVolume() {
        return ResponseEntity.ok(statsService.getHourlyVolume());
    }

    /** 广播任务执行成功率 */
    @GetMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> broadcastStats() {
        return ResponseEntity.ok(statsService.getBroadcastStats());
    }
}
