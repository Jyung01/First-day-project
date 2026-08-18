package kr.co.firstdayproject.dto.my;

import java.time.LocalDateTime;

public interface MyApplicationListProjection {

    Long getApplicationId();

    String getCompanyName();

    String getCompanyLogoUrl();

    String getCompanyStatus();

    String getJobTitle();

    String getCurrentStatus();

    LocalDateTime getAppliedAt();

    LocalDateTime getLatestChangedAt();
}
