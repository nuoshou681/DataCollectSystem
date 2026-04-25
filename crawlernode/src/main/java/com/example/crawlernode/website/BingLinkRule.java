package com.example.crawlernode.website;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BingLinkRule {
    private static final String SEARCH_PREFIX = "https://www.bing.com/search?q=";
    private static final String CK_PREFIX = "https://www.bing.com/ck/a?";
    private static final String BING_HOST_SUFFIX = "bing.com";

    // 去重，筛选链接
    public List<String> filter(String seedUrl, String keyword, List<String> links, int limit) {
        Set<String> urls = new LinkedHashSet<>();
        urls.add(seedUrl);
        for (String link : links) {
            if (link == null || link.isBlank()) {
                continue;
            }

            // 2) /ck/a 解码成外链后清洗掉内部链接
            if (link.startsWith(CK_PREFIX)) {
                String decoded = decodeBingRedirect(link);
                if (!decoded.isEmpty()) {
                    String normalized = normalizeToAbsolute(decoded);
                    if (!normalized.isEmpty() && !isBingSectionLink(normalized)) {
                        urls.add(normalized);
                    }
                }
            }
            if (urls.size() >= limit)
                break;
        }

        return new ArrayList<>(urls);
    }

    private String decodeBingRedirect(String url) {
        try {
            URI uri = URI.create(url);
            String query = uri.getQuery();
            if (query == null) {
                return "";
            }

            String u = null;
            for (String part : query.split("&")) {
                int idx = part.indexOf('=');
                if (idx > 0 && part.substring(0, idx).equals("u")) {
                    u = part.substring(idx + 1);
                    break;
                }
            }
            if (u == null || u.isEmpty()) {
                return "";
            }

            // 去掉 bing 的 a1 前缀
            if (u.startsWith("a1")) {
                u = u.substring(2);
            }

            return decodeBase64Url(u);
        } catch (Exception e) {
            return "";
        }
    }

    private String decodeBase64Url(String input) {
        try {
            int padding = (4 - input.length() % 4) % 4;
            String padded = input + "=".repeat(padding);

            byte[] bytes = java.util.Base64.getUrlDecoder().decode(padded);
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private String normalizeToAbsolute(String url) {
        try {
            URI uri = URI.create(url);
            if (uri.isAbsolute()) {
                return uri.toString();
            }
            // 相对路径补齐到 bing 域
            return URI.create("https://www.bing.com").resolve(uri).toString();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isBingSectionLink(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            String path = uri.getPath();
            if (host == null || path == null) {
                return false;
            }
            boolean isBingHost = host.equals(BING_HOST_SUFFIX) || host.endsWith("." + BING_HOST_SUFFIX);
            if (!isBingHost) {
                return false;
            }
            return path.contains("/maps")
                    || path.contains("/images")
                    || path.contains("/videos")
                    || path.contains("/news")
                    || path.contains("/shop")
                    || path.contains("/travel")
                    || path.contains("microsoft");
        } catch (Exception e) {
            return false;
        }
    }
}
