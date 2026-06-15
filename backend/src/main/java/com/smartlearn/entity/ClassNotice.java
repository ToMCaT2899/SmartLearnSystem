package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("class_notice")
public class ClassNotice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String classIdentifier;
    private String title;
    private String content;
    private Long publisherId;
    private Integer isPinned;

    @TableField(exist = false)
    private String publisherName;
    private LocalDateTime publishedAt;
}
