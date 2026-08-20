package kr.co.firstdayproject.dto.admin;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminReportDTO {
    private Long reportId;
    private String targetType;
    private Long targetId;
    private String targetName;
    private String reasonCode;
    private String detail;
    private String reporterName;
    private String status;
    private String resolutionAction;
    private String resolutionNote;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;
}
