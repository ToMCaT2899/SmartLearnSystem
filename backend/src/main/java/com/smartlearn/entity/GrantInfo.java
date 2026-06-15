package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("grant_info")
public class GrantInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private BigDecimal amount;
    private Integer quota;
    private String criteria;
    private LocalDateTime deadline;
    private Long publisherId;
    private String status;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String publisherName;
    @TableField(exist = false)
    private String publisherRole;
    @TableField(exist = false)
    private Integer applicationCount;
    @TableField(exist = false)
    private Integer admittedCount;
}
