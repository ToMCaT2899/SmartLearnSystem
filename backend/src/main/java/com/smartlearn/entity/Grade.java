package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("grade")
public class Grade {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long courseId;
    private BigDecimal score;
    private BigDecimal gpaPoint;
    private String semester;
    private String examType;

    @TableField(exist = false)
    private String studentNo;

    @TableField(exist = false)
    private String courseName;
}
