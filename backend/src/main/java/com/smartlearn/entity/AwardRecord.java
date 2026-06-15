package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("award_record")
public class AwardRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String awardName;

    @TableField(exist = false)
    private String studentNo;
    private String awardLevel;
    private LocalDate awardDate;
    private String issuer;
    private String certificateFile;
}
