package com.example.telegram.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.telegram.dto.TelegramUserVO;
import com.example.telegram.entity.TelegramUserEntity;
import com.example.telegram.service.TelegramUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户管理 REST API，供 admin.html 调用。
 */
@RestController
@RequestMapping("/api/users")
public class TelegramUserController {

    private final TelegramUserService userService;

    public TelegramUserController(TelegramUserService userService) {
        this.userService = userService;
    }

    /**
     * 分页查询用户列表，支持按角色和状态筛选。
     */
    @GetMapping
    public ResponseEntity<Page<TelegramUserVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status) {

        Page<TelegramUserEntity> entityPage = userService.list(page, size, role, status);
        Page<TelegramUserVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toVO).toList());
        return ResponseEntity.ok(voPage);
    }

    /**
     * 切换用户角色（ADMIN / USER）。
     */
    @PatchMapping("/{id}/role")
    public ResponseEntity<Void> changeRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.changeRole(id, body.get("role"));
        return ResponseEntity.ok().build();
    }

    /**
     * 封禁 / 解封用户（status: 1=正常, 0=黑名单）。
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        userService.toggleStatus(id, body.get("status"));
        return ResponseEntity.ok().build();
    }

    private TelegramUserVO toVO(TelegramUserEntity e) {
        return new TelegramUserVO(
                e.getId(), e.getUserId(), e.getUsername(),
                e.getFirstName(), e.getLastName(),
                e.getRole(), e.getStatus(),
                e.getFirstSeenTime(), e.getLastActiveTime(),
                e.getMessageCount());
    }
}
