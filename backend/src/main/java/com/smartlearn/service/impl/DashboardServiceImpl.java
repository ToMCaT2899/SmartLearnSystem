package com.smartlearn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartlearn.entity.*;
import com.smartlearn.mapper.*;
import com.smartlearn.service.DashboardService;
import com.smartlearn.vo.DashboardVO;
import com.smartlearn.vo.DashboardVO.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final SysUserMapper sysUserMapper;
    private final ScholarshipApplicationMapper scholarshipApplicationMapper;
    private final GrantApplicationMapper grantApplicationMapper;
    private final InfoChangeRequestMapper infoChangeRequestMapper;
    private final FeedbackMapper feedbackMapper;
    private final PrivateMessageMapper privateMessageMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final ActivityRegistrationMapper activityRegistrationMapper;
    private final GradeMapper gradeMapper;

    private static final Map<String, String> STAT_GRADIENTS = Map.of(
            "blue", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
            "pink", "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)",
            "cyan", "linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)",
            "green", "linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)"
    );

    @Override
    public DashboardVO getDashboard(Long userId, String role, String period) {
        boolean isTeacherOrAdmin = "TEACHER".equals(role) || "ADMIN".equals(role);
        return DashboardVO.builder()
                .stats(buildStats(userId, isTeacherOrAdmin))
                .barChart(buildBarChart(period))
                .pieChart(buildPieChart(userId, isTeacherOrAdmin))
                .build();
    }

    private List<StatCardVO> buildStats(Long userId, boolean isTeacherOrAdmin) {
        if (isTeacherOrAdmin) {
            return buildTeacherAdminStats();
        }
        return buildStudentStats(userId);
    }

    private List<StatCardVO> buildTeacherAdminStats() {
        long studentCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "STUDENT").eq(SysUser::getStatus, 1));

        long pendingScholarship = scholarshipApplicationMapper.selectCount(
                new LambdaQueryWrapper<ScholarshipApplication>().eq(ScholarshipApplication::getStatus, "PENDING"));
        long pendingGrant = grantApplicationMapper.selectCount(
                new LambdaQueryWrapper<GrantApplication>().eq(GrantApplication::getStatus, "PENDING"));
        long pendingInfoChange = infoChangeRequestMapper.selectCount(
                new LambdaQueryWrapper<InfoChangeRequest>().eq(InfoChangeRequest::getStatus, "PENDING"));
        long pendingTotal = pendingScholarship + pendingGrant + pendingInfoChange;

        long unreadFeedback = feedbackMapper.selectCount(
                new LambdaQueryWrapper<Feedback>().eq(Feedback::getStatus, "PENDING"));

        long classCount = studentProfileMapper.selectList(null).stream()
                .map(StudentProfile::getClassName)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        return List.of(
                StatCardVO.builder().title("学生总数").value(String.valueOf(studentCount))
                        .icon("User").gradient(STAT_GRADIENTS.get("blue")).build(),
                StatCardVO.builder().title("待审核申请").value(String.valueOf(pendingTotal))
                        .icon("Document").gradient(STAT_GRADIENTS.get("pink")).build(),
                StatCardVO.builder().title("未读反馈").value(String.valueOf(unreadFeedback))
                        .icon("ChatDotRound").gradient(STAT_GRADIENTS.get("cyan")).build(),
                StatCardVO.builder().title("活跃班级").value(String.valueOf(classCount))
                        .icon("School").gradient(STAT_GRADIENTS.get("green")).build()
        );
    }

    private List<StatCardVO> buildStudentStats(Long userId) {
        String currentSemester = getCurrentSemester();
        List<Grade> grades = gradeMapper.selectList(
                new LambdaQueryWrapper<Grade>()
                        .eq(Grade::getStudentId, userId)
                        .eq(Grade::getSemester, currentSemester));

        String avgScore = "--";
        if (!grades.isEmpty()) {
            BigDecimal avg = grades.stream()
                    .map(Grade::getScore)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(grades.stream().filter(g -> g.getScore() != null).count()), 1, RoundingMode.HALF_UP);
            avgScore = avg.toPlainString();
        }

        long scholarshipCount = scholarshipApplicationMapper.selectCount(
                new LambdaQueryWrapper<ScholarshipApplication>().eq(ScholarshipApplication::getStudentId, userId));

        long unreadMsg = privateMessageMapper.selectCount(
                new LambdaQueryWrapper<PrivateMessage>()
                        .eq(PrivateMessage::getReceiverId, userId)
                        .eq(PrivateMessage::getIsRead, 0));

        long courseCount = gradeMapper.selectCount(
                new LambdaQueryWrapper<Grade>()
                        .eq(Grade::getStudentId, userId)
                        .eq(Grade::getSemester, currentSemester));

        return List.of(
                StatCardVO.builder().title("本学期成绩均分").value(avgScore)
                        .icon("Document").gradient(STAT_GRADIENTS.get("blue")).build(),
                StatCardVO.builder().title("奖学金申请").value(String.valueOf(scholarshipCount))
                        .icon("Money").gradient(STAT_GRADIENTS.get("pink")).build(),
                StatCardVO.builder().title("未读消息").value(String.valueOf(unreadMsg))
                        .icon("ChatDotRound").gradient(STAT_GRADIENTS.get("cyan")).build(),
                StatCardVO.builder().title("本学期课程").value(String.valueOf(courseCount))
                        .icon("School").gradient(STAT_GRADIENTS.get("green")).build()
        );
    }

    private ChartDataVO buildBarChart(String period) {
        LocalDate now = LocalDate.now();
        LocalDate start;
        List<String> labels;
        int buckets;

        switch (period != null ? period : "week") {
            case "month":
                start = now.minusWeeks(4);
                labels = List.of("第1周", "第2周", "第3周", "第4周");
                buckets = 4;
                break;
            case "semester":
                start = now.minusMonths(6);
                labels = new ArrayList<>();
                for (int i = 5; i >= 0; i--) {
                    labels.add(now.minusMonths(i).getMonthValue() + "月");
                }
                buckets = 6;
                break;
            default: // week
                start = now.minusDays(6);
                String[] dayNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
                labels = Arrays.asList(dayNames);
                buckets = 7;
                break;
        }

        List<ActivityRegistration> registrations = activityRegistrationMapper.selectList(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .ge(ActivityRegistration::getRegisteredAt, start.atStartOfDay()));

        int[] values = new int[buckets];
        for (ActivityRegistration reg : registrations) {
            if (reg.getRegisteredAt() == null) continue;
            LocalDate regDate = reg.getRegisteredAt().toLocalDate();
            int idx;
            if ("week".equals(period)) {
                idx = regDate.getDayOfWeek().getValue() - 1;
            } else if ("month".equals(period)) {
                long daysBetween = regDate.toEpochDay() - start.toEpochDay();
                idx = Math.min((int) (daysBetween / 7), buckets - 1);
            } else {
                int monthDiff = (now.getYear() - regDate.getYear()) * 12 + (now.getMonthValue() - regDate.getMonthValue());
                idx = Math.min(Math.max(buckets - 1 - monthDiff, 0), buckets - 1);
            }
            if (idx >= 0 && idx < buckets) {
                values[idx]++;
            }
        }

        List<Integer> valList = Arrays.stream(values).boxed().collect(Collectors.toList());
        return ChartDataVO.builder().days(labels).vals(valList).build();
    }

    private List<PieItemVO> buildPieChart(Long userId, boolean isTeacherOrAdmin) {
        if (isTeacherOrAdmin) {
            List<StudentProfile> profiles = studentProfileMapper.selectList(null);
            Map<String, Integer> collegeCount = new LinkedHashMap<>();
            for (StudentProfile p : profiles) {
                String college = p.getCollege() != null ? p.getCollege() : "未分配学院";
                collegeCount.merge(college, 1, Integer::sum);
            }
            return collegeCount.entrySet().stream()
                    .map(e -> PieItemVO.builder().name(e.getKey()).value(e.getValue()).build())
                    .collect(Collectors.toList());
        }

        List<Grade> grades = gradeMapper.selectList(
                new LambdaQueryWrapper<Grade>()
                        .eq(Grade::getStudentId, userId)
                        .eq(Grade::getSemester, getCurrentSemester()));
        Map<String, Integer> dist = new LinkedHashMap<>();
        dist.put("优秀(90-100)", 0);
        dist.put("良好(80-89)", 0);
        dist.put("中等(70-79)", 0);
        dist.put("及格(60-69)", 0);
        dist.put("不及格(<60)", 0);

        for (Grade g : grades) {
            if (g.getScore() == null) continue;
            double s = g.getScore().doubleValue();
            if (s >= 90) dist.merge("优秀(90-100)", 1, Integer::sum);
            else if (s >= 80) dist.merge("良好(80-89)", 1, Integer::sum);
            else if (s >= 70) dist.merge("中等(70-79)", 1, Integer::sum);
            else if (s >= 60) dist.merge("及格(60-69)", 1, Integer::sum);
            else dist.merge("不及格(<60)", 1, Integer::sum);
        }

        return dist.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> PieItemVO.builder().name(e.getKey()).value(e.getValue()).build())
                .collect(Collectors.toList());
    }

    private String getCurrentSemester() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        if (month >= 9) {
            return year + "-" + (year + 1) + "-1";
        } else if (month >= 2) {
            return (year - 1) + "-" + year + "-2";
        } else {
            return (year - 1) + "-" + year + "-1";
        }
    }
}
