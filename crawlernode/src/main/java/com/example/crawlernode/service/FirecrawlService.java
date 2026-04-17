package com.example.crawlernode.service;

import com.firecrawl.client.FirecrawlClient;
import com.firecrawl.models.MapData;
import com.firecrawl.models.MapOptions;
import com.firecrawl.models.SearchData;
import com.firecrawl.models.SearchOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FirecrawlService {

    private final FirecrawlClient client;

    public FirecrawlService(@Value("${firecrawl.api-key}") String apiKey) {
        this.client = FirecrawlClient.builder()
                .apiKey(apiKey)
                .build();
    }

    public List<String> searchLinks(String query, int limit) {
        SearchData results = client.search(
                query,
                SearchOptions.builder()
                        .limit(limit)
                        .build());

        List<String> urls = new ArrayList<>();
        if (results != null && results.getWeb() != null) {
            for (Map<String, Object> item : results.getWeb()) {
                Object url = item.get("url");
                if (url != null) {
                    String link = url.toString();
                    if (!link.isBlank()) {
                        urls.add(link);
                    }
                }
            }
        }
        return urls;
    }

    public List<String> mapLinks(String siteUrl, int limit) {
        MapData data = client.map(
                siteUrl,
                MapOptions.builder()
                        .limit(limit)
                        .build());

        List<String> urls = new ArrayList<>();
        if (data != null && data.getLinks() != null) {
            for (Map<String, Object> item : data.getLinks()) {
                Object url = item.get("url");
                if (url != null) {
                    String link = url.toString();
                    if (!link.isBlank()) {
                        urls.add(link);
                    }
                }
            }
        }
        return urls;
    }

    public String scrapeMarkdown(String url) {
        return client.scrape(url).getMarkdown();
    }
}