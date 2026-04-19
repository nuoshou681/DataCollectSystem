package com.example.crawlernode.website;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BingLinkRule {
    private static final String SEARCH_PREFIX = "https://www.bing.com/search?q=";
    private static final String CK_PREFIX = "https://www.bing.com/ck/a?";

    // 去重，筛选链接
    public List<String> filter(List<String> links, int limit) {
        Set<String> urls = new LinkedHashSet<>();

        for (String link : links) {
            if (link == null || link.isBlank()) {
                continue;
            }

            // 1) search?q= 原样保留
            if (link.startsWith(SEARCH_PREFIX)) {
                urls.add(link);
            }

            // 2) /ck/a 解码成外链后保存
            if (link.startsWith(CK_PREFIX)) {
                String decoded = decodeBingRedirect(link);
                if (!decoded.isEmpty()) {
                    urls.add(decoded);
                }
            }
            // if (urls.size() > limit)
            //     break;
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
}