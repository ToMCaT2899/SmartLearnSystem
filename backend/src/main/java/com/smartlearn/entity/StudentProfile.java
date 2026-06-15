package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("student_profile")
public class StudentProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String studentNo;
    private String name;
    private String photoUrl;
    private String phone;
    private String idCard;
    private String college;
    private String major;
    private String grade;
    private String className;
    private String campus;
    private String dormitory;
    private String offCampusAddress;
    private String medicalHistory;
    private String hobbies;
    private String personality;
    private String careerGoal;
    private String advisorName;
    private String advisorPhone;
    private String difficultyLevel;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
