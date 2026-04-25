package com.example.crawlernode.service;

import com.example.crawlernode.website.BaiduBaikeLinkRule;
import com.example.crawlernode.website.BingLinkRule;
import com.example.crawlernode.website.SohuNewsLinkRule;
import com.firecrawl.client.FirecrawlClient;
import com.firecrawl.models.Document;
import com.firecrawl.models.ScrapeOptions;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;

@Service
public class FirecrawlService {

    private final FirecrawlClient client;

    private BaiduBaikeLinkRule baiduBaikeLinkRule = new BaiduBaikeLinkRule();
    private BingLinkRule bingLinkRule = new BingLinkRule();;
    private SohuNewsLinkRule sohuNewsLinkRule = new SohuNewsLinkRule();

    private static final Logger log = LoggerFactory.getLogger(FirecrawlService.class);

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
        List<String> links = new ArrayList<>();
        if (seedUrl.contains("baike.baidu.com")) {
            links = scrapeLinks(seedUrl, keyword, false);
            links = baiduBaikeLinkRule.filter(normalizedSeed, keyword, normalizeLinks(links), limit);
        } else if (seedUrl.contains("www.bing.com")) {
            links = scrapeLinks(seedUrl, keyword, false);
            links = bingLinkRule.filter(normalizedSeed, keyword, normalizeLinks(links), limit);
        } else if (seedUrl.contains("search.sohu.com")) {
            links = scrapeLinks(seedUrl, keyword, true);
            links = sohuNewsLinkRule.filter(normalizedSeed, keyword, normalizeLinks(links), limit);
        }
        return links;
    }

    public List<String> scrapeLinks(String seedUrl, String keyword, boolean mobile) {
        Document doc = client.scrape(
                seedUrl + keyword,
                ScrapeOptions.builder()
                        .formats(List.of((Object) "links"))
                        .onlyMainContent(false) // 保留全页面上下文，links 更全
                        .waitFor(2500) // 等待动态内容加载
                        .timeout(20000) // 给复杂页面更长渲染时间
                        .mobile(mobile)
                        .build());
        // 判断是否为空
        if (doc == null || doc.getLinks() == null || doc.getLinks().isEmpty()) {
            return List.of();
        }
        for (String link : doc.getLinks()) {
            log.info("收集到的网页地址:" + link);
        }
        return doc.getLinks();
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
