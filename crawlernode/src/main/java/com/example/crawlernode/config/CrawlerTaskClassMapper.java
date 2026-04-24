package com.example.crawlernode.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.support.converter.DefaultClassMapper;

import com.example.crawlernode.entity.Task;

public class CrawlerTaskClassMapper extends DefaultClassMapper {

    public CrawlerTaskClassMapper() {
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("crawlerTask", Task.class);
        setIdClassMapping(idClassMapping);
        setTrustedPackages("*");
    }
}
