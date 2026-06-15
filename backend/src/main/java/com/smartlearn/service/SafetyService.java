package com.smartlearn.service;

import com.smartlearn.common.PageResult;
import com.smartlearn.entity.*;

public interface SafetyService {

    // Late return
    LateReturnRecord registerLateReturn(LateReturnRecord record);

    PageResult<LateReturnRecord> getLateReturns(Long studentId, int page, int size);

    PageResult<LateReturnRecord> getPendingLateReturns(int page, int size);

    void verifyLateReturn(Long id, Long reviewerId, String status);

    void deleteLateReturn(Long id, Long userId);

    void updateLateReturn(LateReturnRecord record, Long userId);

    void cancelLateReturn(Long id, Long userId);

    // Outing report
    OutingReport submitOutingReport(OutingReport report);

    PageResult<OutingReport> getOutingReports(Long studentId, int page, int size);

    PageResult<OutingReport> getPendingOutingReports(int page, int size);

    void reviewOutingReport(Long id, String status, Long reviewerId);

    void deleteOutingReport(Long id, Long userId);

    void updateOutingReport(OutingReport report, Long userId);

    void cancelOutingReport(Long id, Long userId);

    // Safety report
    SafetyReport submitSafetyReport(SafetyReport report);

    PageResult<SafetyReport> getSafetyReports(Long reporterId, int page, int size);

    PageResult<SafetyReport> getPendingSafetyReports(int page, int size);

    void handleSafetyReport(Long id, Long handlerId, String status);

    void deleteSafetyReport(Long id, Long userId);

    void updateSafetyReport(SafetyReport report, Long userId);

    void cancelSafetyReport(Long id, Long userId);
}
