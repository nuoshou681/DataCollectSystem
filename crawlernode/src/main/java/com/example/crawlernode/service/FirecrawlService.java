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
                        .timeout(20000) // 给复杂页面更长渲染时间
                        .mobile(true)
                        .build());
        // 判断是否为空
        if (doc == null || doc.getLinks() == null || doc.getLinks().isEmpty()) {
            return List.of();
        }
        for (String link : doc.getLinks()) {
            log.info("收集到的网页地址:" + link);
        }
        // 去对应网站规则过滤
        List<String> links = new ArrayList<>();
        if (seedUrl.contains("baike.baidu.com"))
            links = baiduBaikeLinkRule.filter(doc.getLinks(), limit);
        if (seedUrl.contains("www.bing.com"))
            links = bingLinkRule.filter(doc.getLinks(), limit);
        if (seedUrl.contains("search.sohu.com"))
            links = sohuNewsLinkRule.filter(doc.getLinks(), limit);
        // 添加原始链接
        links.add(seedUrl + keyword);
        return links;
    }
}