package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("competition_record")
public class CompetitionRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String competitionName;

    @TableField(exist = false)
    private String studentNo;
    private String level;
    private String award;
    private LocalDate date;
    private String certificateFile;
}
