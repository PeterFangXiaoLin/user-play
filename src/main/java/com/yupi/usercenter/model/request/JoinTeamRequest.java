package com.yupi.usercenter.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class JoinTeamRequest implements Serializable {

    private static final long serialVersionUID = -5117944844149606693L;
    /**
     * id
     */
    private Long id;

    /**
     * 密码
     */
    private String password;
}
