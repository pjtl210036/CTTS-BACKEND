package com.example.demo.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实训项目实体
 */
@Data
public class TrainingProject {

    /** 项目编号 */
    private Long id;

    /** 项目名称 */
    private String title;

    /** 项目描述 */
    private String description;

    /** 项目状态：待开始 / 进行中 / 已完成 */
    private String status;

    /** 负责人 */
    private String assignee;

    /** 截止时间 */
    private LocalDateTime deadline;

    /** 创建时间 */
    private LocalDateTime createTime;
}
