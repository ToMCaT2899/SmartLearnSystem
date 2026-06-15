package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("midterm_assessment")
public class MidtermAssessment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String semester;
    private String selfSummary;
    private String advisorComment;
    private BigDecimal score;
    private String status;
    private LocalDateTime createdAt;
}
