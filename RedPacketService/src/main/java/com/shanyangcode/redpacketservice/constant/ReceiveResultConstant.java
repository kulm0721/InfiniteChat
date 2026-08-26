package com.shanyangcode.redpacketservice.constant;

/**
 * 红包领取结果常量类
 * <p>
 * 定义 Redis Lua 脚本的返回码
 *
 */
public final class ReceiveResultConstant {

    private ReceiveResultConstant() {
        // 私有构造函数，防止实例化
    }

    // ==================== Lua 脚本返回码 ====================

    /**
     * 领取失败：用户已经领取过该红包
     */
    public static final int ALREADY_RECEIVED = -1;

    /**
     * 领取失败：红包已被领完或已过期（金额池为空）
     *
     * 注意：此状态表示 Redis 数据为空，需查询数据库获取红包真实状态
     */
    public static final int EMPTY_POOL = -2;

    // ==================== 红包完成标识 ====================

    /**
     * 完成标识：红包未领完
     */
    public static final int COMPLETION_FLAG_NOT_COMPLETED = 0;

    /**
     * 完成标识：红包已领完
     */
    public static final int COMPLETION_FLAG_COMPLETED = 1;

    // ==================== 辅助方法 ====================

    /**
     * 判断红包是否已领完
     *
     * @param completedFlag 完成标识
     * @return true if 红包已领完
     */
    public static boolean isCompleted(int completedFlag) {
        return completedFlag == COMPLETION_FLAG_COMPLETED;
    }
}
