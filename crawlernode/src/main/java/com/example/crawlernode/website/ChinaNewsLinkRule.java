package com.example.crawlernode.website;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ChinaNewsLinkRule {

    private static final Set<String> EXCLUDE_PATHS = Set.of(
        "/video/", "/photo/", "/live/", "/user/", "/app/"
    );

    public List<String> filter(String seedUrl, String keyword, List<String> links, int limit) {
        Set<String> urls = new LinkedHashSet<>();
        for (String link : links) {
            if (link == null || !link.contains("chinanews.com.cn")) continue;
            if (EXCLUDE_PATHS.stream().anyMatch(link::contains)) continue;
            urls.add(link);
            if (urls.size() >= limit) break;
        }
        return new ArrayList<>(urls);
    }
}
