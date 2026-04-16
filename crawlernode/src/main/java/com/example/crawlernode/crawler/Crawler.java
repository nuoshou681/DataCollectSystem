package com.example.crawlernode.crawler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.crawlernode.config.RabbitMQConfig;
import com.example.crawlernode.entity.CrawlerPageResult;
import com.example.crawlernode.entity.CrawlerTaskFinished;
import com.example.crawlernode.entity.SubTask;

@Component
public class Crawler {

    private static final Logger log = LoggerFactory.getLogger(Crawler.class);

    @Value("${node.id}")
    private String nodeId;

    private final RabbitTemplate rabbitTemplate;

    public Crawler(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void crawl(SubTask subTask) {
        Set<String> visited = new HashSet<>();
        List<String> seedUrls = new ArrayList<>();
        String url = "http://www.bing.com/search?q=" + subTask.getUrl() + subTask.getKeyword();
        seedUrls.add(url);

        int totalPages = 0;

        try {
            totalPages = crawlRecursive(subTask, seedUrls, visited, 0);
            reportTaskFinished(subTask, true, totalPages, "crawl finished");
        } catch (Exception e) {
            log.error("【CrawlerNode】{} 递归爬取失败: subTaskId={}, error={}",
                    nodeId, subTask.getSubtaskId(), e.getMessage(), e);
            reportTaskFinished(subTask, false, totalPages, e.getMessage());
        }
    }

    private int crawlRecursive(SubTask subTask, List<String> currentLevelUrls, Set<String> visited, int depth) {
        if (depth > subTask.getMaxDepth()) {
            return 0;
        }

        int totalPages = 0;
        List<String> nextLevelUrls = new ArrayList<>();

        for (String url : currentLevelUrls) {
            if (totalPages >= subTask.getMaxLinksPerLevel()) {
                break;
            }
            if (!visited.add(url)) {
                continue;
            }

            try {
                log.info("【CrawlerNode】{} 开始请求页面: subTaskId={}, depth={}, url={}",
                        nodeId, subTask.getSubtaskId(), depth, url);

                Document doc = Jsoup.connect(url)
                        .userAgent(
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
                        .referrer("https://www.bing.com")
                        .timeout(8000)
                        .get();

                String content = doc.body() != null ? doc.body().text() : "";
                String title = doc.title();

                CrawlerPageResult pageResult = new CrawlerPageResult(
                        subTask.getTaskId(),
                        subTask.getSubtaskId(),
                        nodeId,
                        url,
                        title,
                        depth,
                        true,
                        content,
                        null);

                sendPageResult(pageResult);
                totalPages++;

                Elements links = doc.select("a[href]");
                for (Element a : links) {
                    String href = a.absUrl("href");
                    if (href == null || href.isBlank()) {
                        continue;
                    }
                    nextLevelUrls.add(href);
                    if (nextLevelUrls.size() >= subTask.getMaxLinksPerLevel()) {
                        break;
                    }
                }

            } catch (Exception e) {
                CrawlerPageResult pageResult = new CrawlerPageResult(
                        subTask.getTaskId(),
                        subTask.getSubtaskId(),
                        nodeId,
                        url,
                        null,
                        depth,
                        false,
                        null,
                        e.getMessage());
                sendPageResult(pageResult);
                log.error("【CrawlerNode】{} 页面抓取失败: subTaskId={}, url={}, error={}",
                        nodeId, subTask.getSubtaskId(), url, e.getMessage(), e);
            }
        }

        if (!nextLevelUrls.isEmpty() && depth < subTask.getMaxDepth()) {
            totalPages += crawlRecursive(subTask, nextLevelUrls, visited, depth + 1);
        }

        return totalPages;
    }

    private void sendPageResult(CrawlerPageResult result) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CRAWLER_EXCHANGE,
                RabbitMQConfig.ROUTING_RESULT,
                result);

        log.info("【CrawlerNode】{} 已发送页面结果: subTaskId={}, pageUrl={}, depth={}",
                nodeId, result.getSubTaskId(), result.getPageUrl(), result.getDepth());
    }

    public void reportTaskFinished(SubTask subTask, boolean success, int totalPages, String message) {
        CrawlerTaskFinished finished = new CrawlerTaskFinished(
                subTask.getTaskId(),
                subTask.getSubtaskId(),
                nodeId,
                success,
                totalPages,
                message);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CRAWLER_EXCHANGE,
                RabbitMQConfig.ROUTING_RESULT,
                finished);

        log.info("【CrawlerNode】{} 已发送子任务完成消息: subTaskId={}, success={}, totalPages={}",
                nodeId, subTask.getSubtaskId(), success, totalPages);
    }
}