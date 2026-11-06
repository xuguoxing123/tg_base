package com.example.telegram.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.telegram.entity.TelegramUserEntity;
import com.example.telegram.mapper.TelegramUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Telegram 用户管理服务：负责用户记录 upsert、角色/状态变更、黑名单检查。
 */
@Service
public class TelegramUserService {

    private static final Logger log = LoggerFactory.getLogger(TelegramUserService.class);

    private final TelegramUserMapper mapper;

    public TelegramUserService(TelegramUserMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 每次用户与 Bot 交互时调用，自动创建或更新活跃信息。
     */
    public void recordInteraction(Long userId, String username, String firstName, String lastName) {
        try {
            mapper.upsert(userId, username, firstName, lastName);
        } catch (Exception e) {
            log.error("用户 upsert 失败: userId={}", userId, e);
        }
    }

    /**
     * 检查用户是否在黑名单中（status=0）。
     * 未注册过的用户视为正常。
     */
    public boolean isBlocked(Long userId) {
        TelegramUserEntity user = findByUserId(userId);
        return user != null && user.getStatus() != null && user.getStatus() == 0;
    }

    /**
     * 获取用户角色（默认 USER）。
     */
    public String getRole(Long userId) {
        TelegramUserEntity user = findByUserId(userId);
        return user != null && user.getRole() != null ? user.getRole() : "USER";
    }

    /**
     * 判断是否为管理员。
     */
    public boolean isAdmin(Long userId) {
        return "ADMIN".equalsIgnoreCase(getRole(userId));
    }

    /**
     * 分页查询用户列表。
     */
    public Page<TelegramUserEntity> list(int page, int size, String role, Integer status) {
        LambdaQueryWrapper<TelegramUserEntity> wrapper = new LambdaQueryWrapper<>();
        if (role != null && !role.isBlank()) {
            wrapper.eq(TelegramUserEntity::getRole, role);
        }
        if (status != null) {
            wrapper.eq(TelegramUserEntity::getStatus, status);
        }
        wrapper.orderByDesc(TelegramUserEntity::getLastActiveTime);
        return mapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 切换用户角色。
     */
    public void changeRole(Long id, String role) {
        TelegramUserEntity user = mapper.selectById(id);
        if (user == null) throw new IllegalArgumentException("User not found: id=" + id);
        user.setRole(role);
        mapper.updateById(user);
        log.info("用户角色变更: userId={}, role={}", user.getUserId(), role);
    }

    /**
     * 封禁 / 解封用户。
     */
    public void toggleStatus(Long id, Integer status) {
        TelegramUserEntity user = mapper.selectById(id);
        if (user == null) throw new IllegalArgumentException("User not found: id=" + id);
        user.setStatus(status);
        mapper.updateById(user);
        log.info("用户状态变更: userId={}, status={} ({})", user.getUserId(), status,
                status == 1 ? "正常" : "黑名单");
    }

    private TelegramUserEntity findByUserId(Long userId) {
        return mapper.selectOne(
                new LambdaQueryWrapper<TelegramUserEntity>()
                        .eq(TelegramUserEntity::getUserId, userId));
    }
}
