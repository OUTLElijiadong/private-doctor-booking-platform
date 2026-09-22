package com.doctor.appointment.security;

/**
 * 登录用户上下文（基于 ThreadLocal，请求结束由拦截器清理）
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser user = get();
        return user == null ? null : user.getUserId();
    }

    public static String getRole() {
        LoginUser user = get();
        return user == null ? null : user.getRole();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
