package kr.co.firstdayproject.dto.admin;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class InquiryDetailDto {
    private Long inquiryId;
    private String categoryName;
    private String title;
    private String content;
    private String writerLabel;
    private String createdAt;
    private String status;
    private String statusLabel;
    private String answerContent; // null이면 미답변
    private String answeredAt;
    private List<Attachment> attachments;

    @Getter
    @Builder
    public static class Attachment {
        private Long attachmentId;
        private String originalName;
        private Long fileSize;
    }
}