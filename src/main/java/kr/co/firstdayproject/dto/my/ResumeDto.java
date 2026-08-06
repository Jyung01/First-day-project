package kr.co.firstdayproject.dto.my;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 이력서 관련 요청/응답 DTO 모음.
 */
public class ResumeDto {

    /** 마이페이지 목록 화면에 뿌릴 때 쓰는 응답용 */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ListItem {
        private Long id;
        private String title;
        private String summary;
    }

    /** 보유 기술 칩 표시용 (id + 이름) */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SkillChip {
        private Long id;
        private String name;
    }

    /**
     * 이력서 등록/수정 폼 제출용.
     * 일반 form-urlencoded POST를 @ModelAttribute로 바인딩하므로
     * (JSON이 아님) 반드시 getter/setter를 모두 갖춰야 함.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class FormRequest {
        private Long resumeId;
        private String title;
        private String careerType;
        private String summary;
        private List<EducationItem> educations = new ArrayList<>();
        private List<CareerItem> careers = new ArrayList<>();
        private List<ProjectItem> projects = new ArrayList<>();
        private List<Long> skillIds = new ArrayList<>();

        @Getter
        @Setter
        @NoArgsConstructor
        public static class EducationItem {
            private String schoolName;
            private String major;
            private String degree;
            private YearMonth admissionDate;
            private YearMonth graduationDate;
            private String graduationStatus;
            private BigDecimal gpaScore;
            private BigDecimal gpaScale;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class CareerItem {
            private String companyName;
            private String positionTitle;
            private String employmentType;
            private YearMonth startDate;
            private YearMonth endDate;

            /** "재직" / "퇴사" — 저장 시 isCurrent(+endDate 처리)로 변환 */
            private String employmentStatus;
            private String description;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class ProjectItem {
            private String projectName;
            private String roleText;
            private String description;
        }
    }
}
