package com.atguigu.gulimall.order.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class MyRabbitConfig {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 定制RabbitTemplate
    @PostConstruct // MyRabbitConfig对象创建完成后 才会执行这个方法
    public void initRabbitTemplate() {

        // 2.设置确认回调
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             *
             * @param correlationData correlation data for the callback. 消息的唯一id
             * @param ack true for ack, false for nack 消息是否成功收到
             * @param cause An optional cause, for nack, when available, otherwise null.
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                System.out.println("confirm" + correlationData);
            }
        });

        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            /**
             * 只要消息没有投递给指定的队列 就出发这个失败回调
             * @param message the returned message. 投递失败的消息详细信息
             * @param replyCode the reply code.     回复的状态码
             * @param replyText the reply text.     回复的文本内容
             * @param exchange the exchange.        当时发给哪个交换机
             * @param routingKey the routing key.   当时的路由键
             */
            @Override
            public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
                System.out.println("Failed message:" + message + "replyCode:" + replyCode + "replyText:" + replyText + "exchange:" + exchange);
            }
        });

    }
}
