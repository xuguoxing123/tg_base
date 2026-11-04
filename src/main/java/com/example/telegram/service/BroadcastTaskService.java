package com.example.telegram.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.telegram.dto.BroadcastTaskCreateRequest;
import com.example.telegram.dto.BroadcastTaskUpdateRequest;
import com.example.telegram.dto.BroadcastTaskVO;
import com.example.telegram.entity.BroadcastTaskEntity;
import com.example.telegram.mapper.BroadcastTaskMapper;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 广播任务 CRUD。创建/更新时校验 cron 表达式合法性，并计算下次执行时间。
 */
@Service
public class BroadcastTaskService {

    private final BroadcastTaskMapper mapper;

    public BroadcastTaskService(BroadcastTaskMapper mapper) {
        this.mapper = mapper;
    }

    public Page<BroadcastTaskVO> list(int page, int size) {
        Page<BroadcastTaskEntity> entityPage = mapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<BroadcastTaskEntity>().orderByDesc(BroadcastTaskEntity::getId));
        Page<BroadcastTaskVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    public Long create(BroadcastTaskCreateRequest req) {
        validateCron(req.cronExpression());
        BroadcastTaskEntity e = new BroadcastTaskEntity();
        e.setName(req.name());
        e.setDescription(req.description());
        e.setChatId(req.chatId());
        e.setCronExpression(req.cronExpression());
        e.setReplyType(req.replyType() != null ? req.replyType() : "text");
        e.setReplyContent(req.replyContent());
        e.setReplyMarkup(req.replyMarkup());
        e.setParseMode(req.parseMode());
        e.setStatus(req.status() != null ? req.status() : 1);
        e.setNextExecTime(computeNext(req.cronExpression()));
        mapper.insert(e);
        return e.getId();
    }

    public void update(BroadcastTaskUpdateRequest req) {
        validateCron(req.cronExpression());
        BroadcastTaskEntity e = mapper.selectById(req.id());
        if (e == null) throw new IllegalArgumentException("Task not found: id=" + req.id());
        e.setName(req.name());
        e.setDescription(req.description());
        e.setChatId(req.chatId());
        e.setCronExpression(req.cronExpression());
        e.setReplyType(req.replyType());
        e.setReplyContent(req.replyContent());
        e.setReplyMarkup(req.replyMarkup());
        e.setParseMode(req.parseMode());
        if (req.status() != null) e.setStatus(req.status());
        e.setNextExecTime(computeNext(req.cronExpression()));
        mapper.updateById(e);
    }

    public void toggleStatus(Long id, Integer status) {
        BroadcastTaskEntity e = mapper.selectById(id);
        if (e == null) throw new IllegalArgumentException("Task not found: id=" + id);
        e.setStatus(status);
        if (status != null && status == 1) {
            e.setNextExecTime(computeNext(e.getCronExpression()));
        }
        mapper.updateById(e);
    }

    public void delete(Long id) {
        mapper.deleteById(id);
    }

    /**
     * 手动立即触发一次（调试用），不影响 cron 周期。
     */
    public BroadcastTaskEntity getById(Long id) {
        return mapper.selectById(id);
    }

    private void validateCron(String cron) {
        if (cron == null || cron.isBlank()) {
            throw new IllegalArgumentException("cronExpression 不能为空");
        }
        if (!CronExpression.isValidExpression(cron)) {
            throw new IllegalArgumentException("非法的 cron 表达式: " + cron);
        }
    }

    private LocalDateTime computeNext(String cron) {
        try {
            return CronExpression.parse(cron).next(LocalDateTime.now());
        } catch (Exception e) {
            return null;
        }
    }

    private BroadcastTaskVO toVO(BroadcastTaskEntity e) {
        return new BroadcastTaskVO(
                e.getId(), e.getName(), e.getDescription(), e.getChatId(),
                e.getCronExpression(), e.getReplyType(), e.getReplyContent(),
                e.getReplyMarkup(), e.getParseMode(), e.getStatus(),
                e.getLastExecTime(), e.getNextExecTime(),
                e.getGmtCreate(), e.getGmtModified());
    }
}
