package com.yupi.usercenter.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 队伍和用户信息封装类
 */

@Data
public class TeamUserVO implements Serializable {

    private static final long serialVersionUID = -473241524332323123L;
    /**
     * id
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 0 - 公开， 1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 队长 id
     */
    private Long userId;

    /**
     * 密码
     */
    private String password;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;


    /**
     * 队长（创建人）的用户信息
     */
    private UserVO createUser;

    /**
     * 队伍里的成员列表
     */
    private List<UserVO> userVOList;

    /**
     * 该队伍我是否已加入
     */
    private boolean hasJoin = false;
}
