package com.example.crawlernode.crawler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.example.crawlernode.service.FirecrawlService;
import com.example.crawlernode.config.RabbitMQConfig;
import com.example.crawlernode.entity.CrawlerPageResult;
import com.example.crawlernode.entity.CrawlerTaskFinished;
import com.example.crawlernode.entity.SubTask;
import com.google.gson.JsonObject;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.CDPSession;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Crawler {

    private static final Logger log = LoggerFactory.getLogger(Crawler.class);

    private final RabbitTemplate rabbitTemplate;
    private final FirecrawlService firecrawlService;

    @Value("${node.id}")
    private String nodeId;

    @Value("${crawler.storage.base-dir:/Users/rain/Desktop/DataCollectSystem/shared/crawl-files}")
    private String baseDir;

    public Crawler(RabbitTemplate rabbitTemplate, FirecrawlService firecrawlService) {
        this.rabbitTemplate = rabbitTemplate;
        this.firecrawlService = firecrawlService;
    }

    public void crawl(SubTask subTask) {
        List<String> pageUrls = resolveLinks(subTask);

        for (String pageual : pageUrls) {
            log.info("firecrawl收集到的网站link: " + pageual);
        }
        if (pageUrls.isEmpty()) {
            reportTaskFinished(subTask, false, 0, "no result pages found");
            return;
        }

        int pageIndex = 0;
        int successCount = 0;

        for (String pageUrl : pageUrls) {
            pageIndex++;
            try {
                PageSnapshot snapshot = savePageAsMhtml(subTask, pageUrl, pageIndex);

                CrawlerPageResult result = new CrawlerPageResult(
                        subTask.getTaskId(),
                        subTask.getSubtaskId(),
                        nodeId,
                        pageUrl,
                        snapshot.title(),
                        pageIndex,
                        true,
                        snapshot.filePath(),
                        null);

                sendPageResult(result);
                successCount++;

            } catch (Exception e) {
                log.error("【CrawlerNode】{} 保存页面失败: subTaskId={}, pageUrl={}, error={}",
                        nodeId, subTask.getSubtaskId(), pageUrl, e.getMessage(), e);

                CrawlerPageResult result = new CrawlerPageResult(
                        subTask.getTaskId(),
                        subTask.getSubtaskId(),
                        nodeId,
                        pageUrl,
                        null,
                        pageIndex,
                        false,
                        null,
                        e.getMessage());

                sendPageResult(result);
            }
        }

        reportTaskFinished(subTask, true, successCount, "crawl finished");
    }

    private List<String> resolveLinks(SubTask subTask) {
        String seedUrl = safeTrim(subTask.getUrl());
        String keyword = safeTrim(subTask.getKeyword());
        int limit = subTask.getMaxLinksPerLevel();

        try {
            List<String> links;
            if (isSearchUrl(seedUrl)) {
                links = firecrawlService.searchLinks(keyword, limit);
            } else {
                links = firecrawlService.mapLinks(seedUrl, limit);
            }
            return deduplicateAndLimit(links, limit);
        } catch (Exception e) {
            log.error("【CrawlerNode】{} Firecrawl 解析链接失败: seedUrl={}, keyword={}, error={}",
                    nodeId, seedUrl, keyword, e.getMessage(), e);
            return List.of();
        }
    }

    private PageSnapshot savePageAsMhtml(SubTask subTask, String pageUrl, int pageIndex) throws IOException {
        String safeTaskId = String.valueOf(subTask.getTaskId());
        String safeSubTaskId = String.valueOf(subTask.getSubtaskId());

        Path dir = Path.of(baseDir, safeTaskId, safeSubTaskId);
        Files.createDirectories(dir);

        String fileName = String.format("%02d.mhtml", pageIndex);
        Path file = dir.resolve(fileName);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(java.util.List.of(
                            "--disable-features=LazyImageLoading,LazyFrameLoading",
                            "--no-sandbox")));

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(1600, 12000)
                    .setUserAgent(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36"));

            Page page = context.newPage();
            page.navigate(pageUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            // waitForBingOrPageReady(page);
            forceScrollToBottom(page);

            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(2500);
            waitForImagesComplete(page);

            String title = page.title();

            CDPSession session = context.newCDPSession(page);
            JsonObject params = new JsonObject();
            params.addProperty("format", "mhtml");

            JsonObject snapshot = session.send("Page.captureSnapshot", params);
            String mhtml = snapshot.get("data").getAsString();

            Files.writeString(file, mhtml, StandardCharsets.UTF_8);
            browser.close();

            return new PageSnapshot(title, file.toAbsolutePath().toString());
        }
    }

    private void forceScrollToBottom(Page page) {
        try {
            page.evaluate("""
                    async () => {
                        await new Promise(resolve => {
                            let totalHeight = 0;
                            const distance = 800;
                            const timer = setInterval(() => {
                                const scrollHeight = document.body.scrollHeight;
                                window.scrollBy(0, distance);
                                totalHeight += distance;
                                if (totalHeight >= scrollHeight) {
                                    clearInterval(timer);
                                    window.scrollTo(0, 0);
                                    resolve();
                                }
                            }, 200);
                        });
                    }
                    """);
        } catch (Exception e) {
            log.warn("【CrawlerNode】{} 强制滚动失败: {}", nodeId, e.getMessage());
        }
    }

    private void waitForImagesComplete(Page page) {
        try {
            page.waitForFunction("""
                    () => Array.from(document.images).every(img => img.complete)
                    """);
        } catch (Exception e) {
            log.warn("【CrawlerNode】{} 等待图片 complete 超时: {}", nodeId, e.getMessage());
        }
    }

    private void sendPageResult(CrawlerPageResult result) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CRAWLER_EXCHANGE,
                RabbitMQConfig.ROUTING_RESULT,
                result);

        log.info("【CrawlerNode】{} 已发送页面结果: subTaskId={}, pageUrl={}, filePath={}",
                nodeId, result.getSubTaskId(), result.getPageUrl(), result.getFilePath());
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
                RabbitMQConfig.ROUTING_TASK_FINISHED,
                finished);

        log.info("【CrawlerNode】{} 已发送子任务完成消息: subTaskId={}, success={}, totalPages={}",
                nodeId, subTask.getSubtaskId(), success, totalPages);
    }

    private record PageSnapshot(String title, String filePath) {
    }

    private boolean isSearchUrl(String seedUrl) {
        if (seedUrl == null) {
            return false;
        }
        return seedUrl.contains("/search") || seedUrl.contains("q=") || seedUrl.contains("{keyword}");
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private List<String> deduplicateAndLimit(List<String> links, int limit) {
        if (links == null || links.isEmpty() || limit <= 0) {
            return List.of();
        }

        Set<String> unique = new LinkedHashSet<>();
        for (String link : links) {
            if (link != null && !link.isBlank()) {
                unique.add(link);
            }
            if (unique.size() >= limit) {
                break;
            }
        }

        return new ArrayList<>(unique);
    }
}