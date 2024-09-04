package com.atguigu.common.exception;

public enum BizCodeEnum {
    VALID_EXCEPTION(10001,"参数格式校验失败"),
    UNKNOW_EXCEPTION(1000,"未知异常"),
    PRODUCT_UP_EXCEPTION(11000,"商品上架异常"),
    USER_EXIST_EXCEPTION(15001, "用户名存在"),
    PHONE_EXIST_EXCEPTION(15002, "手机号存在"),
    LOGINACCT_PASSWORD_INVALID_EXCEPTION(15003,"账号或密码错误");
    private int code;
    private String msg;
    BizCodeEnum(int code, String msg){
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
