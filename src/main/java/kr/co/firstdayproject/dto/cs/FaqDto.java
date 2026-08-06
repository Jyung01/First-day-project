package kr.co.firstdayproject.dto.cs;

import kr.co.firstdayproject.entity.cs.Faq;
import kr.co.firstdayproject.entity.cs.FaqCategory;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.format.DateTimeFormatter;

public class FaqDto {

    // 카테고리 옵션 (필터·등록 셀렉트박스 공용)
    @Getter
    @Builder
    public static class CategoryOption {
        private Long faqCategoryId;
        private String categoryName;

        public static CategoryOption from(FaqCategory c) {
            return CategoryOption.builder()
                    .faqCategoryId(c.getFaqCategoryId())
                    .categoryName(c.getCategoryName())
                    .build();
        }
    }

    // 목록 한 행 (관리자 테이블 / 사용자 아코디언 공용)
    @Getter
    @Builder
    public static class ListItem {
        private Long faqId;
        private Long faqCategoryId;
        private String categoryName;
        private String question;
        private String answer;
        private String createdAtText;
        private String updatedAtText;

        public static ListItem from(Faq f, String categoryName) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd");
            return ListItem.builder()
                    .faqId(f.getFaqId())
                    .faqCategoryId(f.getFaqCategoryId())
                    .categoryName(categoryName)
                    .question(f.getQuestion())
                    .answer(f.getAnswer())
                    .createdAtText(f.getCreatedAt() != null ? f.getCreatedAt().format(fmt) : "")
                    .updatedAtText(f.getUpdatedAt() != null ? f.getUpdatedAt().format(fmt) : "")
                    .build();
        }
    }

    // 목록 응답 (페이지네이션 포함)
    @Getter
    @Builder
    public static class ListResponse {
        private java.util.List<ListItem> items;
        private long totalCount;
        private int totalPages;
        private int page;
    }

    // 상세 (수정 모달 프리필용)
    @Getter
    @Builder
    public static class Detail {
        private Long faqId;
        private Long faqCategoryId;
        private String categoryName;
        private String question;
        private String answer;

        public static Detail from(Faq f, String categoryName) {
            return Detail.builder()
                    .faqId(f.getFaqId())
                    .faqCategoryId(f.getFaqCategoryId())
                    .categoryName(categoryName)
                    .question(f.getQuestion())
                    .answer(f.getAnswer())
                    .build();
        }
    }

    // 등록/수정 요청 (관리자, JSON 바디)
    @Getter
    @Setter
    public static class SaveRequest {
        private Long faqCategoryId;
        private String question;
        private String answer;
    }
}