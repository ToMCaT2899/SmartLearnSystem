package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("late_return_record")
public class LateReturnRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private LocalDate date;
    private LocalTime returnTime;
    private String reason;
    private String status;
    private Long reviewerId;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String studentName;
    @TableField(exist = false)
    private String studentNo;
    @TableField(exist = false)
    private String reviewerName;
}
