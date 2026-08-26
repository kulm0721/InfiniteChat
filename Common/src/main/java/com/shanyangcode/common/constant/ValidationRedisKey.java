package com.shanyangcode.common.constant;

/**
 * 消息校验 Redis 键常量
 *
 * 定义消息校验相关的 Redis 键前缀和缓存配置
 * 统一所有服务的 Redis 键定义
 */
public class ValidationRedisKey {

    private ValidationRedisKey() {
        // 工具类，禁止实例化
    }

    // ===================== Redis 键前缀（Redis Key Prefix）=====================

    /**
     * 好友状态缓存键前缀
     * 完整格式：msg:validate:friend:status:{userId}:{friendId}
     */
    public static final String FRIEND_STATUS_KEY_PREFIX = "msg:validate:friend:status:";

    // ===================== 缓存值（Cache Values）=====================

    /**
     * 非好友状态的缓存值
     * 用于区分"查询到非好友"和"缓存未命中"
     */
    public static final String NON_FRIEND_CACHE_VALUE = "-1";

    // ===================== 缓存 TTL（Cache TTL）=====================

    /**
     * 好友关系缓存 TTL（分钟）
     */
    public static final long FRIEND_STATUS_TTL_MINUTES = 5;

    // ===================== 工具方法（Utility Methods）=====================

    /**
     * 构建好友状态缓存键
     *
     * @param userId   用户 ID
     * @param friendId 好友 ID
     * @return 完整的 Redis 键
     */
    public static String buildFriendStatusKey(Long userId, Long friendId) {
        return FRIEND_STATUS_KEY_PREFIX + userId + ":" + friendId;
    }

    /**
     * 判断缓存值是否表示非好友状态
     *
     * @param cachedValue 缓存值
     * @return 是否为非好友
     */
    public static boolean isNonFriendValue(String cachedValue) {
        return NON_FRIEND_CACHE_VALUE.equals(cachedValue);
    }
}
