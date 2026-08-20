package kr.co.firstdayproject.dto.corp.dashboard;

import java.time.LocalDateTime;

public interface CorpDashboardApplicantProjection {
    Long getApplicationId();
    String getApplicantName();
    String getJobTitle();
    LocalDateTime getAppliedAt();
    String getCurrentStatus();
}
