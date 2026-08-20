package kr.co.firstdayproject.dto.my;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import kr.co.firstdayproject.validation.YearMonthRange;
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

        @NotBlank(message = "이력서 제목을 입력해주세요.")
        @Size(max = 200, message = "이력서 제목은 200자 이내로 입력해주세요.")
        private String title;

        @NotBlank(message = "경력 구분을 선택해주세요.")
        private String careerType;

        private String summary;

        @Valid
        private List<EducationItem> educations = new ArrayList<>();

        @Valid
        private List<CareerItem> careers = new ArrayList<>();

        @Valid
        private List<ProjectItem> projects = new ArrayList<>();

        private List<Long> skillIds = new ArrayList<>();

        /**
         * 각 항목의 제약은 DB 컬럼 정의와 CHECK 제약에 맞춘다.
         * 여기서 걸러지지 않으면 저장 시점에 DB가 거부해 500이 된다.
         */
        @Getter
        @Setter
        @NoArgsConstructor
        public static class EducationItem {

            // school_name VARCHAR(200). 값이 비면 서비스가 그 행을 건너뛰므로 @NotBlank는 쓰지 않는다.
            @Size(max = 200, message = "학교명은 200자 이내로 입력해주세요.")
            private String schoolName;

            @Size(max = 200, message = "전공은 200자 이내로 입력해주세요.")
            private String major;

            @Size(max = 100, message = "학위는 100자 이내로 입력해주세요.")
            private String degree;

            @YearMonthRange(message = "입학일을 올바르게 입력해주세요.")
            private YearMonth admissionDate;

            @YearMonthRange(message = "졸업(예정)일을 올바르게 입력해주세요.")
            private YearMonth graduationDate;

            /*
             * CHECK (graduation_status IS NULL OR graduation_status IN (...))와 같은 목록.
             * 선택하지 않으면 빈 문자열이 오므로 빈 값도 허용하고, 저장 직전에 null로 바꾼다.
             */
            @Pattern(
                    regexp = "^$|^(재학|휴학|졸업예정|졸업|수료|중퇴)$",
                    message = "졸업 상태를 목록에서 선택해주세요."
            )
            private String graduationStatus;

            // gpa_score DECIMAL(3,2) → 정수부 1자리, 소수부 2자리, 최대 9.99
            @DecimalMin(value = "0.00", message = "학점은 0 이상이어야 합니다.")
            @DecimalMax(value = "9.99", message = "학점은 9.99 이하여야 합니다.")
            @Digits(integer = 1, fraction = 2, message = "학점은 소수점 둘째 자리까지 입력해주세요.")
            private BigDecimal gpaScore;

            @DecimalMin(value = "0.00", message = "학점 기준은 0 이상이어야 합니다.")
            @DecimalMax(value = "9.99", message = "학점 기준은 9.99 이하여야 합니다.")
            @Digits(integer = 1, fraction = 2, message = "학점 기준은 소수점 둘째 자리까지 입력해주세요.")
            private BigDecimal gpaScale;

            /** DB CHECK(gpa_score <= gpa_scale)와 같은 조건. 둘 다 있을 때만 따진다. */
            @AssertTrue(message = "학점은 학점 기준보다 클 수 없습니다.")
            public boolean isGpaWithinScale() {
                if (gpaScore == null || gpaScale == null) {
                    return true;
                }
                return gpaScore.compareTo(gpaScale) <= 0;
            }

            /** 졸업일이 입학일보다 앞설 수 없다. */
            @AssertTrue(message = "졸업(예정)일은 입학일 이후여야 합니다.")
            public boolean isGraduationAfterAdmission() {
                if (admissionDate == null || graduationDate == null) {
                    return true;
                }
                return !graduationDate.isBefore(admissionDate);
            }
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class CareerItem {

            // company_name VARCHAR(200). 비면 서비스가 건너뛴다.
            @Size(max = 200, message = "회사명은 200자 이내로 입력해주세요.")
            private String companyName;

            @Size(max = 100, message = "직무는 100자 이내로 입력해주세요.")
            private String positionTitle;

            /* CHECK (employment_type IS NULL OR employment_type IN (...))와 같은 목록. */
            @Pattern(
                    regexp = "^$|^(정규직|계약직|인턴|프리랜서|파견직|기타)$",
                    message = "고용 형태를 목록에서 선택해주세요."
            )
            private String employmentType;

            @YearMonthRange(message = "입사일을 올바르게 입력해주세요.")
            private YearMonth startDate;

            @YearMonthRange(message = "퇴사일을 올바르게 입력해주세요.")
            private YearMonth endDate;

            /** "재직" / "퇴사" — 저장 시 isCurrent(+endDate 처리)로 변환 */
            private String employmentStatus;

            private String description;

            /** DB CHECK(end_date >= start_date)와 같은 조건. */
            @AssertTrue(message = "퇴사일은 입사일 이후여야 합니다.")
            public boolean isEndAfterStart() {
                if (startDate == null || endDate == null) {
                    return true;
                }
                return !endDate.isBefore(startDate);
            }
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class ProjectItem {

            // project_name VARCHAR(200). 비면 서비스가 건너뛴다.
            @Size(max = 200, message = "프로젝트명은 200자 이내로 입력해주세요.")
            private String projectName;

            @Size(max = 300, message = "역할은 300자 이내로 입력해주세요.")
            private String roleText;

            private String description;
        }
    }
}
