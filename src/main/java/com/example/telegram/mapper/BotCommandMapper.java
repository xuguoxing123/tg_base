package com.example.telegram.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.telegram.entity.BotCommandEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BotCommandMapper extends BaseMapper<BotCommandEntity> {
}
