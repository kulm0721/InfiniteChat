package com.shanyangcode.redpacketservice.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shanyangcode.redpacketservice.model.entity.RedPacket;
import org.apache.ibatis.annotations.Mapper;

/**
 * 红包 Mapper
 */
@Mapper
public interface RedPacketMapper extends BaseMapper<RedPacket> {
}
