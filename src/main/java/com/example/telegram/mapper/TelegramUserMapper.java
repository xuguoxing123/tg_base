package com.example.telegram.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.telegram.entity.TelegramUserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TelegramUserMapper extends BaseMapper<TelegramUserEntity> {

    /**
     * UPSERT：存在则更新活跃信息，不存在则插入。
     * 利用 user_id 唯一索引实现 ON DUPLICATE KEY UPDATE。
     */
    @Update("INSERT INTO cs_telegram_user (user_id, username, first_name, last_name, role, status, " +
            "first_seen_time, last_active_time, message_count) " +
            "VALUES (#{userId}, #{username}, #{firstName}, #{lastName}, 'USER', 1, NOW(), NOW(), 1) " +
            "ON DUPLICATE KEY UPDATE " +
            "username = VALUES(username), first_name = VALUES(first_name), " +
            "last_name = VALUES(last_name), last_active_time = NOW(), " +
            "message_count = message_count + 1")
    void upsert(@Param("userId") Long userId,
                @Param("username") String username,
                @Param("firstName") String firstName,
                @Param("lastName") String lastName);
}
