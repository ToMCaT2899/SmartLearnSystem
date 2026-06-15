package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("safety_report")
public class SafetyReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reporterId;
    private String reportType;
    private String title;
    private String description;
    private String location;
    private String images;
    private String status;
    private Long handlerId;
    private LocalDateTime handledAt;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String reporterName;
    @TableField(exist = false)
    private String reporterNo;
    @TableField(exist = false)
    private String handlerName;
}
