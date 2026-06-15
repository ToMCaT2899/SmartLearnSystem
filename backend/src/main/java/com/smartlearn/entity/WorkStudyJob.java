package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("work_study_job")
public class WorkStudyJob {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String requirements;
    private String salary;
    private String location;
    private Integer quota;
    private Integer enrolledCount;
    private Long publisherId;
    private String status;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String publisherName;
    @TableField(exist = false)
    private String publisherRole;
    @TableField(exist = false)
    private Integer applicationCount;
}
