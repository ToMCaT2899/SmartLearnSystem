package com.smartlearn.service;

import org.springframework.web.multipart.MultipartFile;

public interface OssService {
    String uploadFile(MultipartFile file, String category);

    String getPresignedUrl(String url);

    void deleteFile(String url);
}
