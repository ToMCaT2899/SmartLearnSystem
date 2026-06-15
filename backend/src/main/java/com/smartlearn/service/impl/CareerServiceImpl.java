package com.smartlearn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlearn.common.PageResult;
import com.smartlearn.entity.*;
import com.smartlearn.mapper.*;
import com.smartlearn.service.CareerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CareerServiceImpl implements CareerService {

    private final JobInfoMapper jobInfoMapper;
    private final JobSubscriptionMapper jobSubscriptionMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<JobInfo> getJobs(String keyword, String location, int page, int size) {
        Page<JobInfo> p = new Page<>(page, size);
        LambdaQueryWrapper<JobInfo> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(JobInfo::getTitle, keyword).or().like(JobInfo::getCompany, keyword));
        }
        if (location != null && !location.isBlank()) {
            wrapper.like(JobInfo::getLocation, location);
        }
        wrapper.orderByDesc(JobInfo::getPublishTime);
        IPage<JobInfo> result = jobInfoMapper.selectPage(p, wrapper);
        populatePublisherNames(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public JobInfo getJobById(Long id) {
        JobInfo job = jobInfoMapper.selectById(id);
        if (job == null) {
            throw new IllegalArgumentException("岗位不存在");
        }
        if (job.getPublisherId() != null) {
            SysUser publisher = sysUserMapper.selectById(job.getPublisherId());
            if (publisher != null) {
                job.setPublisherName(publisher.getRealName());
            }
        }
        return job;
    }

    private void populatePublisherNames(List<JobInfo> jobs) {
        List<Long> publisherIds = jobs.stream()
                .map(JobInfo::getPublisherId)
                .distinct()
                .collect(Collectors.toList());
        if (publisherIds.isEmpty()) return;
        List<SysUser> publishers = sysUserMapper.selectBatchIds(publisherIds);
        Map<Long, String> nameMap = publishers.stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));
        for (JobInfo job : jobs) {
            job.setPublisherName(nameMap.get(job.getPublisherId()));
        }
    }

    @Override
    public JobInfo createJob(JobInfo job) {
        if (job.getPublishTime() == null) {
            job.setPublishTime(LocalDateTime.now());
        }
        job.setStatus("OPEN");
        jobInfoMapper.insert(job);
        return job;
    }

    @Override
    public JobInfo updateJob(JobInfo job, Long userId) {
        JobInfo existing = jobInfoMapper.selectById(job.getId());
        if (existing == null) {
            throw new IllegalArgumentException("岗位不存在");
        }
        checkDeletePermission(existing.getPublisherId(), userId);
        jobInfoMapper.updateById(job);
        return job;
    }

    @Override
    public void deleteJob(Long id, Long userId) {
        JobInfo job = jobInfoMapper.selectById(id);
        if (job == null) {
            throw new IllegalArgumentException("岗位不存在");
        }
        checkDeletePermission(job.getPublisherId(), userId);
        jobInfoMapper.deleteById(id);
    }

    private void checkDeletePermission(Long publisherId, Long userId) {
        if (publisherId.equals(userId)) return;
        SysUser currentUser = sysUserMapper.selectById(userId);
        if (currentUser != null && "ADMIN".equals(currentUser.getRole())) return;
        SysUser publisher = sysUserMapper.selectById(publisherId);
        if (publisher != null && "ADMIN".equals(publisher.getRole())) {
            throw new IllegalArgumentException("不能删除管理员发布的内容");
        }
        throw new IllegalArgumentException("只能删除自己发布的内容");
    }

    @Override
    @Transactional
    public JobSubscription saveSubscription(Long studentId, JobSubscription subscription) {
        jobSubscriptionMapper.delete(
                new LambdaQueryWrapper<JobSubscription>()
                        .eq(JobSubscription::getStudentId, studentId)
        );
        subscription.setId(null);
        subscription.setStudentId(studentId);
        jobSubscriptionMapper.insert(subscription);
        return subscription;
    }

    @Override
    public JobSubscription getSubscription(Long studentId) {
        return jobSubscriptionMapper.selectOne(
                new LambdaQueryWrapper<JobSubscription>()
                        .eq(JobSubscription::getStudentId, studentId)
        );
    }

    @Override
    public void deleteSubscription(Long studentId) {
        jobSubscriptionMapper.delete(
                new LambdaQueryWrapper<JobSubscription>()
                        .eq(JobSubscription::getStudentId, studentId)
        );
    }
}
