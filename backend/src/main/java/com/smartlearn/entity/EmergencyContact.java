package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("emergency_contact")
public class EmergencyContact {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String name;
    private String relation;
    private String phone;
    private String address;
}
