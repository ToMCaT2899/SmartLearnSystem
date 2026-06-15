package com.smartlearn.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.smartlearn.service.OssService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OssServiceImpl implements OssService {

    private final OSS ossClient;

    @Value("${aliyun.oss.bucket}")
    private String bucket;

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long PRESIGNED_EXPIRE_MS = 60 * 60 * 1000; // 1 hour

    @Override
    public String uploadFile(MultipartFile file, String category) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过10MB");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        if (!ext.matches("\\.(pdf|jpg|jpeg|png|doc|docx)")) {
            throw new IllegalArgumentException("不支持的文件类型: " + ext);
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String objectKey = String.format("uploads/%s/%s/%s%s", category, datePath, UUID.randomUUID(), ext);

        try {
            ossClient.putObject(bucket, objectKey, file.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }

        return String.format("https://%s.%s/%s", bucket, endpoint, objectKey);
    }

    @Override
    public String getPresignedUrl(String url) {
        String prefix = String.format("https://%s.%s/", bucket, endpoint);
        if (!url.startsWith(prefix)) {
            throw new IllegalArgumentException("无效的文件URL: " + url);
        }
        String objectKey = url.substring(prefix.length());
        Date expiration = new Date(System.currentTimeMillis() + PRESIGNED_EXPIRE_MS);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, objectKey);
        request.setExpiration(expiration);
        URL signedUrl = ossClient.generatePresignedUrl(request);
        return signedUrl.toString();
    }

    @Override
    public void deleteFile(String url) {
        if (url == null || url.isBlank()) return;
        String prefix = String.format("https://%s.%s/", bucket, endpoint);
        if (!url.startsWith(prefix)) return;
        String objectKey = url.substring(prefix.length());
        ossClient.deleteObject(bucket, objectKey);
    }
}
