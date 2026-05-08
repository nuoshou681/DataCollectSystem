package com.example.crawlernode.website;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class WikipediaLinkRule {
    private static final String WIKI_HOST = "en.wikipedia.org";

    private static final String[] EXCLUDE_NAMESPACES = {
        "/wiki/Wikipedia:", "/wiki/Help:", "/wiki/File:", "/wiki/Category:",
        "/wiki/Template:", "/wiki/Portal:", "/wiki/Special:",
        "/wiki/Talk:", "/wiki/User_talk:", "/wiki/Wikipedia_talk:",
        "/wiki/User:", "/wiki/Draft:", "/wiki/TimedText:", "/wiki/Module:"
    };

    public List<String> filter(String seedUrl, String keyword, List<String> links, int limit) {
        Set<String> urls = new LinkedHashSet<>();
        urls.add(seedUrl);

        for (String link : links) {
            if (link == null || link.isBlank()) continue;
            if (urls.size() >= limit) break;

            String host = getHost(link);
            if (host == null) continue;

            if (WIKI_HOST.equals(host)) {
                String path = getPath(link);
                if (path != null && path.startsWith("/wiki/") && !isExcludedNamespace(path)) {
                    urls.add(link);
                }
            }
        }

        return new ArrayList<>(urls);
    }

    private boolean isExcludedNamespace(String path) {
        for (String ns : EXCLUDE_NAMESPACES) {
            if (path.startsWith(ns)) return true;
        }
        // 排除包含 "_talk:" 的任何讨论页
        String lower = path.toLowerCase();
        if (lower.contains("_talk:") && lower.startsWith("/wiki/")) return true;
        return false;
    }

    private String getHost(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private String getPath(String url) {
        try {
            return URI.create(url).getPath();
        } catch (Exception e) {
            return null;
        }
    }
}
