package com.shanyangcode.redpacketservice.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shanyangcode.redpacketservice.model.entity.BalanceLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 余额变动记录 Mapper
 */
@Mapper
public interface BalanceLogMapper extends BaseMapper<BalanceLog> {
}
