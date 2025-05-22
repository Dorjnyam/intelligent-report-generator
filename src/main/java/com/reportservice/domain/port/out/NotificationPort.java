package com.reportservice.domain.port.out;

import com.reportservice.domain.model.GeneratedReport;

public interface NotificationPort {
    void notifyReportGenerated(GeneratedReport report);
    void notifyReportFailed(String requestId, String error);
}