package com.atguigu.gulimall.product.vo;

import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {
    private boolean hasStock = true;
    // 1.sku基本信息 pms_sku_info
    SkuInfoEntity info;

    // 2.sku图片信息 pms_sku_images
    List<SkuImagesEntity> images;

    // 3.获取spu销售属性组合
    List<SkuItemSaleAttrVo> saleAttr;

    // 4.获取spu介绍
    SpuInfoDescEntity desc;

    // 5.获取spu的规格参数信息
    List<SpuItemAttrGroupVo> groupAttrs;

    //6、秒杀商品的优惠信息
    private SeckillSkuVo seckillSkuVo;

    @Data
    public static class SpuBaseAttrVo{
        private String attrName;
        private String attrValue;
    }

}
