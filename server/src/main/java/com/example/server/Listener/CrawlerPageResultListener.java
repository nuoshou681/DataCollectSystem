package com.example.server.Listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.server.config.RabbitMQConfig;
import com.example.server.entity.Message.CrawlerPageResult;
import com.example.server.service.CrawlerRuntimeStore;

@Component
public class CrawlerPageResultListener {

    private static final Logger log = LoggerFactory.getLogger(CrawlerPageResultListener.class);

    private final CrawlerRuntimeStore crawlerRuntimeStore;

    public CrawlerPageResultListener(CrawlerRuntimeStore crawlerRuntimeStore) {
        this.crawlerRuntimeStore = crawlerRuntimeStore;
    }

    @RabbitListener(queues = RabbitMQConfig.CRAWLER_RESULT_QUEUE)
    public void handlePageResult(CrawlerPageResult result) {
        log.info("收到页面结果: taskId={}, subTaskId={}, url={}, depth={}, success={}",
                result.getTaskId(),
                result.getSubTaskId(),
                result.getPageUrl(),
                result.getFilePath(),
                result.isSuccess());

        crawlerRuntimeStore.addPageResult(result);

        if (result.isSuccess()) {
            // 这里可以：
            // 1. 写入页面结果表
            // 2. 绑定到 subtask
            // 3. 生成前端展示数据
            // 4. 记录 snapshotPath 供下载
        } else {
            // 失败也建议落库，方便前端显示错误页面
        }
    }
}