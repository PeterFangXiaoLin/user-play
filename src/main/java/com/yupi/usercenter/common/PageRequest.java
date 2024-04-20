package com.yupi.usercenter.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class PageRequest implements Serializable {

    // 参考文章：https://zhuanlan.zhihu.com/p/66210653
    private static final long serialVersionUID = 4855003648063789465L;
    /**
     * 当前第几页
     */
    protected long pageNum = 1;

    /**
     * 每页的大小
     */
    protected long pageSize = 10;
}
