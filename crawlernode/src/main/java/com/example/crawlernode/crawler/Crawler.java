package com.example.crawlernode.crawler;

import com.example.crawlernode.config.RabbitMQConfig;
import com.example.crawlernode.entity.CrawlerPageResult;
import com.example.crawlernode.entity.CrawlerTaskFinished;
import com.example.crawlernode.entity.Task;
import com.example.crawlernode.service.FirecrawlService;
import com.google.gson.JsonObject;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.CDPSession;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Crawler {

    private static final Logger log = LoggerFactory.getLogger(Crawler.class);

    private static final List<String> CONSENT_SELECTORS = List.of(
            "#onetrust-accept-btn-handler",
            "button#didomi-notice-agree-button",
            "#CybotCookiebotDialogBodyLevelButtonLevelOptinAllowAll",
            "button[aria-label*='Accept' i]",
            "button[title*='Accept' i]");

    private static final List<String> CONSENT_TEXT_SELECTORS = List.of(
            "button:has-text('Accept All')",
            "button:has-text('Accept all')",
            "button:has-text('Accept')",
            "button:has-text('I Agree')",
            "button:has-text('I agree')",
            "button:has-text('Allow all')",
            "button:has-text('同意')",
            "button:has-text('我同意')",
            "button:has-text('确定')",
            "button:has-text('全部接受')",
            "button:has-text('允许全部')",
            "[role='button']:has-text('Accept')",
            "[role='button']:has-text('同意')",
            "[role='button']:has-text('确定')");

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

    public void crawl(Task task) {
        List<String> pageUrls = resolveLinks(task);
        if (pageUrls.isEmpty()) {
            reportTaskFinished(task, false, 0, 0, 0, "NO_RESULT_PAGES", "no result pages found");
            return;
        }

        int pageIndex = 0;
        int totalPages = pageUrls.size();
        int successCount = 0;
        int failedCount = 0;

        for (String pageUrl : pageUrls) {
            pageIndex++;
            try {
                PageSnapshot snapshot = savePageAsMhtml(task, pageUrl, pageIndex);
                CrawlerPageResult result = new CrawlerPageResult(
                        task.getTaskId(),
                        nodeId,
                        resolveSiteType(task, pageUrl),
                        pageUrl,
                        snapshot.title(),
                        pageIndex,
                        totalPages,
                        true,
                        snapshot.filePath(),
                        "FILE",
                        "multipart/related",
                        snapshot.fileSizeBytes(),
                        snapshot.sha256(),
                        null,
                        null);
                sendPageResult(result);
                successCount++;
            } catch (Exception e) {
                log.error("【CrawlerNode】{} 保存页面失败: taskId={}, pageUrl={}, error={}",
                        nodeId, task.getTaskId(), pageUrl, e.getMessage(), e);
                CrawlerPageResult result = new CrawlerPageResult(
                        task.getTaskId(),
                        nodeId,
                        resolveSiteType(task, pageUrl),
                        pageUrl,
                        null,
                        pageIndex,
                        totalPages,
                        false,
                        null,
                        "FILE",
                        "multipart/related",
                        null,
                        null,
                        "PAGE_CAPTURE_ERROR",
                        e.getMessage());
                sendPageResult(result);
                failedCount++;
            }
        }

        reportTaskFinished(task, failedCount == 0, totalPages, successCount, failedCount, failedCount == 0 ? null : "PARTIAL_CAPTURE_FAILED", "crawl finished");
    }

    private void sendPageResult(CrawlerPageResult result) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CRAWLER_EXCHANGE,
                RabbitMQConfig.ROUTING_RESULT,
                result);
    }

    public void reportTaskFinished(Task task, boolean success, int totalPages, int successPages, int failedPages, String errorCode, String message) {
        CrawlerTaskFinished finished = new CrawlerTaskFinished(
                task.getTaskId(),
                nodeId,
                success,
                totalPages,
                successPages,
                failedPages,
                errorCode,
                message);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CRAWLER_EXCHANGE,
                RabbitMQConfig.ROUTING_TASK_FINISHED,
                finished);
    }

    private List<String> resolveLinks(Task task) {
        String seedUrl = safeTrim(task.getUrl());
        String keyword = safeTrim(task.getKeyword());
        int limit = task.getMaxLinksPerLevel() == null ? 10 : task.getMaxLinksPerLevel();

        try {
            return firecrawlService.getLinks(seedUrl, keyword, limit);
        } catch (Exception e) {
            log.error("【CrawlerNode】{} Firecrawl 解析链接失败: seedUrl={}, keyword={}, error={}",
                    nodeId, seedUrl, keyword, e.getMessage(), e);
            return List.of();
        }
    }

    private PageSnapshot savePageAsMhtml(Task task, String pageUrl, int pageIndex) throws IOException {
        String safeTaskId = String.valueOf(task.getTaskId());
        Path dir = Path.of(baseDir, safeTaskId);
        Files.createDirectories(dir);

        String fileName = String.format("%02d.mhtml", pageIndex);
        Path file = dir.resolve(fileName);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of("--disable-features=LazyImageLoading,LazyFrameLoading", "--no-sandbox")));

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(1600, 12000)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36"));

            Page page = context.newPage();
            page.navigate(pageUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            acceptConsentIfPresent(page);
            forceScrollToBottom(page);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(2000);
            acceptConsentIfPresent(page);
            waitForImagesComplete(page);

            String title = page.title();
            CDPSession session = context.newCDPSession(page);
            JsonObject params = new JsonObject();
            params.addProperty("format", "mhtml");

            JsonObject snapshot = session.send("Page.captureSnapshot", params);
            String mhtml = snapshot.get("data").getAsString();
            Files.writeString(file, mhtml, StandardCharsets.UTF_8);
            browser.close();

            byte[] bytes = Files.readAllBytes(file);
            return new PageSnapshot(title, file.toAbsolutePath().toString(), (long) bytes.length, sha256(bytes));
        }
    }

    private void acceptConsentIfPresent(Page page) {
        for (int round = 0; round < 3; round++) {
            boolean roundClicked = false;
            for (Frame frame : page.frames()) {
                if (clickConsentInFrame(frame)) {
                    roundClicked = true;
                    page.waitForTimeout(500);
                }
            }
            if (!roundClicked) {
                break;
            }
        }
    }

    private boolean clickConsentInFrame(Frame frame) {
        for (String selector : CONSENT_SELECTORS) {
            try {
                Locator locator = frame.locator(selector).first();
                if (locator.isVisible(new Locator.IsVisibleOptions().setTimeout(500))) {
                    locator.click(new Locator.ClickOptions().setTimeout(1200));
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        for (String selector : CONSENT_TEXT_SELECTORS) {
            try {
                Locator locator = frame.locator(selector).first();
                if (locator.isVisible(new Locator.IsVisibleOptions().setTimeout(500))) {
                    locator.click(new Locator.ClickOptions().setTimeout(1200));
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        return false;
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
            page.waitForFunction("() => Array.from(document.images).every(img => img.complete)");
        } catch (Exception e) {
            log.warn("【CrawlerNode】{} 等待图片 complete 超时: {}", nodeId, e.getMessage());
        }
    }

    private String resolveSiteType(Task task, String pageUrl) {
        if (task.getSiteType() != null && !task.getSiteType().isBlank()) {
            return task.getSiteType();
        }
        if (pageUrl.contains("sohu.com")) {
            return "SOHU";
        }
        if (pageUrl.contains("bing.com")) {
            return "BING";
        }
        if (pageUrl.contains("baidu.com")) {
            return "BAIDU_BAIKE";
        }
        return "UNKNOWN";
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private record PageSnapshot(String title, String filePath, Long fileSizeBytes, String sha256) {
    }
}
