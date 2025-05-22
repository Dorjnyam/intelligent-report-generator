package com.reportservice.infrastructure.adapter.out;

import com.reportservice.domain.model.GeneratedReport;
import com.reportservice.domain.port.out.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationAdapter implements NotificationPort {

    @Override
    public void notifyReportGenerated(GeneratedReport report) {
        log.info("Report generated successfully: {} - {}", report.getId(), report.getFileName());
        // In a real implementation, you might send emails, push notifications, etc.
        // For now, we'll just log the event
    }

    @Override
    public void notifyReportFailed(String requestId, String error) {
        log.error("Report generation failed for request {}: {}", requestId, error);
        // In a real implementation, you might send error notifications
    }
}