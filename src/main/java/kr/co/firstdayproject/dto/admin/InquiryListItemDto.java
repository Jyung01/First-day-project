package kr.co.firstdayproject.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InquiryListItemDto {
    private Long inquiryId;
    private String categoryName;
    private String title;
    private String writerLabel;
    private String status;      // PENDING / ANSWERED
    private String statusLabel; // 미답변 / 답변완료
    private String createdAt;
}