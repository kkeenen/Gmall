package com.atguigu.gulimall.order.controller;

import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.UUID;

@RestController
public class RabbitController {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @GetMapping("/sendMQ")
    public String sendMQ(@RequestParam(value = "num",defaultValue = "10") Integer num){
        for(int i=1;i<=num;i++){
            if(i%2==0){
                OrderReturnReasonEntity entity = new OrderReturnReasonEntity();
//                entity.setId(1L);
                entity.setName("嘻嘻");
                entity.setCreateTime(new Date());
                rabbitTemplate.convertAndSend("hello-java-exchange","hello.java",entity, new CorrelationData(UUID.randomUUID().toString()));
            }else{
                OrderReturnReasonEntity entity = new OrderReturnReasonEntity();
//                entity.setId(1L);
                entity.setName("不嘻嘻");
                rabbitTemplate.convertAndSend("hello-java-exchange","hello22.java",entity, new CorrelationData(UUID.randomUUID().toString()));
            }
        }
        return "ok";
    }
}
