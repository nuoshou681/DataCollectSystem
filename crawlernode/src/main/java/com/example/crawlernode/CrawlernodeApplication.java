package com.example.crawlernode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.example.crawlernode.entity.task.SubTask;
import com.example.crawlernode.service.TaskExecutor;
@SpringBootApplication
public class CrawlernodeApplication implements org.springframework.boot.CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(CrawlernodeApplication.class, args);
	}

	@Override
	public void run(String... args) {
        SubTask task = new SubTask(
                "https://baike.baidu.com/item/",
                "文学"
        );

        TaskExecutor.execute(task);
    }
}
