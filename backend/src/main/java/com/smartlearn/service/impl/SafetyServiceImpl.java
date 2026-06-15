package com.smartlearn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlearn.common.PageResult;
import com.smartlearn.entity.*;
import com.smartlearn.mapper.*;
import com.smartlearn.service.SafetyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SafetyServiceImpl implements SafetyService {

    private final LateReturnRecordMapper lateReturnRecordMapper;
    private final OutingReportMapper outingReportMapper;
    private final SafetyReportMapper safetyReportMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional
    public LateReturnRecord registerLateReturn(LateReturnRecord record) {
        record.setCreatedAt(LocalDateTime.now());
        record.setStatus("PENDING");
        lateReturnRecordMapper.insert(record);
        return record;
    }

    @Override
    public PageResult<LateReturnRecord> getLateReturns(Long studentId, int page, int size) {
        Page<LateReturnRecord> p = new Page<>(page, size);
        LambdaQueryWrapper<LateReturnRecord> wrapper = new LambdaQueryWrapper<LateReturnRecord>()
                .eq(LateReturnRecord::getStudentId, studentId)
                .orderByDesc(LateReturnRecord::getCreatedAt);
        IPage<LateReturnRecord> result = lateReturnRecordMapper.selectPage(p, wrapper);
        populateStudentNames(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public PageResult<LateReturnRecord> getPendingLateReturns(int page, int size) {
        Page<LateReturnRecord> p = new Page<>(page, size);
        LambdaQueryWrapper<LateReturnRecord> wrapper = new LambdaQueryWrapper<LateReturnRecord>()
                .orderByDesc(LateReturnRecord::getCreatedAt);
        IPage<LateReturnRecord> result = lateReturnRecordMapper.selectPage(p, wrapper);
        populateStudentNames(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    @Transactional
    public void verifyLateReturn(Long id, Long reviewerId, String status) {
        LateReturnRecord record = lateReturnRecordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("该记录不存在");
        }
        record.setStatus(status);
        record.setReviewerId(reviewerId);
        record.setVerifiedAt(LocalDateTime.now());
        lateReturnRecordMapper.updateById(record);
    }

    @Override
    @Transactional
    public void deleteLateReturn(Long id, Long userId) {
        LateReturnRecord record = lateReturnRecordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("该记录不存在");
        }
        if (!record.getStudentId().equals(userId)) {
            throw new IllegalArgumentException("只能删除自己的记录");
        }
        if (!"PENDING".equals(record.getStatus()) && !"CANCELLED".equals(record.getStatus())) {
            throw new IllegalArgumentException("已审核的记录无法删除，请先取消");
        }
        lateReturnRecordMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void updateLateReturn(LateReturnRecord record, Long userId) {
        LateReturnRecord existing = lateReturnRecordMapper.selectById(record.getId());
        if (existing == null) {
            throw new IllegalArgumentException("该记录不存在");
        }
        if (!existing.getStudentId().equals(userId)) {
            throw new IllegalArgumentException("只能修改自己的记录");
        }
        if (!"PENDING".equals(existing.getStatus())) {
            throw new IllegalArgumentException("已审核的记录无法修改");
        }
        record.setStudentId(existing.getStudentId());
        record.setStatus(existing.getStatus());
        record.setReviewerId(existing.getReviewerId());
        record.setVerifiedAt(existing.getVerifiedAt());
        record.setCreatedAt(existing.getCreatedAt());
        lateReturnRecordMapper.updateById(record);
    }

    @Override
    @Transactional
    public void cancelLateReturn(Long id, Long userId) {
        LateReturnRecord record = lateReturnRecordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("该记录不存在");
        }
        if (!record.getStudentId().equals(userId)) {
            throw new IllegalArgumentException("只能取消自己的记录");
        }
        if (!"PENDING".equals(record.getStatus())) {
            throw new IllegalArgumentException("只能取消待审核的记录");
        }
        record.setStatus("CANCELLED");
        lateReturnRecordMapper.updateById(record);
    }

    @Override
    @Transactional
    public OutingReport submitOutingReport(OutingReport report) {
        report.setCreatedAt(LocalDateTime.now());
        report.setStatus("PENDING");
        outingReportMapper.insert(report);
        return report;
    }

    @Override
    public PageResult<OutingReport> getOutingReports(Long studentId, int page, int size) {
        Page<OutingReport> p = new Page<>(page, size);
        LambdaQueryWrapper<OutingReport> wrapper = new LambdaQueryWrapper<OutingReport>()
                .eq(OutingReport::getStudentId, studentId)
                .orderByDesc(OutingReport::getCreatedAt);
        IPage<OutingReport> result = outingReportMapper.selectPage(p, wrapper);
        populateStudentNames(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public PageResult<OutingReport> getPendingOutingReports(int page, int size) {
        Page<OutingReport> p = new Page<>(page, size);
        LambdaQueryWrapper<OutingReport> wrapper = new LambdaQueryWrapper<OutingReport>()
                .orderByDesc(OutingReport::getCreatedAt);
        IPage<OutingReport> result = outingReportMapper.selectPage(p, wrapper);
        populateStudentNames(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    @Transactional
    public void reviewOutingReport(Long id, String status, Long reviewerId) {
        OutingReport report = outingReportMapper.selectById(id);
        if (report == null) {
            throw new IllegalArgumentException("该报告不存在");
        }
        report.setStatus(status);
        report.setReviewerId(reviewerId);
        report.setReviewedAt(LocalDateTime.now());
        outingReportMapper.updateById(report);
    }

    @Override
    @Transactional
    public void deleteOutingReport(Long id, Long userId) {
        OutingReport report = outingReportMapper.selectById(id);
        if (report == null) {
            throw new IllegalArgumentException("该报告不存在");
        }
        if (!report.getStudentId().equals(userId)) {
            throw new IllegalArgumentException("只能删除自己的报告");
        }
        if (!"PENDING".equals(report.getStatus()) && !"CANCELLED".equals(report.getStatus())) {
            throw new IllegalArgumentException("已审核的报告无法删除，请先取消");
        }
        outingReportMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void updateOutingReport(OutingReport report, Long userId) {
        OutingReport existing = outingReportMapper.selectById(report.getId());
        if (existing == null) {
            throw new IllegalArgumentException("该报告不存在");
        }
        if (!existing.getStudentId().equals(userId)) {
            throw new IllegalArgumentException("只能修改自己的报告");
        }
        if (!"PENDING".equals(existing.getStatus())) {
            throw new IllegalArgumentException("已审核的报告无法修改");
        }
        report.setStudentId(existing.getStudentId());
        report.setStatus(existing.getStatus());
        report.setReviewerId(existing.getReviewerId());
        report.setReviewedAt(existing.getReviewedAt());
        report.setCreatedAt(existing.getCreatedAt());
        outingReportMapper.updateById(report);
    }

    @Override
    @Transactional
    public void cancelOutingReport(Long id, Long userId) {
        OutingReport report = outingReportMapper.selectById(id);
        if (report == null) {
            throw new IllegalArgumentException("该报告不存在");
        }
        if (!report.getStudentId().equals(userId)) {
            throw new IllegalArgumentException("只能取消自己的报告");
        }
        if (!"PENDING".equals(report.getStatus())) {
            throw new IllegalArgumentException("只能取消待审核的报告");
        }
        report.setStatus("CANCELLED");
        outingReportMapper.updateById(report);
    }

    private void populateStudentNames(List<?> records) {
        Set<Long> ids = new java.util.HashSet<>();
        for (Object obj : records) {
            if (obj instanceof LateReturnRecord) {
                LateReturnRecord r = (LateReturnRecord) obj;
                if (r.getStudentId() != null) ids.add(r.getStudentId());
                if (r.getReviewerId() != null) ids.add(r.getReviewerId());
            } else if (obj instanceof OutingReport) {
                OutingReport r = (OutingReport) obj;
                if (r.getStudentId() != null) ids.add(r.getStudentId());
                if (r.getReviewerId() != null) ids.add(r.getReviewerId());
            }
        }
        if (ids.isEmpty()) return;
        List<SysUser> users = sysUserMapper.selectBatchIds(new java.util.ArrayList<>(ids));
        Map<Long, SysUser> userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        for (Object obj : records) {
            SysUser s = null, r = null;
            if (obj instanceof LateReturnRecord) {
                LateReturnRecord lr = (LateReturnRecord) obj;
                s = userMap.get(lr.getStudentId());
                r = userMap.get(lr.getReviewerId());
                if (s != null) { lr.setStudentName(s.getRealName()); lr.setStudentNo(s.getUsername()); }
                if (r != null) lr.setReviewerName(r.getRealName());
            } else if (obj instanceof OutingReport) {
                OutingReport or_ = (OutingReport) obj;
                s = userMap.get(or_.getStudentId());
                r = userMap.get(or_.getReviewerId());
                if (s != null) { or_.setStudentName(s.getRealName()); or_.setStudentNo(s.getUsername()); }
                if (r != null) or_.setReviewerName(r.getRealName());
            }
        }
    }

    private void populateReporterNames(List<SafetyReport> reports) {
        Set<Long> ids = new java.util.HashSet<>();
        for (SafetyReport r : reports) {
            if (r.getReporterId() != null) ids.add(r.getReporterId());
            if (r.getHandlerId() != null) ids.add(r.getHandlerId());
        }
        if (ids.isEmpty()) return;
        List<SysUser> users = sysUserMapper.selectBatchIds(new java.util.ArrayList<>(ids));
        Map<Long, SysUser> userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        for (SafetyReport r : reports) {
            SysUser u = userMap.get(r.getReporterId());
            if (u != null) { r.setReporterName(u.getRealName()); r.setReporterNo(u.getUsername()); }
            SysUser h = userMap.get(r.getHandlerId());
            if (h != null) r.setHandlerName(h.getRealName());
        }
    }

    @Override
    @Transactional
    public SafetyReport submitSafetyReport(SafetyReport report) {
        report.setCreatedAt(LocalDateTime.now());
        report.setStatus("PENDING");
        safetyReportMapper.insert(report);
        return report;
    }

    @Override
    public PageResult<SafetyReport> getSafetyReports(Long reporterId, int page, int size) {
        Page<SafetyReport> p = new Page<>(page, size);
        LambdaQueryWrapper<SafetyReport> wrapper = new LambdaQueryWrapper<SafetyReport>()
                .eq(SafetyReport::getReporterId, reporterId)
                .orderByDesc(SafetyReport::getCreatedAt);
        IPage<SafetyReport> result = safetyReportMapper.selectPage(p, wrapper);
        populateReporterNames(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public PageResult<SafetyReport> getPendingSafetyReports(int page, int size) {
        Page<SafetyReport> p = new Page<>(page, size);
        LambdaQueryWrapper<SafetyReport> wrapper = new LambdaQueryWrapper<SafetyReport>()
                .orderByDesc(SafetyReport::getCreatedAt);
        IPage<SafetyReport> result = safetyReportMapper.selectPage(p, wrapper);
        populateReporterNames(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    @Transactional
    public void handleSafetyReport(Long id, Long handlerId, String status) {
        SafetyReport report = safetyReportMapper.selectById(id);
        if (report == null) {
            throw new IllegalArgumentException("该报告不存在");
        }
        report.setStatus(status);
        report.setHandlerId(handlerId);
        report.setHandledAt(LocalDateTime.now());
        safetyReportMapper.updateById(report);
    }

    @Override
    @Transactional
    public void deleteSafetyReport(Long id, Long userId) {
        SafetyReport report = safetyReportMapper.selectById(id);
        if (report == null) {
            throw new IllegalArgumentException("该报告不存在");
        }
        if (!report.getReporterId().equals(userId)) {
            throw new IllegalArgumentException("只能删除自己的报告");
        }
        if (!"PENDING".equals(report.getStatus()) && !"CANCELLED".equals(report.getStatus())) {
            throw new IllegalArgumentException("已处理的报告无法删除，请先取消");
        }
        safetyReportMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void updateSafetyReport(SafetyReport report, Long userId) {
        SafetyReport existing = safetyReportMapper.selectById(report.getId());
        if (existing == null) {
            throw new IllegalArgumentException("该报告不存在");
        }
        if (!existing.getReporterId().equals(userId)) {
            throw new IllegalArgumentException("只能修改自己的报告");
        }
        if (!"PENDING".equals(existing.getStatus())) {
            throw new IllegalArgumentException("已处理的报告无法修改");
        }
        report.setReporterId(existing.getReporterId());
        report.setStatus(existing.getStatus());
        report.setHandlerId(existing.getHandlerId());
        report.setHandledAt(existing.getHandledAt());
        report.setCreatedAt(existing.getCreatedAt());
        safetyReportMapper.updateById(report);
    }

    @Override
    @Transactional
    public void cancelSafetyReport(Long id, Long userId) {
        SafetyReport report = safetyReportMapper.selectById(id);
        if (report == null) {
            throw new IllegalArgumentException("该报告不存在");
        }
        if (!report.getReporterId().equals(userId)) {
            throw new IllegalArgumentException("只能取消自己的报告");
        }
        if (!"PENDING".equals(report.getStatus())) {
            throw new IllegalArgumentException("只能取消待处理的报告");
        }
        report.setStatus("CANCELLED");
        safetyReportMapper.updateById(report);
    }
}
