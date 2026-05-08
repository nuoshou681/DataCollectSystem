package com.example.crawlernode.website;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TencentNewsLinkRule {

    private static final Set<String> EXCLUDE_PATHS = Set.of(
        "/v/", "/video/", "/live/", "/photo/", "/zhihu/",
        "/ads/", "/advert/", "/user/", "/app/", "/download/"
    );

    public List<String> filter(String seedUrl, String keyword, List<String> links, int limit) {
        Set<String> urls = new LinkedHashSet<>();
        for (String link : links) {
            if (link == null || !link.contains("news.qq.com")) continue;
            if (EXCLUDE_PATHS.stream().anyMatch(link::contains)) continue;
            urls.add(link);
            if (urls.size() >= limit) break;
        }
        return new ArrayList<>(urls);
    }
}
