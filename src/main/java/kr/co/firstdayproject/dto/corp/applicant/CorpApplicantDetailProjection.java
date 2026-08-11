package kr.co.firstdayproject.dto.corp.applicant;

import java.time.LocalDateTime;

public interface CorpApplicantDetailProjection {

    Long getApplicationId();

    Long getApplicantUserId();

    String getApplicantName();

    String getApplicantEmail();

    String getApplicantPhone();

    String getJobTitle();

    String getCurrentStatus();

    LocalDateTime getAppliedAt();

    String getResumeSnapshotJson();

    String getCoverLetterSnapshotJson();
}
