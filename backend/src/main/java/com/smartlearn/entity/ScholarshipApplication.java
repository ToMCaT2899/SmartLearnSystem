package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("scholarship_application")
public class ScholarshipApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scholarshipId;
    private Long studentId;
    private String applyReason;
    private String supportingFiles;
    private String status;
    private Long reviewerId;
    private String reviewComment;
    private LocalDateTime applyTime;
    private LocalDateTime reviewTime;
}
