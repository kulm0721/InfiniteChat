package com.shanyangcode.userservice.constant;

public enum FriendApplicationStatusEnum {
    UNREAD(0, "未读"),

    ACCEPTED(1,"通过"),

    REJECTED(2,"拒绝"),

    READ(3,"已读"),

    EXPIRED(4,"过期");

    private final int code;
    private final String description;

    FriendApplicationStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static FriendApplicationStatusEnum fromCode(int code) {
        for (FriendApplicationStatusEnum status : FriendApplicationStatusEnum.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的好友申请状态码 ： " + code);
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
    }
