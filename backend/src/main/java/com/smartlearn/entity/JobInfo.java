package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("job_info")
public class JobInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String company;
    private String description;
    private String requirements;
    private String salaryRange;
    private String location;
    private Long publisherId;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    private LocalDate deadline;

    @TableField(exist = false)
    private String publisherName;
}
