package com.shanyangcode.redpacketservice.constant;


/**
 * Redis Key 常量类
 * <p>
 * 定义红包模块使用的 Redis Key 模板和前缀
 *
 */
public final class RedisKeyConstant {

    private RedisKeyConstant() {
        // 私有构造函数，防止实例化
    }

    // ==================== 红包 Redis Key 模板 ====================

    /**
     * 红包金额池 Key 模板
     * <p>
     * 数据结构：List
     * <p>
     * 用法：String.format(POOL_TEMPLATE, redPacketId)
     * <p>
     * 示例：redpacket:1001:pool
     */
    public static final String POOL_TEMPLATE = "redpacket:%s:pool";

    /**
     * 红包领取记录 Key 模板
     * <p>
     * 数据结构：Hash (userId -> amount)
     * <p>
     * 用法：String.format(RECORDS_TEMPLATE, redPacketId)
     * <p>
     * 示例：redpacket:1001:records
     */
    public static final String RECORDS_TEMPLATE = "redpacket:%s:records";

    /**
     * 红包过期任务队列 Key
     * <p>
     * 数据结构：ZSet (member=redPacketId, score=expireTimestamp)
     * <p>
     * 用于时间轮算法的延迟任务调度
     */
    public static final String EXPIRE_ZSET = "redpacket-expire-zset";

    /** 防重复提交 Key 前缀 */
    public static final String PREVENT_DUPLICATE_PREFIX = "prevent:duplicate:";

    // ==================== 辅助方法 ====================

    /**
     * 生成红包金额池 Key
     *
     * @param redPacketId 红包ID
     * @return Redis Key
     */
    public static String getPoolKey(Long redPacketId) {
        return String.format(POOL_TEMPLATE, redPacketId);
    }

    public static String getPoolKey(String redPacketId) {
        return String.format(POOL_TEMPLATE, redPacketId);
    }

    /**
     * 生成红包领取记录 Key
     *
     * @param redPacketId 红包ID
     * @return Redis Key
     */
    public static String getRecordsKey(Long redPacketId) {
        return String.format(RECORDS_TEMPLATE, redPacketId);
    }

}
