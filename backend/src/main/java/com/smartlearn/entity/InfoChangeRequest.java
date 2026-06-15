package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("info_change_request")
public class InfoChangeRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String reason;
    private String status;
    private Long reviewerId;
    private String reviewComment;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
