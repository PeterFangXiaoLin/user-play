package com.yupi.usercenter.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 8978229923644091981L;

    private String userAccount;
    private String userPassword;
}
