package com.atguigu.gulimall.search.vo;

import lombok.Data;
import org.elasticsearch.client.license.LicensesStatus;

import java.sql.Struct;
import java.util.List;

/**
 * 封装页面所有可能传递过来的查询条件
 */
@Data
public class SearchParam {

    private String keyword; // 页面传来的关键字
    private Long catalog3Id; // 三级分类id

    private String sort; // 排序条件
    private Integer hasStock; //是否有货
    private String skuPrice; // 按价格
    private List<Long> brandId; // 按品牌
    private List<String> attrs; // 按照属性筛选

    private Integer pageNum=1; // 分页
    private String _queryString; //原生的所有查询条件


}
