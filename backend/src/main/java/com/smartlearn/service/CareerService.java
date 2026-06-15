package com.smartlearn.service;

import com.smartlearn.common.PageResult;
import com.smartlearn.entity.*;

public interface CareerService {

    PageResult<JobInfo> getJobs(String keyword, String location, int page, int size);

    JobInfo getJobById(Long id);

    JobInfo createJob(JobInfo job);

    JobInfo updateJob(JobInfo job, Long userId);

    void deleteJob(Long id, Long userId);

    JobSubscription saveSubscription(Long studentId, JobSubscription subscription);

    JobSubscription getSubscription(Long studentId);

    void deleteSubscription(Long studentId);
}
