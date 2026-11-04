package com.example.telegram.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.telegram.dto.BroadcastTaskCreateRequest;
import com.example.telegram.dto.BroadcastTaskUpdateRequest;
import com.example.telegram.dto.BroadcastTaskVO;
import com.example.telegram.service.BroadcastTaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 广播任务管理 REST API。
 */
@RestController
@RequestMapping("/api/broadcast-tasks")
public class BroadcastTaskController {

    private final BroadcastTaskService service;

    public BroadcastTaskController(BroadcastTaskService service) {
        this.service = service;
    }

    @GetMapping
    public Page<BroadcastTaskVO> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.list(page, size);
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> create(@RequestBody BroadcastTaskCreateRequest req) {
        Long id = service.create(req);
        return ResponseEntity.ok(Map.of("id", id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody BroadcastTaskUpdateRequest req) {
        service.update(new BroadcastTaskUpdateRequest(id, req.name(), req.description(), req.chatId(),
                req.cronExpression(), req.replyType(), req.replyContent(), req.replyMarkup(),
                req.parseMode(), req.status()));
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        service.toggleStatus(id, body.get("status"));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
