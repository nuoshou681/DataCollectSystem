package com.example.crawlernode;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRabbit
@EnableScheduling
@SpringBootApplication
public class CrawlernodeApplication {

        public static void main(String[] args) {
                SpringApplication.run(CrawlernodeApplication.class, args);
        }

}
