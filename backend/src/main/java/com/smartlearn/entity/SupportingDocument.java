package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("supporting_document")
public class SupportingDocument {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String docType;
    private String filePath;
    private LocalDateTime uploadTime;
}
