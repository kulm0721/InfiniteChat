package com.shanyangcode.common.constant;

/**
 * 消息类型常量
 *
 * 消息类型分类：
 * - 0-99：聊天消息（文本、图片、表情包、红包等）
 * - 100-199：系统通知（好友申请、新会话等）
 *
 * 本常量类定义系统通知类型（100-199范围）
 */
public class MessageTypeConstant {
    public static final int TEXT_MESSAGE = 0;

    public static final int IMAGE_MESSAGE = 1;

    public static final int EMOJI_MESSAGE = 2;

    public static final int RED_PACKET_MESSAGE = 3;

    /**
     * 系统通知：收到好友申请
     * 场景：用户A向用户B发送好友申请
     */
    public static final int TYPE_SYSTEM_NEW_APPLY = 101;

    /**
     * 系统通知：新会话创建
     * 场景：用户A和用户B成为好友后，系统创建单聊会话
     */
    public static final int TYPE_SYSTEM_NEW_SESSION = 102;

    /**
     * 系统通知：新群聊会话创建（群组邀请通知）
     * 场景：用户被邀请加入群聊
     */
    public static final int TYPE_SYSTEM_NEW_GROUP_SESSION = 103;

    /**
     * 系统通知：群聊踢出通知
     * 场景：用户被踢出群聊，通知群聊所有成员（包括被踢出者）
     */
    public static final int TYPE_SYSTEM_GROUP_KICK = 104;

    /**
     * 聊天消息类型范围：0-99
     */
    public static final int CHAT_MESSAGE_MIN = 0;
    public static final int CHAT_MESSAGE_MAX = 99;

    /**
     * 系统通知类型范围：100-199
     */
    public static final int SYSTEM_NOTIFICATION_MIN = 100;
    public static final int SYSTEM_NOTIFICATION_MAX = 199;

    /**
     * 判断是否为系统通知类型
     *
     * @param type 消息类型
     * @return true if 是系统通知类型
     */
    public static boolean isSystemNotification(int type) {
        return type >= SYSTEM_NOTIFICATION_MIN && type <= SYSTEM_NOTIFICATION_MAX;
    }

    /**
     * 判断是否为聊天消息类型
     *
     * @param type 消息类型
     * @return true if 是聊天消息类型
     */
    public static boolean isChatMessage(int type) {
        return type >= CHAT_MESSAGE_MIN && type <= CHAT_MESSAGE_MAX;
    }
}
