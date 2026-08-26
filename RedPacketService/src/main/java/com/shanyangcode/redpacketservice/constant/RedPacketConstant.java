package com.shanyangcode.redpacketservice.constant;

/**
 * 红包业务常量类
 * <p>
 * 定义红包相关的状态码、类型和时间配置
 *
 * @author shanyangcode
 */
public final class RedPacketConstant {

    private RedPacketConstant() {
        // 私有构造函数，防止实例化
    }

    // ==================== 红包状态常量 ====================

    /**
     * 红包状态：未领取完（初始状态）
     */
    public static final int STATUS_NOT_COMPLETED = 0;

    /**
     * 红包状态：已领取完
     */
    public static final int STATUS_COMPLETED = 1;

    /**
     * 红包状态：已过期
     */
    public static final int STATUS_EXPIRED = 2;

    /**
     * 红包状态：不存在数据库
     */
    public static final int STATUS_NOT_EXIST = -1;

    // ==================== 红包类型常量 ====================

    /**
     * 红包类型：普通红包（均分）
     */
    public static final int TYPE_NORMAL = 0;

    /**
     * 红包类型：拼手气红包（随机）
     */
    public static final int TYPE_RANDOM = 1;

    // ==================== 时间配置常量 ====================

    /**
     * 红包过期时间：24小时（毫秒）
     */
    public static final long EXPIRE_TIME_MS = 24 * 60 * 60 * 1000L;

    /**
     * Redis 缓存过期时间：25小时（比红包过期时间多1小时余量）
     */
    public static final int REDIS_CACHE_EXPIRE_HOURS = 25;

    // ==================== 金额限制常量 ====================

    /**
     * 单个红包金额上限：200 元（单位：元）
     * <p>
     * 参考微信红包规则：单个红包金额不超过 200 元
     */
    public static final int MAX_SINGLE_AMOUNT_YUAN = 200;

    // ==================== 金额转换常量 ====================

    /**
     * 元转分的乘数因子
     */
    public static final int YUAN_TO_FEN_MULTIPLIER = 100;

    // ==================== 辅助方法 ====================

    /**
     * 判断红包类型是否有效
     *
     * @param type 红包类型
     * @return true if 类型有效
     */
    public static boolean isValidType(int type) {
        return type == TYPE_NORMAL || type == TYPE_RANDOM;
    }
}
