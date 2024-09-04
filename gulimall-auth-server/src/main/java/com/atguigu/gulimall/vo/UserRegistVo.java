package com.atguigu.gulimall.vo;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;

@Data
public class UserRegistVo {
    // jsr303
    @NotEmpty(message = "用户名必须提交")
    @Length(min = 3, max = 16, message = "用户名必须为3~16位")
    private String username;
    @NotEmpty(message = "密码必须提交")
    @Length(min = 3, max = 16, message = "密码必须为3~16位")
    private String password;
    @NotEmpty(message = "手机号必须填写")
    private String phone;
    @NotEmpty(message = "验证码必须填写")
    private String code;

}
