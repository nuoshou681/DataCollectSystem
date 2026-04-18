package com.example.crawlernode.service;

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
import org.slf4j.Logger;

@Service
public class FirecrawlService {

    private final FirecrawlClient client;

    private static final Logger log = LoggerFactory.getLogger(FirecrawlService.class);

    public FirecrawlService(@Value("${firecrawl.api-key}") String apiKey) {
        this.client = FirecrawlClient.builder()
                .apiKey(apiKey)
                .build();
    }

    public List<String> mapLinks(String seedUrl, String keyword, int limit) {
        if (seedUrl == null || seedUrl.isBlank() || limit <= 0) {
            return List.of();
        }

        Document doc = client.scrape(
                seedUrl + keyword,
                ScrapeOptions.builder()
                        .formats(List.of((Object) "links"))
                        .onlyMainContent(false) // 保留全页面上下文，links 更全
                        .waitFor(2500) // 等待动态内容加载
                        .timeout(45000) // 给复杂页面更长渲染时间
                        .mobile(false)
                        .build());
        if (doc == null || doc.getLinks() == null || doc.getLinks().isEmpty()) {
            return List.of();
        }
        return normalizeLinks(doc.getLinks(), limit);
    }

    private List<String> normalizeLinks(List<String> links, int limit) {
        Set<String> uniq = new LinkedHashSet<>();
        int len = links.size();
        for (int i = len / 4; i < len; i++) {
            if (links.get(i) == null) {
                continue;
            }
            String v = links.get(i).trim();
            if (v.isEmpty()) {
                continue;
            }
            if (!(v.startsWith("http://") || v.startsWith("https://"))) {
                continue;
            }
            uniq.add(v);
            if (uniq.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(uniq);
    }
}