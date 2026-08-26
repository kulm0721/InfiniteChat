package com.shanyangcode.common.constant;

/**
 * 用户状态常量
 * <p>
 * 定义用户账号状态相关的常量值
 * 消除魔法数字，统一所有服务的用户状态判断
 */
public final class UserStateConstant {

    private UserStateConstant() {
        // 工具类，禁止实例化
    }

    // ===================== 用户状态值（User State）=====================

    /**
     * 用户正常状态
     */
    public static final int STATE_NORMAL = 0;

    /**
     * 用户封禁状态
     */
    public static final int STATE_BANNED = 1;

    /**
     * 用户注销状态
     */
    public static final int STATE_CANCELLED = 2;

    // ===================== 状态描述（State Description）=====================

    /**
     * 正常状态描述
     */
    public static final String DESC_NORMAL = "正常";

    /**
     * 封禁状态描述
     */
    public static final String DESC_BANNED = "封禁";

    /**
     * 注销状态描述
     */
    public static final String DESC_CANCELLED = "注销";

    /**
     * 用户不存在描述
     */
    public static final String DESC_NOT_EXIST = "用户不存在";

    /**
     * 未知状态描述
     */
    public static final String DESC_UNKNOWN = "未知";

    /**
     * 根据状态码获取状态描述
     *
     * @param state 状态码
     * @return 状态描述
     */
    public static String getStateName(Integer state) {
        if (state == null) {
            return DESC_NOT_EXIST;
        }
        return switch (state) {
            case STATE_NORMAL -> DESC_NORMAL;
            case STATE_BANNED -> DESC_BANNED;
            case STATE_CANCELLED -> DESC_CANCELLED;
            default -> DESC_UNKNOWN;
        };
    }

}
