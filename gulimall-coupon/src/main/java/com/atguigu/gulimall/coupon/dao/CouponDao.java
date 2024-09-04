package com.atguigu.gulimall.coupon.dao;

import com.atguigu.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author shiguangchao
 * @email 2398180969@qq.com
 * @date 2024-05-11 20:21:07
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
