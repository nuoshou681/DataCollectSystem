package com.example.server.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("分布式爬虫采集系统 API")
                        .version("1.0")
                        .description("Spring Boot + Vue 3 分布式爬虫数据采集系统接口文档"));
    }
}
