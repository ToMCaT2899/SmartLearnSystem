package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("second_classroom_score")
public class SecondClassroomScore {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String category;
    private String activityName;
    private BigDecimal score;
    private LocalDate recordDate;
    private Long verifierId;
}
