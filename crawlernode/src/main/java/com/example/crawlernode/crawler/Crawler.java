package com.example.crawlernode.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.crawlernode.entity.CrawlerResult;
import com.example.crawlernode.entity.SubTask;

@Component
public class Crawler {

    @Value("${node.id}")
    private String nodeId;

    public CrawlerResult crawl(SubTask subTask) {
        try {
            Document doc = Jsoup.connect(subTask.getUrl() + subTask.getKeyword())
                    .userAgent(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36")
                    .referrer("https://www.bing.com")
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    .header("Cookie",
                            "ab_sr=1.0.1_N2NhNWNmN2NhNzMzNWYyYmMyNGNjOWQ1YTgxMjgwZTI1ZjAwMzM0OTM2ZTQxMzA5NjZjZjI5OTZjYTg2ZWFhZGRjZDBlYmRlNmY4Yjc0N2IzNjFiNTdiNWQ5ZjdhNzM3MzMyZGMwN2I4OThmZjkyYmFiNzNhNGIwNzk3Y2NjYTA3MTkxMDM5YWIxZGE0NThjYjk2NzIwMDI3ODg5YTk5ZmFiOWYzNWQ5MzgzNGQ1ODE0OWJhYmM1YmUwNDIxNzI1; BAIDU_WISE_UID=wapp_1767797753951_886; BAIDUID=7968177C29C8D0F287531BDEC65BD410:FG=1; baikeVisitId=40393ee6-61ba-420b-a86e-2fee9bb000bc; BDUSS=TIybUl5UENDYU1RMGx0d35ydk5RYlRMWFpWVzJGVjRnRTg5YmxaSnNTNHlBSVpwSVFBQUFBJCQAAAAAAQAAAAEAAAAiY8R4bnVvc2hvdTY4MQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADJzXmkyc15pM; channel=bing; H_WISE_SIDS=110085_660925_667684_675800_673302_675908_676689_679557_680626_680934_681079_681548_681637_681649_681358_675613_679782_682434_682876_682132_683230_681305_682415_683502_680578_680580_683785_683168_683922_683918_683963_683998_684153_683854_684226_684280_681439_653708_683059_684647_684614_684657_684617_684942_682565_683764_8000097_8000118_8000120_8000138_8000164_8000170_8000177_8000179_8000194_8000203; ploganon=deg1; ZFYDMLF1bmwQv8ytFHkeX8klhD4j3W63a6=BAiMqy1PoDMA:C; zhishiTopicRequestTime=1768359916394")
                    .timeout(5000)
                    .get();
            String text = doc.text();
            if (text.contains(subTask.getKeyword())) {
                return new CrawlerResult(subTask.getTaskId(), subTask.getSubtaskId(), nodeId, true, text, null);
            } else {
                return new CrawlerResult(subTask.getTaskId(), subTask.getSubtaskId(),nodeId, false, text, null);
            }
        } catch (Exception e) {
            return new CrawlerResult(subTask.getTaskId(), subTask.getSubtaskId(),nodeId, false, e.getMessage(),null);
        }
    }
}
