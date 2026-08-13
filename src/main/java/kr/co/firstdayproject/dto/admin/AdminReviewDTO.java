package kr.co.firstdayproject.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdminReviewDTO {
    private String reviewType;
    private Long reviewId;
    private Long companyId;
    private Long authorUserId;
    private String companyName;
    private String authorName;
    private String title;
    private String content;
    private String secondaryContent;
    private BigDecimal rating;
    private String interviewType;
    private String interviewResult;
    private String difficulty;
    private String status;
    private String hiddenReason;
    private String adminMemo;
    private Integer reportCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
