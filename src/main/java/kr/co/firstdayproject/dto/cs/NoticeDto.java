package kr.co.firstdayproject.dto.cs;

import kr.co.firstdayproject.entity.cs.Notice;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NoticeDto {

    // 목록 한 행
    @Getter
    @Builder
    public static class ListItem {
        private Long noticeId;
        private String title;
        private Boolean isPinned;
        private String status;
        private String createdAtText;
        private String updatedAtText;

        public static ListItem from(Notice n) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd");
            return ListItem.builder()
                    .noticeId(n.getNoticeId())
                    .title(n.getTitle())
                    .isPinned(n.getIsPinned())
                    .status(n.getStatus())
                    .createdAtText(n.getCreatedAt() != null ? n.getCreatedAt().format(fmt) : "")
                    .updatedAtText(n.getUpdatedAt() != null ? n.getUpdatedAt().format(fmt) : "")
                    .build();
        }
    }

    // 상세 (관리자 모달 프리필 + 사용자 상세화면 공용, JSON 직렬화 가능해야 함)
    @Getter
    @Builder
    public static class Detail {
        private Long noticeId;
        private String title;
        private String content;
        private Boolean isPinned;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Detail from(Notice n) {
            return Detail.builder()
                    .noticeId(n.getNoticeId())
                    .title(n.getTitle())
                    .content(n.getContent())
                    .isPinned(n.getIsPinned())
                    .status(n.getStatus())
                    .createdAt(n.getCreatedAt())
                    .updatedAt(n.getUpdatedAt())
                    .build();
        }
    }

    // 등록/수정 요청 (관리자, JSON 바디로 받음 -> Setter 필요)
    @Getter
    @Setter
    public static class SaveRequest {
        private String title;
        private String content;
        private Boolean isPinned;
        private String status; // 임시저장 / 공개 / 비공개
    }
}