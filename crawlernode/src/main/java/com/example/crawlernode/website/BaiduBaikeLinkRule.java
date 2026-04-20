package com.example.crawlernode.website;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BaiduBaikeLinkRule {
    private String template_site_1 = "baike.baidu.com/item/";
    private String tempalte_site_2 = "fromModule=lemma_inlink";

    // 去重，筛选链接
    public List<String> filter(String seedUrl, String keyword, List<String> links, int limit) {
        Set<String> urls = new LinkedHashSet<>();
        urls.add(seedUrl + keyword);
        for (String link : links) {
            if (link.contains(template_site_1) && link.contains(tempalte_site_2))
                urls.add(link);
            if (urls.size() >= limit)
                break;
        }
        return new ArrayList<>(urls);
    }
}
