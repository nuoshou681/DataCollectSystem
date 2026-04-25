package com.example.server.controller;

import org.springframework.web.bind.annotation.RestController;

import com.example.server.common.util.SecurityUtils;
import com.example.server.entity.CrawlerPageResultRecord;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Message.ErrorCode;
import com.example.server.service.CrawlerPageResultService;
import com.example.server.service.CrawlerPageResultStreamService;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class TaskResultController {

    @Autowired
    private CrawlerPageResultService crawlerPageResultService;

    @Autowired
    private CrawlerPageResultStreamService crawlerPageResultStreamService;

    @GetMapping("/task/page-results")
    public ApiResponse<List<CrawlerPageResultRecord>> pageResults(@RequestParam(required = false) Long taskId) {
        return ApiResponse.success(
                crawlerPageResultService.queryResults(taskId, SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin()));
    }

    @GetMapping(value = "/task/page-results/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter pageResultsStream() {
        return crawlerPageResultStreamService.subscribe(SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin());
    }

    @PostMapping("/task/cache-mhtml")
    public ApiResponse<?> cacheMhtml(@RequestParam Long pageResultId) {
        if (pageResultId == null) {
            return ApiResponse.error(ErrorCode.PARAM_ERROR, "pageResultId 不能为空");
        }

        try {
            CrawlerPageResultRecord cached = crawlerPageResultService.cacheMhtml(
                    pageResultId,
                    SecurityUtils.getCurrentUserId(),
                    SecurityUtils.isAdmin());
            if (cached == null) {
                return ApiResponse.error(ErrorCode.NOT_FOUND, "页面结果不存在或无权限访问");
            }
            return ApiResponse.success(cached);
        } catch (Exception e) {
            return ApiResponse.error(ErrorCode.SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/task/download-mhtml")
    public ResponseEntity<?> downloadMhtml(@RequestParam Long pageResultId) {
        if (pageResultId == null) {
            return ResponseEntity.badRequest().body("pageResultId 不能为空");
        }

        try {
            CrawlerPageResultService.MhtmlDownloadData downloadData = crawlerPageResultService
                    .loadMhtmlForDownload(pageResultId, SecurityUtils.getCurrentUserId(), SecurityUtils.isAdmin());
            if (downloadData == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("页面结果不存在或无权限访问");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename(downloadData.getFileName(), StandardCharsets.UTF_8)
                            .build());
            headers.setContentLength(downloadData.getContent().length);

            return new ResponseEntity<>(downloadData.getContent(), headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping(value = "/task/page-results/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<?> exportPageResults(@RequestParam(required = false) Long taskId) {
        List<CrawlerPageResultRecord> records = crawlerPageResultService.queryResults(
                taskId,
                SecurityUtils.getCurrentUserId(),
                SecurityUtils.isAdmin());

        String csv = buildCsv(records);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("page-results.csv", StandardCharsets.UTF_8).build());
        return new ResponseEntity<>(csv.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }

    private String buildCsv(List<CrawlerPageResultRecord> records) {
        String header = "pageResultId,taskId,pageIndex,success,siteType,pageTitle,pageUrl,errorCode,errorMessage";
        String body = records.stream()
                .map(record -> String.join(",",
                        csv(record.getPageResultId()),
                        csv(record.getTaskId()),
                        csv(record.getPageIndex()),
                        csv(record.getSuccess()),
                        csv(record.getSiteType()),
                        csv(record.getPageTitle()),
                        csv(record.getPageUrl()),
                        csv(record.getErrorCode()),
                        csv(record.getErrorMessage())))
                .collect(Collectors.joining("\n"));
        return body.isBlank() ? header + "\n" : header + "\n" + body + "\n";
    }

    private String csv(Object value) {
        if (value == null) {
            return "\"\"";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

}
