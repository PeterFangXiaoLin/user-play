package com.yupi.usercenter.constant;

public interface UserConstant {
    /**
     * 用户登录状态 key
     */
    String USER_LOGIN_STATE = "userLoginState";

    /**
     * 用户权限
     * 普通用户 - 0
     * 管理员 - 1
     */
    int DEFAULT_ROLE = 0;
    int ADMIN_ROLE = 1;

    /**
     * 分页查询大小
     */
    long PAGE_NUM = 1;

    long PAGE_SIZE = 10;

    String USER_MATCH_KEY = "mysoul:match:%s";
}
