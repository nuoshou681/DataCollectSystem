package com.example.server.controller;

import org.springframework.web.bind.annotation.RestController;

import com.example.server.entity.CrawlerPageResultRecord;
import com.example.server.entity.Message.ApiResponse;
import com.example.server.entity.Message.ErrorCode;
import com.example.server.service.CrawlerPageResultService;
import com.example.server.service.CrawlerPageResultStreamService;

import java.nio.charset.StandardCharsets;
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
        return ApiResponse.success(crawlerPageResultService.queryResults(taskId));
    }

    @GetMapping(value = "/task/page-results/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter pageResultsStream() {
        return crawlerPageResultStreamService.subscribe();
    }

    @PostMapping("/task/cache-mhtml")
    public ApiResponse<?> cacheMhtml(@RequestParam Long pageResultId) {
        if (pageResultId == null) {
            return ApiResponse.error(ErrorCode.PARAM_ERROR, "pageResultId 不能为空");
        }

        try {
            CrawlerPageResultRecord cached = crawlerPageResultService.cacheMhtml(pageResultId);
            if (cached == null) {
                return ApiResponse.error(ErrorCode.NOT_FOUND, "页面结果不存在");
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
                    .loadMhtmlForDownload(pageResultId);
            if (downloadData == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("页面结果不存在");
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

}
