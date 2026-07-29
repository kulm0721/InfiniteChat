package com.shanyangcode.realtimeservice.websocket;

import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentHashMap;

public class ChannelManager {
    private static final ConcurrentHashMap<String, Channel> USER_CHANNEL_MAP = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Channel, String> CHANNEL_USER_MAP = new ConcurrentHashMap<>();

    public static void addUserChannel(String userId,Channel channel) {
        USER_CHANNEL_MAP.put(userId, channel);
    }

    public static void addChannelUser(String userId,Channel channel) {
        CHANNEL_USER_MAP.put(channel, userId);
    }

    public static void removeChannelUser(Channel channel) {
        CHANNEL_USER_MAP.remove(channel);
    }

    public static void removeUserChannel(String userId) {
        USER_CHANNEL_MAP.remove(userId);
    }

    public static Channel getChannelByUserId(String userId) {
        return USER_CHANNEL_MAP.get(userId);
    }

    public static String getUserIdByChannel(Channel channel) {
        return CHANNEL_USER_MAP.get(channel);
    }
}
