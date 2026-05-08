package com.example.crawlernode.service;

import com.example.crawlernode.website.BaiduBaikeLinkRule;
import com.example.crawlernode.website.CctvNewsLinkRule;
import com.example.crawlernode.website.ChinaNewsLinkRule;
import com.example.crawlernode.website.GuanchaLinkRule;
import com.example.crawlernode.website.HuanqiuLinkRule;
import com.example.crawlernode.website.SinaNewsLinkRule;
import com.example.crawlernode.website.SohuNewsLinkRule;
import com.example.crawlernode.website.TencentNewsLinkRule;
import com.example.crawlernode.website.ThePaperLinkRule;
import com.example.crawlernode.website.WikipediaLinkRule;
import com.firecrawl.client.FirecrawlClient;
import com.firecrawl.models.Document;
import com.firecrawl.models.ScrapeOptions;
import com.firecrawl.models.SearchData;
import com.firecrawl.models.SearchOptions;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;

@Service
public class FirecrawlService {

    private final FirecrawlClient client;

    private final BaiduBaikeLinkRule baiduBaikeLinkRule = new BaiduBaikeLinkRule();
    private final CctvNewsLinkRule cctvNewsLinkRule = new CctvNewsLinkRule();
    private final ChinaNewsLinkRule chinaNewsLinkRule = new ChinaNewsLinkRule();
    private final GuanchaLinkRule guanchaLinkRule = new GuanchaLinkRule();
    private final HuanqiuLinkRule huanqiuLinkRule = new HuanqiuLinkRule();
    private final SinaNewsLinkRule sinaNewsLinkRule = new SinaNewsLinkRule();
    private final SohuNewsLinkRule sohuNewsLinkRule = new SohuNewsLinkRule();
    private final TencentNewsLinkRule tencentNewsLinkRule = new TencentNewsLinkRule();
    private final ThePaperLinkRule thePaperLinkRule = new ThePaperLinkRule();
    private final WikipediaLinkRule wikipediaLinkRule = new WikipediaLinkRule();

    private static final Logger log = LoggerFactory.getLogger(FirecrawlService.class);

    private static final List<String> DIRTY_PATH_KEYWORDS = List.of(
        "/login", "/signin", "/signup", "/register", "/registration", "/auth", "/oauth", "/sso",
        "/logout", "/signout", "/account", "/profile", "/settings", "/password", "/reset",
        "/verify", "/activate", "/change-password", "/forgot-password",
        "/admin", "/administrator", "/wp-admin", "/wp-login", "/dashboard", "/backend",
        "/manage", "/cms", "/console", "/controlpanel", "/cp/",
        "/privacy", "/terms", "/tos", "/policy", "/agreement", "/cookies", "/disclaimer",
        "/eula", "/gdpr", "/legal", "/notice",
        "/help", "/faq", "/support", "/contact", "/about", "/about-us", "/feedback",
        "/guide", "/tutorial", "/docs", "/documentation", "/manual",
        "/download", "/downloads", "/app", "/apps", "/install", "/mobile", "/getapp",
        "/client", "/software", "/apk",
        "/share", "/sharing", "/invite", "/referral", "/refer", "/affiliate", "/promo",
        "/advertise", "/campaign",
        "/video", "/videos", "/audio", "/music", "/podcast", "/live", "/stream",
        "/radio", "/playlist", "/watch", "/channel",
        "/cart", "/checkout", "/payment", "/order", "/basket", "/pay", "/pricing",
        "/subscribe", "/subscription", "/shop", "/buy",
        "/api/", "/graphql", "/rss", "/feed", "/atom", "robots.txt", "favicon",
        "manifest.json", "service-worker",
        "/redirect", "/linkout", "/goto", "/jump"
    );

    private static final List<String> FILE_EXTENSIONS = List.of(
        ".apk", ".exe", ".dmg", ".pkg", ".deb", ".rpm", ".msi",
        ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
        ".zip", ".rar", ".7z", ".tar", ".gz",
        ".mp4", ".mp3", ".avi", ".mov", ".wmv", ".flv", ".webm", ".ogg", ".wav",
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".tiff", ".svg", ".ico",
        ".json", ".xml", ".css", ".js", ".map"
    );

    public FirecrawlService(@Value("${firecrawl.api-key}") String apiKey) {
        this.client = FirecrawlClient.builder()
                .apiKey(apiKey)
                .build();
    }

    public List<String> getLinks(String seedUrl, String keyword, int limit) {
        if (seedUrl == null || seedUrl.isBlank() || limit <= 0) {
            return List.of();
        }

        String normalizedSeed = normalizeSeedUrl(seedUrl, keyword);
        List<String> links;

        if (seedUrl.contains("chinanews.com.cn")) {
            links = searchLinks(keyword, limit, "chinanews.com.cn");
            links = normalizeLinks(links);
            links = filterDirtyLinks(links);
            links = chinaNewsLinkRule.filter(normalizedSeed, keyword, links, limit);
        } else if (seedUrl.contains("cctv.com")) {
            links = searchLinks(keyword, limit, "cctv.com");
            links = normalizeLinks(links);
            links = filterDirtyLinks(links);
            links = cctvNewsLinkRule.filter(normalizedSeed, keyword, links, limit);
        } else if (seedUrl.contains("guancha.cn")) {
            links = searchLinks(keyword, limit, "guancha.cn");
            links = normalizeLinks(links);
            links = filterDirtyLinks(links);
            links = guanchaLinkRule.filter(normalizedSeed, keyword, links, limit);
        } else if (seedUrl.contains("news.qq.com")) {
            links = searchLinks(keyword, limit, "news.qq.com");
            links = normalizeLinks(links);
            links = filterDirtyLinks(links);
            links = tencentNewsLinkRule.filter(normalizedSeed, keyword, links, limit);
        } else if (seedUrl.contains("news.sina.com.cn") || seedUrl.contains("sina.com.cn")) {
            links = searchLinks(keyword, limit, "news.sina.com.cn");
            links = normalizeLinks(links);
            links = filterDirtyLinks(links);
            links = sinaNewsLinkRule.filter(normalizedSeed, keyword, links, limit);
        } else if (seedUrl.contains("thepaper.cn")) {
            links = searchLinks(keyword, limit, "thepaper.cn");
            links = normalizeLinks(links);
            links = filterDirtyLinks(links);
            links = thePaperLinkRule.filter(normalizedSeed, keyword, links, limit);
        } else if (seedUrl.contains("huanqiu.com")) {
            links = searchLinks(keyword, limit, "huanqiu.com");
            links = normalizeLinks(links);
            links = filterDirtyLinks(links);
            links = huanqiuLinkRule.filter(normalizedSeed, keyword, links, limit);
        } else if (seedUrl.contains("www.bing.com")) {
            links = searchLinks(keyword, limit, null);
            links = normalizeLinks(links);
            links = filterDirtyLinks(links);
        } else if (seedUrl.contains("baike.baidu.com")) {
            links = scrapeLinks(seedUrl, keyword, false);
            links = normalizeLinks(links);
            links = filterDirtyLinks(links);
            links = baiduBaikeLinkRule.filter(normalizedSeed, keyword, links, limit);
        } else if (seedUrl.contains("search.sohu.com")) {
            links = scrapeLinks(seedUrl, keyword, true);
            links = normalizeLinks(links);
            links = filterDirtyLinks(links);
            links = sohuNewsLinkRule.filter(normalizedSeed, keyword, links, limit);
        } else if (seedUrl.contains("en.wikipedia.org")) {
            links = scrapeLinks(seedUrl, keyword, false);
            links = normalizeLinks(links);
            links = filterDirtyLinks(links);
            links = wikipediaLinkRule.filter(normalizedSeed, keyword, links, limit);
        } else {
            return List.of();
        }
        return links;
    }

    private List<String> searchLinks(String keyword, int limit, String siteDomain) {
        String query = keyword;
        if (siteDomain != null && !siteDomain.isBlank()) {
            query = "site:" + siteDomain + " " + keyword;
        }

        log.info("Firecrawl /search: query={}, limit={}", query, limit);
        try {
            SearchOptions options = SearchOptions.builder()
                    .limit(limit)
                    .timeout(30000)
                    .build();
            SearchData data = client.search(query, options);
            if (data == null || data.getWeb() == null || data.getWeb().isEmpty()) {
                log.warn("Firecrawl /search 返回空结果: query={}", query);
                return List.of();
            }
            List<String> urls = data.getWeb().stream()
                    .map(item -> (String) item.get("url"))
                    .filter(url -> url != null && !url.isBlank())
                    .collect(Collectors.toList());
            log.info("Firecrawl /search 返回 {} 条结果", urls.size());
            for (String url : urls) {
                log.info("  search结果: {}", url);
            }
            return urls;
        } catch (Exception e) {
            log.error("Firecrawl /search 调用失败: query={}, error={}", query, e.getMessage());
            return List.of();
        }
    }

    public List<String> scrapeLinks(String seedUrl, String keyword, boolean mobile) {
        Document doc = client.scrape(
                seedUrl + keyword,
                ScrapeOptions.builder()
                        .formats(List.of((Object) "links"))
                        .onlyMainContent(false)
                        .waitFor(2500)
                        .timeout(20000)
                        .mobile(mobile)
                        .build());
        if (doc == null || doc.getLinks() == null || doc.getLinks().isEmpty()) {
            return List.of();
        }
        for (String link : doc.getLinks()) {
            log.info("收集到的网页地址:" + link);
        }
        return doc.getLinks();
    }

    private List<String> filterDirtyLinks(List<String> links) {
        if (links == null || links.isEmpty()) {
            return List.of();
        }

        Set<String> seenPath = new LinkedHashSet<>();
        List<String> result = new ArrayList<>();

        for (String link : links) {
            if (link == null || link.isBlank()) continue;

            String lower = link.toLowerCase();
            if (lower.startsWith("javascript:") || lower.startsWith("mailto:")
                    || lower.startsWith("tel:") || link.equals("#")) {
                continue;
            }

            try {
                URI uri = URI.create(link);
                String host = uri.getHost();
                String path = uri.getPath();
                if (path == null) path = "/";
                String lowerPath = path.toLowerCase();

                boolean isFile = false;
                for (String ext : FILE_EXTENSIONS) {
                    if (lowerPath.endsWith(ext)) {
                        isFile = true;
                        break;
                    }
                }
                if (isFile) continue;

                boolean isDirty = false;
                for (String kw : DIRTY_PATH_KEYWORDS) {
                    if (lowerPath.contains(kw.toLowerCase())) {
                        isDirty = true;
                        break;
                    }
                }
                if (isDirty) continue;

                String key = (host != null ? host : "") + path;
                if (seenPath.contains(key)) continue;
                seenPath.add(key);

                result.add(link);
            } catch (Exception e) {
                continue;
            }
        }
        return result;
    }

    private List<String> normalizeLinks(List<String> links) {
        if (links == null || links.isEmpty()) {
            return List.of();
        }
        return links.stream()
                .map(this::canonicalizeUrl)
                .filter(link -> link != null && !link.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private String normalizeSeedUrl(String seedUrl, String keyword) {
        return canonicalizeUrl(seedUrl + URLEncoder.encode(keyword == null ? "" : keyword, StandardCharsets.UTF_8));
    }

    private String canonicalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(rawUrl.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return rawUrl.trim();
            }

            String path = uri.getPath() == null || uri.getPath().isBlank() ? "/" : uri.getPath();
            String query = normalizeQuery(uri.getRawQuery());
            URI normalized = new URI(
                    scheme.toLowerCase(),
                    uri.getUserInfo(),
                    host.toLowerCase(),
                    uri.getPort(),
                    path,
                    query,
                    null);
            return normalized.toString();
        } catch (Exception e) {
            return rawUrl.trim();
        }
    }

    private String normalizeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }

        List<String> kept = new ArrayList<>();
        for (String pair : rawQuery.split("&")) {
            if (pair == null || pair.isBlank()) {
                continue;
            }
            String key = pair;
            int idx = pair.indexOf('=');
            if (idx >= 0) {
                key = pair.substring(0, idx);
            }
            String lowerKey = key.toLowerCase();
            if (lowerKey.startsWith("utm_")
                    || "spm".equals(lowerKey)
                    || "scm".equals(lowerKey)
                    || "from".equals(lowerKey)
                    || "frommodule".equals(lowerKey)
                    || "refer".equals(lowerKey)
                    || "src".equals(lowerKey)
                    || "fr".equals(lowerKey)) {
                continue;
            }
            kept.add(pair);
        }

        if (kept.isEmpty()) {
            return null;
        }
        return String.join("&", kept);
    }
}
