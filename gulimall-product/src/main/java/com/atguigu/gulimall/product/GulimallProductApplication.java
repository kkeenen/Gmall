package com.atguigu.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 整合mybatis-plus
 *
 */
@EnableCaching // 开启缓存
@SpringBootApplication
@MapperScan("com.atguigu.gulimall.product.dao")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.atguigu.gulimall.product.feign")
public class GulimallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
