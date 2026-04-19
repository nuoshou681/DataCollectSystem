package com.example.crawlernode.website;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SohuNewsLinkRule {
    private String tempalte_site = "www.sohu.com/a/";

    public List<String> filter(List<String> links, int limit) {
        // 去重，筛选链接
        Set<String> urls = new LinkedHashSet<>();
        for (String link : links) {
            if (link.contains(tempalte_site))
                urls.add(link);
            if (urls.size() > limit)
                break;
        }
        return new ArrayList<>(urls);
    }

}
