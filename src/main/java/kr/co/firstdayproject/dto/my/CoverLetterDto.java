package kr.co.firstdayproject.dto.my;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 자기소개서 관련 요청/응답 DTO 모음.
 * 화면(목록/작성)별로 필요한 형태가 달라서 static nested class로 구분해둠.
 */
public class CoverLetterDto {

    /** 마이페이지 목록 화면에 뿌릴 때 쓰는 응답용 */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ListItem {
        private Long id;
        private String title;
        private int questionCount;
        private int charCount;
        private boolean aiReviewed;
        private String aiStatusLabel;
        private String aiStatusBadgeClass;

        public String getAiStatusLabel() {
            return aiReviewed ? "AI 첨삭 완료" : "검사 전";
        }

        public String getAiStatusBadgeClass() {
            return aiReviewed ? "status-badge--ai-done" : "status-badge--pending";
        }
    }

    /** 새 자기소개서 작성/수정 폼에서 저장 요청할 때 쓰는 요청용 */
    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        private String title;
        private List<Item> items;

        @Getter
        @NoArgsConstructor
        public static class Item {
            private String question;
            private String answer;
            private Integer displayOrder;
        }
    }
}