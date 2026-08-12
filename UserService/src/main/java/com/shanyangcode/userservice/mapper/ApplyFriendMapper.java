package com.shanyangcode.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shanyangcode.userservice.model.entity.ApplyFriend;
import org.apache.ibatis.annotations.Mapper;
/**
 * 好友申请Mapper接口
 *
 * 功能说明：
 * - 继承MyBatis-Plus的BaseMapper，自动提供CRUD方法
 * - 无需编写XML配置或自定义SQL
 * - 复杂查询在Service层使用Lambda Wrapper实现
 */
@Mapper
public interface ApplyFriendMapper extends BaseMapper<ApplyFriend> {
}
