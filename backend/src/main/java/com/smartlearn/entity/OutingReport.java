package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("outing_report")
public class OutingReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String destination;
    private LocalDate departureDate;
    private LocalDate returnDate;
    private String companion;
    private String reason;
    private String emergencyContact;
    private String status;
    private Long reviewerId;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String studentName;
    @TableField(exist = false)
    private String studentNo;
    @TableField(exist = false)
    private String reviewerName;
}
