package com.example.telegram.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.telegram.dto.CommandCreateRequest;
import com.example.telegram.dto.CommandUpdateRequest;
import com.example.telegram.dto.CommandVO;
import com.example.telegram.service.BotCommandService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/commands")
public class BotCommandController {

    private final BotCommandService service;

    public BotCommandController(BotCommandService service) {
        this.service = service;
    }

    @GetMapping
    public Page<CommandVO> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.list(page, size);
    }

    @PostMapping
    public ResponseEntity<Map<String, Long>> create(@RequestBody CommandCreateRequest req) {
        Long id = service.create(req);
        return ResponseEntity.ok(Map.of("id", id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody CommandUpdateRequest req) {
        // 确保 path id 和 body id 一致
        service.update(new CommandUpdateRequest(id, req.name(), req.displayName(), req.description(),
                req.replyType(), req.replyContent(), req.replyMarkup(), req.status()));
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

    @PostMapping("/reload")
    public ResponseEntity<Map<String, String>> reload() {
        service.reload();
        return ResponseEntity.ok(Map.of("result", "ok"));
    }
}
