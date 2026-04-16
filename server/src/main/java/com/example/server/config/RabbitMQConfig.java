package com.example.server.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String CRAWLER_EXCHANGE = "crawler.exchange";
    public static final String CRAWLER_TASK_QUEUE = "crawler.task.queue";
    public static final String CRAWLER_RESULT_QUEUE = "crawler.result.queue";
    public static final String CRAWLER_STATUS_QUEUE = "crawler.status.queue";

    public static final String ROUTING_TASK = "crawler.task";
    public static final String ROUTING_RESULT = "crawler.result";
    public static final String ROUTING_STATUS = "crawler.status";

    @Bean
    public DirectExchange crawDirectExchange() {
        return new DirectExchange(CRAWLER_EXCHANGE);
    }

    @Bean
    public Queue crawlerTaskQueue() {
        return new Queue(CRAWLER_TASK_QUEUE, true);
    }

    @Bean
    public Queue crawlerResultQueue() {
        return new Queue(CRAWLER_RESULT_QUEUE, true);
    }

    @Bean
    public Queue crawlerStatusQueue() {
        return new Queue(CRAWLER_STATUS_QUEUE, true);
    }

    @Bean
    public DefaultClassMapper rabbitClassMapper() {
        DefaultClassMapper classMapper = new DefaultClassMapper();
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("com.example.crawlernode.entity.CrawlerStatus",
                com.example.server.entity.CrawlerStatus.class);
        idClassMapping.put("com.example.crawlernode.entity.CrawlerResult",
                com.example.server.entity.CrawlResult.class);
        classMapper.setIdClassMapping(idClassMapping);
        classMapper.setTrustedPackages("*"); // 不太安全但是方便
        return classMapper;
    }

    @Bean
    public MessageConverter rabbitMessageConverter(DefaultClassMapper rabbitClassMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setClassMapper(rabbitClassMapper);
        return converter;
    }

    /**
     * 让 @RabbitListener 使用上面的 JSON 转换器和类型映射。
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        return factory;
    }

    @Bean
    public Binding taskBinding(DirectExchange crawlDirectExchange, Queue crawlerTaskQueue) {
        return BindingBuilder.bind(crawlerTaskQueue)
                .to(crawlDirectExchange)
                .with(ROUTING_TASK);
    }

    @Bean
    public Binding resultBinding(DirectExchange crawlDirectExchange, Queue crawlerResultQueue) {
        return BindingBuilder.bind(crawlerResultQueue)
                .to(crawlDirectExchange)
                .with(ROUTING_RESULT);
    }

    @Bean
    public Binding statusBinding(DirectExchange crawlDirectExchange, Queue crawlerStatusQueue) {
        return BindingBuilder.bind(crawlerStatusQueue)
                .to(crawlDirectExchange)
                .with(ROUTING_STATUS);
    }
}
