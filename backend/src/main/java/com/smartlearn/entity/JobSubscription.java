package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("job_subscription")
public class JobSubscription {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String keywords;
    private String industries;
    private String locations;
}
