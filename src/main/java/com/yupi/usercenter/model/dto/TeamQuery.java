package com.yupi.usercenter.model.dto;

import com.yupi.usercenter.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 队伍查询封装类
 */

// @EqualsAndHashCode(callSuper = true)
// 参考文章：https://blog.csdn.net/c851204293/article/details/96989512

@Data
@EqualsAndHashCode(callSuper = true)
public class TeamQuery extends PageRequest {

    private Long id;

    /**
     * 队伍 id 列表查询
     */
    private List<Long> teamIdList;

    /**
     * 队伍名称
     */
    private String name;


    /**
     * 用户输入的搜索关键词
     */
    private String searchText;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 队长 id
     */
    private Long userId;
    /**
     * 0 - 公开， 1 - 私有，2 - 加密
     */
    private Integer status;

}
