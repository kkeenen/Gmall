package com.atguigu.gulimall.product.vo;

import lombok.Data;

@Data
public class AttrResponseVo extends AttrVo{
    private String catelogName;
    private String groupName;

    private Long[] catelogPath;

}
