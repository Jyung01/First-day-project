package kr.co.firstdayproject.dto.corp.applicant;

import java.time.LocalDateTime;

public interface CorpApplicantListProjection {

    Long getApplicationId();

    String getApplicantName();

    String getJobTitle();

    String getCurrentStatus();

    LocalDateTime getAppliedAt();

    String getResumeSnapshotJson();
}
