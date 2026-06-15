package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("family_member")
public class FamilyMember {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String relation;
    private String name;
    private String phone;
    private String workUnit;
    private String address;
}
