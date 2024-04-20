package com.yupi.usercenter.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class QuitTeamRequest implements Serializable {
    private static final long serialVersionUID = 1778917642236151059L;
    /**
     * 退出的队伍 id
     */
    private Long id;
}
