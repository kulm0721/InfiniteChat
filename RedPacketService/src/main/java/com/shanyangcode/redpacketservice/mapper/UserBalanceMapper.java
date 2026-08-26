package com.shanyangcode.redpacketservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shanyangcode.redpacketservice.model.entity.UserBalance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户余额 Mapper
 * <p>
 * 数据访问层只继承 BaseMapper，不定义自定义 SQL。
 * 余额的扣减 / 增加（含原子条件校验）由 Service 层构建 LambdaUpdateWrapper 调用 update 完成，
 * 保持 Mapper/Service 职责分离。
 */
@Mapper
public interface UserBalanceMapper extends BaseMapper<UserBalance> {
}
