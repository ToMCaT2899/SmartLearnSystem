package com.smartlearn.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartlearn.entity.*;
import com.smartlearn.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final CourseMapper courseMapper;
    private final ScholarshipMapper scholarshipMapper;
    private final GrantInfoMapper grantInfoMapper;
    private final WorkStudyJobMapper workStudyJobMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        initAdmin();
        initTeacher();
        Long studentUserId = initStudent();
        initCourses();
        initScholarships();
        initGrants();
        initWorkStudyJobs();
    }

    private void initAdmin() {
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "admin")
        );
        SysUser admin;
        if (count == 0) {
            admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setRealName("系统管理员");
            admin.setRole("ADMIN");
            admin.setPhone("13800000000");
            admin.setEmail("admin@smartlearn.cn");
            admin.setAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=admin");
            admin.setStatus(1);
            sysUserMapper.insert(admin);
            log.info("默认管理员账号已创建: admin / 123456");
        } else {
            admin = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "admin")
            );
            if (admin != null && !passwordEncoder.matches("123456", admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode("123456"));
                sysUserMapper.updateById(admin);
                log.info("管理员密码已重置为: 123456");
            }
        }

        // Ensure admin has a student_profile (required by student info module)
        if (admin != null) {
            Long profileCount = studentProfileMapper.selectCount(
                    new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, admin.getId())
            );
            if (profileCount == 0) {
                StudentProfile profile = new StudentProfile();
                profile.setUserId(admin.getId());
                profile.setName("系统管理员");
                profile.setPhone("13800000000");
                profile.setStudentNo("ADM20240001");
                studentProfileMapper.insert(profile);
                log.info("管理员档案已创建，工号: ADM20240001");
            }
        }
    }

    private void initTeacher() {
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "teacher")
        );
        if (count == 0) {
            SysUser teacher = new SysUser();
            teacher.setUsername("teacher");
            teacher.setPassword(passwordEncoder.encode("123456"));
            teacher.setRealName("张老师");
            teacher.setRole("TEACHER");
            teacher.setPhone("13800000001");
            teacher.setEmail("teacher@smartlearn.cn");
            teacher.setAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=teacher");
            teacher.setStatus(1);
            sysUserMapper.insert(teacher);

            // Create student_profile for teacher as well (required by student info module)
            StudentProfile profile = new StudentProfile();
            profile.setUserId(teacher.getId());
            profile.setName("张老师");
            profile.setPhone("13800000001");
            profile.setStudentNo("TCH20240001");
            profile.setCollege("计算机科学与技术学院");
            profile.setMajor("软件工程");
            profile.setGrade("2024级");
            profile.setCampus("主校区");
            profile.setDormitory("教师公寓1栋");
            profile.setHobbies("阅读、羽毛球");
            profile.setPersonality("严谨认真、平易近人");
            profile.setCareerGoal("培养优秀计算机人才");
            studentProfileMapper.insert(profile);

            log.info("默认教师账号已创建: teacher / 123456，工号: TCH20240001");
        }
    }

    private Long initStudent() {
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "student")
        );
        if (count > 0) {
            SysUser student = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "student")
            );
            return student != null ? student.getId() : null;
        }

        SysUser student = new SysUser();
        student.setUsername("student");
        student.setPassword(passwordEncoder.encode("123456"));
        student.setRealName("李同学");
        student.setRole("STUDENT");
        student.setPhone("13800000002");
        student.setEmail("student@smartlearn.cn");
        student.setAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=student");
        student.setStatus(1);
        sysUserMapper.insert(student);

        StudentProfile profile = new StudentProfile();
        profile.setUserId(student.getId());
        profile.setStudentNo("20240001");
        profile.setName("李同学");
        profile.setPhone("13800000002");
        profile.setIdCard("320102200001011234");
        profile.setCollege("计算机科学与技术学院");
        profile.setMajor("软件工程");
        profile.setGrade("2024级");
        profile.setClassName("软工2401班");
        profile.setCampus("主校区");
        profile.setDormitory("A区3栋501室");
        profile.setMedicalHistory("无重大病史");
        profile.setHobbies("编程、篮球、阅读");
        profile.setPersonality("积极乐观、认真负责");
        profile.setCareerGoal("成为全栈开发工程师");
        profile.setAdvisorName("王导员");
        profile.setAdvisorPhone("13800000003");
        profile.setDifficultyLevel("GENERAL");
        studentProfileMapper.insert(profile);

        log.info("默认学生账号已创建: student / 123456，学号: 20240001");
        return student.getId();
    }

    private void initCourses() {
        Long count = courseMapper.selectCount(null);
        if (count > 0) return;

        String[][] data = {
                {"CS101", "数据结构与算法", "4.0", "2024-2025-1", "陈教授", "学习线性表、树、图等基本数据结构及常用算法"},
                {"CS102", "操作系统原理", "3.5", "2024-2025-1", "刘教授", "掌握进程管理、内存管理、文件系统等操作系统核心知识"},
                {"CS103", "计算机网络", "3.5", "2024-2025-2", "赵教授", "理解TCP/IP协议栈、路由算法、网络安全等网络基础"},
                {"CS104", "数据库系统概论", "3.0", "2024-2025-2", "周教授", "学习关系数据库理论、SQL语言及数据库设计方法"},
                {"MATH101", "高等数学（上）", "5.0", "2024-2025-1", "吴教授", "涵盖函数极限、微积分等高等数学基础内容"},
                {"MATH102", "线性代数", "3.0", "2024-2025-1", "郑教授", "矩阵运算、线性空间、特征值与特征向量等"},
                {"ENG101", "大学英语（一）", "2.0", "2024-2025-1", "冯教授", "提升英语听说读写综合能力"},
                {"CS201", "Java程序设计", "3.5", "2024-2025-2", "陈教授", "学习Java语言基础、面向对象编程及常用框架"},
        };
        for (String[] row : data) {
            Course course = new Course();
            course.setCourseCode(row[0]);
            course.setCourseName(row[1]);
            course.setCredit(new BigDecimal(row[2]));
            course.setSemester(row[3]);
            course.setTeacherName(row[4]);
            course.setDescription(row[5]);
            courseMapper.insert(course);
        }
        log.info("初始化了 {} 门示例课程", data.length);
    }

    private void initScholarships() {
        Long count = scholarshipMapper.selectCount(null);
        if (count > 0) return;

        SysUser teacher = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "teacher"));
        Long publisherId = teacher != null ? teacher.getId() : 1L;

        String[][] data = {
                {"国家奖学金", "国家奖学金", "8000", "5", "学业成绩排名前5%，综合素质优秀", "2026-06-30"},
                {"校级一等奖学金", "校级奖学金", "3000", "20", "学年成绩排名前15%", "2026-07-15"},
                {"企业奖学金-华为ICT", "企业奖学金", "5000", "3", "计算机相关专业，成绩优异", "2026-06-15"},
        };
        for (String[] row : data) {
            Scholarship s = new Scholarship();
            s.setName(row[0]);
            s.setType(row[1]);
            s.setAmount(new BigDecimal(row[2]));
            s.setQuota(Integer.parseInt(row[3]));
            s.setCriteria(row[4]);
            s.setDeadline(LocalDate.parse(row[5]).atStartOfDay());
            s.setPublisherId(publisherId);
            s.setStatus("OPEN");
            s.setCreatedAt(LocalDateTime.now());
            scholarshipMapper.insert(s);
        }
        log.info("初始化了 {} 条奖学金数据", data.length);
    }

    private void initGrants() {
        Long count = grantInfoMapper.selectCount(null);
        if (count > 0) return;

        SysUser teacher = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "teacher"));
        Long publisherId = teacher != null ? teacher.getId() : 1L;

        String[][] data = {
                {"国家助学金", "3000", "10", "家庭经济困难，品学兼优", "2026-06-30"},
                {"困难补助", "1500", null, "家庭突发困难或特殊经济需求", "2026-07-15"},
                {"勤勉助学金", "2000", "15", "学习努力，生活简朴", "2026-07-01"},
        };
        for (String[] row : data) {
            GrantInfo g = new GrantInfo();
            g.setName(row[0]);
            g.setAmount(new BigDecimal(row[1]));
            g.setQuota(row[2] != null ? Integer.parseInt(row[2]) : null);
            g.setCriteria(row[3]);
            g.setDeadline(LocalDate.parse(row[4]).atStartOfDay());
            g.setPublisherId(publisherId);
            g.setStatus("OPEN");
            g.setCreatedAt(LocalDateTime.now());
            grantInfoMapper.insert(g);
        }
        log.info("初始化了 {} 条助学金数据", data.length);
    }

    private void initWorkStudyJobs() {
        Long count = workStudyJobMapper.selectCount(null);
        if (count > 0) return;

        SysUser teacher = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "teacher"));
        Long publisherId = teacher != null ? teacher.getId() : 1L;

        String[][] data = {
                {"图书馆管理员", "负责图书整理、借还登记等工作", "细心认真，课余时间充足", "15元/小时", "图书馆", "3"},
                {"实验室助教", "协助实验课准备、设备维护", "相关专业，动手能力强", "20元/小时", "实验楼302", "2"},
                {"校园新媒体运营", "负责学校公众号内容编辑和排版", "有写作功底，熟悉公众号操作", "18元/小时", "行政楼105", "2"},
        };
        for (String[] row : data) {
            WorkStudyJob job = new WorkStudyJob();
            job.setTitle(row[0]);
            job.setDescription(row[1]);
            job.setRequirements(row[2]);
            job.setSalary(row[3]);
            job.setLocation(row[4]);
            job.setQuota(Integer.parseInt(row[5]));
            job.setEnrolledCount(0);
            job.setPublisherId(publisherId);
            job.setStatus("OPEN");
            job.setCreatedAt(LocalDateTime.now());
            workStudyJobMapper.insert(job);
        }
        log.info("初始化了 {} 条勤工助学岗位数据", data.length);
    }
}
