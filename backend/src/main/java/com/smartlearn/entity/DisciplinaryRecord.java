package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("disciplinary_record")
public class DisciplinaryRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String violationType;
    private String description;
    private String punishment;
    private LocalDate recordDate;
    private String status;
}
