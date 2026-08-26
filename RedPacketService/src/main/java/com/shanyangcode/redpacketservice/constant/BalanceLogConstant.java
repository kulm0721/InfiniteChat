package com.shanyangcode.redpacketservice.constant;

/**
 * 余额日志类型常量类
 * <p>
 * 定义用户余额变动的类型
 *
 * @author shanyangcode
 */
public final class BalanceLogConstant {
    private BalanceLogConstant() {
        // 私有构造函数，防止实例化
    }

    // ==================== 余额变动类型常量 ====================

    /**
     * 余额变动类型：发送红包（余额减少）
     */
    public static final int TYPE_SEND = 0;

    /**
     * 余额变动类型：领取红包（余额增加）
     */
    public static final int TYPE_RECEIVE = 1;

    /**
     * 余额变动类型：红包退回（余额增加）
     * <p>
     * 场景：红包过期后，剩余金额退回给发送者
     */
    public static final int TYPE_REFUND = 2;
}
