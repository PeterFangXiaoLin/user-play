package com.yupi.usercenter.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteTeamRequest implements Serializable {

    private static final long serialVersionUID = 4462678433153528635L;
    /**
     * 删除（解散）的队伍 id
     */
    Long id;
}
