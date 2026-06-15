-- Smart Learn System Initial Schema

DROP DATABASE IF EXISTS smart_learn;

CREATE DATABASE IF NOT EXISTS smart_learn DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_learn;

-- ==================== Auth & Users ====================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `real_name` VARCHAR(50) NOT NULL,
    `role` VARCHAR(20) NOT NULL COMMENT 'STUDENT, TEACHER, ADMIN',
    `phone` VARCHAR(20),
    `email` VARCHAR(100),
    `avatar` VARCHAR(255),
    `status` TINYINT DEFAULT 1 COMMENT '1=active, 0=disabled',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_role` (`role`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB;

-- ==================== Module 1: Student Basic Info ====================
-- student_id FK → student_profile.id (StudentInfoController resolves userId→profileId)
CREATE TABLE `student_profile` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL UNIQUE,
    `student_no` VARCHAR(20) NOT NULL UNIQUE COMMENT '学号',
    `name` VARCHAR(50) NOT NULL,
    `photo_url` VARCHAR(255) COMMENT '照片',
    `phone` VARCHAR(20),
    `id_card` VARCHAR(18) COMMENT '身份证号码',
    `college` VARCHAR(100) COMMENT '学院',
    `major` VARCHAR(100) COMMENT '专业',
    `grade` VARCHAR(20) COMMENT '年级',
    `class_name` VARCHAR(50) COMMENT '班级',
    `campus` VARCHAR(50) COMMENT '校区',
    `dormitory` VARCHAR(100) COMMENT '宿舍号',
    `off_campus_address` VARCHAR(255) COMMENT '非集中住宿地址',
    `medical_history` TEXT COMMENT '病史',
    `hobbies` VARCHAR(255) COMMENT '爱好',
    `personality` VARCHAR(255) COMMENT '性格特征',
    `career_goal` VARCHAR(500) COMMENT '职业目标',
    `advisor_name` VARCHAR(50) COMMENT '班主任姓名',
    `advisor_phone` VARCHAR(20) COMMENT '班主任联系方式',
    `difficulty_level` VARCHAR(20) COMMENT '困难认定等级',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_college` (`college`),
    INDEX `idx_major` (`major`),
    INDEX `idx_grade` (`grade`),
    FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `family_member` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `relation` VARCHAR(20) NOT NULL COMMENT '关系(父亲/母亲等)',
    `name` VARCHAR(50) NOT NULL,
    `phone` VARCHAR(20),
    `work_unit` VARCHAR(200) COMMENT '工作单位',
    `address` VARCHAR(255) COMMENT '住址',
    FOREIGN KEY (`student_id`) REFERENCES `student_profile`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `emergency_contact` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `name` VARCHAR(50) NOT NULL,
    `relation` VARCHAR(20) NOT NULL,
    `phone` VARCHAR(20) NOT NULL,
    `address` VARCHAR(255),
    FOREIGN KEY (`student_id`) REFERENCES `student_profile`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `supporting_document` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `doc_type` VARCHAR(50) NOT NULL COMMENT '材料类型',
    `file_path` VARCHAR(255) NOT NULL,
    `upload_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`student_id`) REFERENCES `student_profile`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `info_change_request` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `field_name` VARCHAR(50) NOT NULL,
    `old_value` VARCHAR(500),
    `new_value` VARCHAR(500) NOT NULL,
    `reason` VARCHAR(500),
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    `reviewer_id` BIGINT,
    `review_comment` VARCHAR(500),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `reviewed_at` DATETIME,
    FOREIGN KEY (`student_id`) REFERENCES `student_profile`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ==================== Module 2: Financial Aid ====================
-- student_id FK → sys_user.id (controllers pass userId directly)

CREATE TABLE `scholarship` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `type` VARCHAR(50) NOT NULL,
    `amount` DECIMAL(10,2),
    `quota` INT COMMENT '名额',
    `criteria` TEXT COMMENT '申请条件',
    `deadline` DATETIME,
    `publisher_id` BIGINT NOT NULL,
    `status` VARCHAR(20) DEFAULT 'OPEN' COMMENT 'OPEN/CLOSED',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_status` (`status`),
    INDEX `idx_deadline` (`deadline`)
) ENGINE=InnoDB;

CREATE TABLE `scholarship_application` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `scholarship_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `apply_reason` TEXT,
    `supporting_files` VARCHAR(500) COMMENT '证明材料JSON数组',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    `reviewer_id` BIGINT,
    `review_comment` VARCHAR(500),
    `apply_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `review_time` DATETIME,
    FOREIGN KEY (`scholarship_id`) REFERENCES `scholarship`(`id`),
    FOREIGN KEY (`student_id`) REFERENCES `sys_user`(`id`),
    INDEX `idx_student` (`student_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB;

CREATE TABLE `grant_info` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `amount` DECIMAL(10,2),
    `quota` INT COMMENT '名额',
    `criteria` TEXT,
    `deadline` DATETIME,
    `publisher_id` BIGINT NOT NULL,
    `status` VARCHAR(20) DEFAULT 'OPEN',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB;

CREATE TABLE `grant_application` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `grant_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `apply_reason` TEXT,
    `supporting_files` VARCHAR(500),
    `status` VARCHAR(20) DEFAULT 'PENDING',
    `reviewer_id` BIGINT,
    `review_comment` VARCHAR(500),
    `apply_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `review_time` DATETIME,
    FOREIGN KEY (`grant_id`) REFERENCES `grant_info`(`id`),
    FOREIGN KEY (`student_id`) REFERENCES `sys_user`(`id`),
    INDEX `idx_student` (`student_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB;

CREATE TABLE `work_study_job` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `requirements` TEXT,
    `salary` VARCHAR(100),
    `location` VARCHAR(100),
    `quota` INT DEFAULT 1,
    `enrolled_count` INT DEFAULT 0,
    `publisher_id` BIGINT NOT NULL,
    `status` VARCHAR(20) DEFAULT 'OPEN',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB;

CREATE TABLE `work_study_application` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `job_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `resume` VARCHAR(500),
    `status` VARCHAR(20) DEFAULT 'PENDING',
    `reviewer_id` BIGINT,
    `review_comment` VARCHAR(500),
    `review_time` DATETIME,
    `apply_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`job_id`) REFERENCES `work_study_job`(`id`),
    FOREIGN KEY (`student_id`) REFERENCES `sys_user`(`id`),
    INDEX `idx_student` (`student_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB;

-- ==================== Module 3: Academic Development ====================
-- student_id FK → sys_user.id (AcademicController passes userId directly as studentId)

CREATE TABLE `course` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `course_code` VARCHAR(20) NOT NULL UNIQUE,
    `course_name` VARCHAR(100) NOT NULL,
    `credit` DECIMAL(3,1),
    `semester` VARCHAR(20),
    `teacher_name` VARCHAR(50),
    `description` VARCHAR(500) COMMENT '课程简介',
    INDEX `idx_semester` (`semester`)
) ENGINE=InnoDB;

CREATE TABLE `grade` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `course_id` BIGINT NOT NULL,
    `score` DECIMAL(5,2),
    `gpa_point` DECIMAL(3,2),
    `semester` VARCHAR(20),
    `exam_type` VARCHAR(20) COMMENT '期末考试/补考/重修',
    FOREIGN KEY (`student_id`) REFERENCES `sys_user`(`id`),
    FOREIGN KEY (`course_id`) REFERENCES `course`(`id`),
    INDEX `idx_student_semester` (`student_id`, `semester`)
) ENGINE=InnoDB;

CREATE TABLE `award_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `award_name` VARCHAR(100) NOT NULL,
    `award_level` VARCHAR(30) COMMENT '国家级/省部级/校级/院级',
    `award_date` DATE,
    `issuer` VARCHAR(100),
    `certificate_file` VARCHAR(255),
    FOREIGN KEY (`student_id`) REFERENCES `sys_user`(`id`),
    INDEX `idx_student` (`student_id`)
) ENGINE=InnoDB;

CREATE TABLE `disciplinary_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `violation_type` VARCHAR(50) NOT NULL,
    `description` TEXT,
    `punishment` VARCHAR(100),
    `record_date` DATE,
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/RESCINDED',
    FOREIGN KEY (`student_id`) REFERENCES `sys_user`(`id`),
    INDEX `idx_student` (`student_id`)
) ENGINE=InnoDB;

CREATE TABLE `midterm_assessment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `semester` VARCHAR(20) NOT NULL,
    `self_summary` TEXT,
    `advisor_comment` TEXT,
    `score` DECIMAL(5,2),
    `status` VARCHAR(20) DEFAULT 'DRAFT',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`student_id`) REFERENCES `sys_user`(`id`),
    INDEX `idx_student_semester` (`student_id`, `semester`)
) ENGINE=InnoDB;

CREATE TABLE `second_classroom_score` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `category` VARCHAR(50) COMMENT '类别',
    `activity_name` VARCHAR(100),
    `score` DECIMAL(5,2),
    `record_date` DATE,
    `verifier_id` BIGINT,
    FOREIGN KEY (`student_id`) REFERENCES `sys_user`(`id`),
    INDEX `idx_student` (`student_id`)
) ENGINE=InnoDB;

CREATE TABLE `competition_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `competition_name` VARCHAR(100) NOT NULL,
    `level` VARCHAR(30) COMMENT '国家级/省部级/校级',
    `award` VARCHAR(50),
    `date` DATE,
    `certificate_file` VARCHAR(255),
    FOREIGN KEY (`student_id`) REFERENCES `sys_user`(`id`),
    INDEX `idx_student` (`student_id`)
) ENGINE=InnoDB;

-- ==================== Module 4: Career Planning ====================
-- student_id FK → sys_user.id (CareerController passes userId directly)

CREATE TABLE `job_info` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(100) NOT NULL,
    `company` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `requirements` TEXT,
    `salary_range` VARCHAR(50),
    `location` VARCHAR(100),
    `publisher_id` BIGINT NOT NULL,
    `status` VARCHAR(20) DEFAULT 'OPEN' COMMENT 'OPEN/CLOSED',
    `publish_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `deadline` DATETIME,
    INDEX `idx_status` (`status`),
    INDEX `idx_deadline` (`deadline`)
) ENGINE=InnoDB;

CREATE TABLE `job_subscription` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL UNIQUE,
    `keywords` VARCHAR(500) COMMENT 'JSON关键词',
    `industries` VARCHAR(500) COMMENT 'JSON行业',
    `locations` VARCHAR(500) COMMENT 'JSON地点',
    FOREIGN KEY (`student_id`) REFERENCES `sys_user`(`id`)
) ENGINE=InnoDB;

-- ==================== Module 5: Safety Management ====================
-- student_id/reporter_id FK → sys_user.id (SafetyController passes userId directly)

CREATE TABLE `late_return_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `date` DATE NOT NULL,
    `return_time` TIME NOT NULL,
    `reason` VARCHAR(500),
    `status` VARCHAR(20) DEFAULT 'REGISTERED' COMMENT 'REGISTERED/VERIFIED',
    `reviewer_id` BIGINT,
    `verified_at` DATETIME COMMENT '审核时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`student_id`) REFERENCES `sys_user`(`id`),
    INDEX `idx_student_date` (`student_id`, `date`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB;

CREATE TABLE `outing_report` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `destination` VARCHAR(200) NOT NULL,
    `departure_date` DATE NOT NULL,
    `return_date` DATE NOT NULL,
    `companion` VARCHAR(100),
    `reason` VARCHAR(500),
    `emergency_contact` VARCHAR(50),
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    `reviewer_id` BIGINT,
    `reviewed_at` DATETIME COMMENT '审核时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`student_id`) REFERENCES `sys_user`(`id`),
    INDEX `idx_student` (`student_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB;

CREATE TABLE `safety_report` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `reporter_id` BIGINT NOT NULL,
    `report_type` VARCHAR(30) NOT NULL COMMENT 'CAMPUS_HAZARD/PERSONAL_EMERGENCY/OTHER',
    `title` VARCHAR(200) NOT NULL,
    `description` TEXT,
    `location` VARCHAR(200),
    `images` VARCHAR(500),
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/HANDLING/RESOLVED',
    `handler_id` BIGINT,
    `handled_at` DATETIME,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`reporter_id`) REFERENCES `sys_user`(`id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_reporter` (`reporter_id`)
) ENGINE=InnoDB;

-- ==================== Module 6: Communication ====================
-- student_id FK → sys_user.id (CommunicationController passes userId directly)

CREATE TABLE `private_message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `sender_id` BIGINT NOT NULL,
    `receiver_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `is_read` TINYINT DEFAULT 0,
    `sent_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_sender_receiver` (`sender_id`, `receiver_id`),
    INDEX `idx_receiver_read` (`receiver_id`, `is_read`)
) ENGINE=InnoDB;

CREATE TABLE `class_notice` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `class_identifier` VARCHAR(50) NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT NOT NULL,
    `publisher_id` BIGINT NOT NULL,
    `is_pinned` TINYINT DEFAULT 0,
    `published_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_class_time` (`class_identifier`, `published_at`)
) ENGINE=InnoDB;

CREATE TABLE `feedback` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT NOT NULL,
    `type` VARCHAR(30) DEFAULT 'SUGGESTION' COMMENT 'SUGGESTION/COMPLAINT/OTHER',
    `is_anonymous` TINYINT DEFAULT 0,
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/REPLIED/CLOSED',
    `reply` TEXT,
    `replied_at` DATETIME,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `sys_user`(`id`),
    INDEX `idx_user` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB;

CREATE TABLE `activity_post` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT,
    `category` VARCHAR(50) COMMENT '学术交流/文体活动/志愿服务/其他',
    `organizer` VARCHAR(100),
    `location` VARCHAR(200),
    `event_time` DATETIME,
    `max_participants` INT,
    `current_participants` INT DEFAULT 0,
    `publisher_id` BIGINT NOT NULL,
    `participant_type` VARCHAR(20) DEFAULT 'STUDENT' COMMENT 'STUDENT/TEACHER/BOTH',
    `target_class` VARCHAR(50) COMMENT '仅班级活动时有值',
    `status` VARCHAR(20) DEFAULT 'OPEN' COMMENT 'OPEN/CLOSED/CANCELLED',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_status` (`status`),
    INDEX `idx_event_time` (`event_time`)
) ENGINE=InnoDB;

CREATE TABLE `activity_registration` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `activity_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `registered_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `status` VARCHAR(20) DEFAULT 'REGISTERED' COMMENT 'REGISTERED/SIGNED_IN/CANCELLED',
    FOREIGN KEY (`activity_id`) REFERENCES `activity_post`(`id`),
    FOREIGN KEY (`student_id`) REFERENCES `sys_user`(`id`),
    UNIQUE KEY `uk_activity_student` (`activity_id`, `student_id`)
) ENGINE=InnoDB;

-- ==================== Seed Data ====================
-- Default accounts (password for all: 123456)
-- BCrypt hash generated by Spring Security BCryptPasswordEncoder

INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `role`, `phone`, `email`, `avatar`, `status`) VALUES
(1, 'admin', '$2a$10$zbFZZG582SprT4ogMmDlm.wLOBpR9y7H/gRwlCDe3hbOdup1ClqYC', '系统管理员', 'ADMIN', '13800000000', 'admin@smartlearn.cn', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin', 1),
(2, 'teacher', '$2a$10$zbFZZG582SprT4ogMmDlm.wLOBpR9y7H/gRwlCDe3hbOdup1ClqYC', '张老师', 'TEACHER', '13800000001', 'teacher@smartlearn.cn', 'https://api.dicebear.com/7.x/avataaars/svg?seed=teacher', 1),
(3, 'student', '$2a$10$zbFZZG582SprT4ogMmDlm.wLOBpR9y7H/gRwlCDe3hbOdup1ClqYC', '李同学', 'STUDENT', '13800000002', 'student@smartlearn.cn', 'https://api.dicebear.com/7.x/avataaars/svg?seed=student', 1);

-- Student profiles for admin, teacher and student
INSERT INTO `student_profile` (`id`, `user_id`, `student_no`, `name`, `photo_url`, `phone`, `id_card`, `college`, `major`, `grade`, `class_name`, `campus`, `dormitory`, `off_campus_address`, `medical_history`, `hobbies`, `personality`, `career_goal`, `advisor_name`, `advisor_phone`, `difficulty_level`) VALUES
(1, 1, '202313840356', '系统管理员', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin', '13800000000', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(2, 2, '202313840357', '张老师', 'https://api.dicebear.com/7.x/avataaars/svg?seed=teacher', '13800000001', NULL, '计算机科学与技术学院', '软件工程', '2024级', NULL, '主校区', '教师公寓1栋', NULL, NULL, '阅读、羽毛球', '严谨认真、平易近人', '培养优秀计算机人才', NULL, NULL, NULL),
(3, 3, '202313840358', '李同学', 'https://api.dicebear.com/7.x/avataaars/svg?seed=student', '13800000002', '320102200001011234', '计算机科学与技术学院', '软件工程', '2024级', '软工2401班', '主校区', 'A区3栋501室', NULL, '无重大病史', '编程、篮球、阅读', '积极乐观、认真负责', '成为全栈开发工程师', '王导员', '13800000003', 'GENERAL');

-- Family members for student
INSERT INTO `family_member` (`id`, `student_id`, `relation`, `name`, `phone`, `work_unit`, `address`) VALUES
(1, 2, '父亲', '李明华', '13900001111', '华为技术有限公司', '广东省深圳市南山区科技园'),
(2, 2, '母亲', '陈秀英', '13900002222', '中国银行', '广东省深圳市福田区华强北路');

-- Emergency contacts for student
INSERT INTO `emergency_contact` (`id`, `student_id`, `name`, `relation`, `phone`, `address`) VALUES
(1, 2, '李明华', '父亲', '13900001111', '广东省深圳市南山区科技园'),
(2, 2, '张伟', '好友', '13900003333', '本校宿舍B区2栋302室');

-- Courses
INSERT INTO `course` (`id`, `course_code`, `course_name`, `credit`, `semester`, `teacher_name`, `description`) VALUES
(1, 'CS101', '高等数学', 5.0, '2025-2026-1', '王教授', '高等数学（上）'),
(2, 'CS102', '线性代数', 3.0, '2025-2026-1', '李教授', '线性代数基础'),
(3, 'CS103', '程序设计基础', 4.0, '2025-2026-1', '张老师', 'C语言程序设计'),
(4, 'CS104', '大学英语', 3.0, '2025-2026-1', '陈教授', '大学英语综合教程'),
(5, 'CS201', '数据结构', 4.0, '2025-2026-2', '赵教授', '数据结构与算法'),
(6, 'CS202', '计算机网络', 3.0, '2025-2026-2', '刘教授', '计算机网络原理'),
(7, 'CS203', '操作系统', 4.0, '2025-2026-2', '周教授', '操作系统原理'),
(8, 'CS204', '数据库原理', 3.0, '2025-2026-2', '吴教授', '数据库系统概论');
