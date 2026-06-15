package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("grant_application")
public class GrantApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long grantId;
    private Long studentId;
    private String applyReason;
    private String supportingFiles;
    private String status;
    private Long reviewerId;
    private String reviewComment;
    private LocalDateTime applyTime;
    private LocalDateTime reviewTime;
}
