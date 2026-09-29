package com.yumi.cute.common;

public class UserContext {
    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<Long>();

    public static void setUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static Long getUserId() {
        return CURRENT_USER_ID.get();
    }

    /** 请求结束后必须调用，清理数据 **/
    public static void clear(){
        CURRENT_USER_ID.remove();
    }
}
