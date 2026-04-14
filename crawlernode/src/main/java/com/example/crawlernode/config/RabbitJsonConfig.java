package com.example.crawlernode.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

@Configuration
public class RabbitJsonConfig {

    @Bean
    public MessageConverter rabbitMessageConverter() {
        // 使用 Jackson 把对象 <-> JSON
        return new Jackson2JsonMessageConverter();
    }
}