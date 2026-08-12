package com.shanyangcode.userservice.constant;
/**
 * 好友状态枚举
 *
 * 数据库存储值说明：
 * - 0: 正常好友关系
 * - 1: 已拉黑
 * - 2: 已删除
 *
 */
public enum FriendStatusEnum {
    /**
     * 好友状态：非好友关系
     */
    NON_FRIEND(-1, "非好友"),

    /**
     * 好友状态：正常好友关系
     */
    NORMAL(0, "好友"),

    /**
     * 好友状态：已拉黑
     */
    BLOCKED(1, "拉黑"),

    /**
     * 好友状态：已删除
     */
    DELETED(2, "删除");

    private final int code;
    private final String description;

    FriendStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
    /**
     * 根据状态码获取枚举值
     *
     * @param code 状态码
     * @return 对应的枚举值
     * @throws IllegalArgumentException 如果状态码无效
     */
    public static FriendStatusEnum fromCode(int code) {
        for (FriendStatusEnum status : FriendStatusEnum.values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的好友状态码: " + code);
    }
}
