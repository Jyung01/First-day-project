package kr.co.firstdayproject.dto.my;

import java.time.LocalDateTime;

public interface MyApplicationDetailProjection {

    Long getApplicationId();
    Long getJobPostingId();
    String getCompanyName();
    String getCompanyLogoUrl();
    String getJobTitle();
    String getCurrentStatus();
    LocalDateTime getAppliedAt();
    String getResumeSnapshotJson();
    String getCoverLetterSnapshotJson();
}
