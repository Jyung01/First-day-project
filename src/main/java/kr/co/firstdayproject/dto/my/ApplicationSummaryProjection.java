package kr.co.firstdayproject.dto.my;

import java.time.LocalDateTime;

public interface ApplicationSummaryProjection {

    Long getApplicationId();

    String getCompanyName();

    String getJobTitle();

    String getCurrentStatus();

    LocalDateTime getAppliedAt();
}
