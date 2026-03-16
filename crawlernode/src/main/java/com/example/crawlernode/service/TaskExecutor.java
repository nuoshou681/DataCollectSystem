package com.example.crawlernode.service;

import com.example.crawlernode.crawler.TextCrawler;
import com.example.crawlernode.entity.result.CrawlerResult;
import com.example.crawlernode.entity.task.SubTask;

public class TaskExecutor {
    public static CrawlerResult execute(SubTask task) {
        TextCrawler crawler = new TextCrawler();
        CrawlerResult result = crawler.crawl(task);
        System.out.println("执行结果: " + result.getData());
        return result;
    }
}
