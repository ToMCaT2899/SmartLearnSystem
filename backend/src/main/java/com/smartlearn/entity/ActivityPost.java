package com.smartlearn.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("activity_post")
public class ActivityPost {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String category;
    private String organizer;
    private String location;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventTime;

    private Integer maxParticipants;
    private Integer currentParticipants;
    private Long publisherId;
    private String participantType;
    private String targetClass;
    private String status;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String publisherName;
}
