package com.example.telegram.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.telegram.command.CommandRegistry;
import com.example.telegram.dto.CommandCreateRequest;
import com.example.telegram.dto.CommandUpdateRequest;
import com.example.telegram.dto.CommandVO;
import com.example.telegram.entity.BotCommandEntity;
import com.example.telegram.mapper.BotCommandMapper;
import org.springframework.stereotype.Service;

@Service
public class BotCommandService {

    private final BotCommandMapper mapper;
    private final CommandRegistry registry;

    public BotCommandService(BotCommandMapper mapper, CommandRegistry registry) {
        this.mapper = mapper;
        this.registry = registry;
    }

    public Page<CommandVO> list(int page, int size) {
        Page<BotCommandEntity> entityPage = mapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<BotCommandEntity>().orderByAsc(BotCommandEntity::getSortOrder));
        Page<CommandVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    public Long create(CommandCreateRequest req) {
        BotCommandEntity e = new BotCommandEntity();
        e.setName(req.name());
        e.setDisplayName(req.displayName());
        e.setDescription(req.description());
        e.setReplyType(req.replyType() != null ? req.replyType() : "text");
        e.setReplyContent(req.replyContent());
        e.setReplyMarkup(req.replyMarkup());
        e.setStatus(req.status() != null ? req.status() : 1);
        mapper.insert(e);
        registry.refresh();
        return e.getId();
    }

    public void update(CommandUpdateRequest req) {
        BotCommandEntity e = mapper.selectById(req.id());
        if (e == null) throw new IllegalArgumentException("Command not found: id=" + req.id());
        e.setName(req.name());
        e.setDisplayName(req.displayName());
        e.setDescription(req.description());
        e.setReplyType(req.replyType());
        e.setReplyContent(req.replyContent());
        e.setReplyMarkup(req.replyMarkup());
        if (req.status() != null) e.setStatus(req.status());
        mapper.updateById(e);
        registry.refresh();
    }

    public void toggleStatus(Long id, Integer status) {
        BotCommandEntity e = mapper.selectById(id);
        if (e == null) throw new IllegalArgumentException("Command not found: id=" + id);
        e.setStatus(status);
        mapper.updateById(e);
        registry.refresh();
    }

    public void delete(Long id) {
        mapper.deleteById(id);
        registry.refresh();
    }

    public void reload() {
        registry.refresh();
    }

    private CommandVO toVO(BotCommandEntity e) {
        return new CommandVO(
                e.getId(), e.getName(), e.getDisplayName(), e.getDescription(),
                e.getReplyType(), e.getReplyContent(), e.getReplyMarkup(),
                e.getStatus(), e.getSortOrder(), e.getGmtCreate(), e.getGmtModified());
    }
}
