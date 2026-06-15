package com.smartlearn.controller;

import com.smartlearn.common.Result;
import com.smartlearn.service.OssService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final OssService ossService;

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                  @RequestParam(defaultValue = "general") String category) {
        String url = ossService.uploadFile(file, category);
        return Result.success(url);
    }

    @PostMapping("/upload/batch")
    public Result<List<String>> uploadBatch(@RequestParam("files") List<MultipartFile> files,
                                             @RequestParam(defaultValue = "general") String category) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(ossService.uploadFile(file, category));
        }
        return Result.success(urls);
    }

    @GetMapping("/download")
    public Result<String> download(@RequestParam("url") String url) {
        String signedUrl = ossService.getPresignedUrl(url);
        return Result.success(signedUrl);
    }
}
