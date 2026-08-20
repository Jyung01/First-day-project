package kr.co.firstdayproject.dto.my;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import kr.co.firstdayproject.validation.DateRanges;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 이력서 폼 검증 규칙 테스트.
 *
 * <p>여기서 막지 못하면 DB의 DATE·DECIMAL(3,2) 한계와 CHECK 제약에 걸려
 * 저장 시점에 500이 난다. 각 규칙이 대응하는 DB 제약을 주석으로 남긴다.
 */
class ResumeFormRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void acceptsValidForm() {
        assertThat(validator.validate(validForm())).isEmpty();
    }

    /** admission_date DATE — MySQL DATE는 9999년까지만 담을 수 있다. */
    @Test
    void rejectsAdmissionDateWithUnrealisticYear() {
        ResumeDto.FormRequest form = validForm();
        form.getEducations().get(0).setAdmissionDate(YearMonth.of(131212, 12));

        assertThat(validator.validate(form))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString().equals("educations[0].admissionDate"));
    }

    @Test
    void rejectsGraduationDateBeforeAdmissionDate() {
        ResumeDto.FormRequest form = validForm();
        ResumeDto.FormRequest.EducationItem education = form.getEducations().get(0);
        education.setAdmissionDate(YearMonth.of(2020, 3));
        education.setGraduationDate(YearMonth.of(2019, 2));

        assertThat(validator.validate(form)).isNotEmpty();
    }

    /** 졸업 예정일은 미래여야 하므로 여유 범위 안이면 통과해야 한다. */
    @Test
    void acceptsNearFutureGraduationDate() {
        ResumeDto.FormRequest form = validForm();
        form.getEducations().get(0).setGraduationDate(YearMonth.of(Year.now().getValue() + 2, 2));

        assertThat(validator.validate(form)).isEmpty();
    }

    /** gpa_score DECIMAL(3,2) — 최대 9.99 */
    @Test
    void rejectsGpaScoreOverColumnLimit() {
        ResumeDto.FormRequest form = validForm();
        form.getEducations().get(0).setGpaScore(new BigDecimal("12131"));

        assertThat(validator.validate(form))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString().equals("educations[0].gpaScore"));
    }

    @Test
    void rejectsGpaScaleOverColumnLimit() {
        ResumeDto.FormRequest form = validForm();
        form.getEducations().get(0).setGpaScale(new BigDecimal("122112"));

        assertThat(validator.validate(form))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString().equals("educations[0].gpaScale"));
    }

    /** CHECK (gpa_score <= gpa_scale) */
    @Test
    void rejectsGpaScoreGreaterThanScale() {
        ResumeDto.FormRequest form = validForm();
        ResumeDto.FormRequest.EducationItem education = form.getEducations().get(0);
        education.setGpaScore(new BigDecimal("4.50"));
        education.setGpaScale(new BigDecimal("4.00"));

        assertThat(validator.validate(form)).isNotEmpty();
    }

    /** CHECK (end_date >= start_date) */
    @Test
    void rejectsCareerEndDateBeforeStartDate() {
        ResumeDto.FormRequest form = validForm();
        ResumeDto.FormRequest.CareerItem career = form.getCareers().get(0);
        career.setStartDate(YearMonth.of(2023, 5));
        career.setEndDate(YearMonth.of(2022, 5));

        assertThat(validator.validate(form)).isNotEmpty();
    }

    @Test
    void rejectsBlankTitle() {
        ResumeDto.FormRequest form = validForm();
        form.setTitle("  ");

        assertThat(validator.validate(form))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("title"));
    }

    /**
     * 학교명이 비어 있는 행은 서비스가 저장에서 건너뛴다.
     * 빈 행 때문에 저장이 막히면 안 되므로 통과해야 한다.
     */
    @Test
    void acceptsEmptyEducationRow() {
        ResumeDto.FormRequest form = validForm();
        form.getEducations().add(new ResumeDto.FormRequest.EducationItem());

        assertThat(validator.validate(form)).isEmpty();
    }

    /**
     * "상태 선택"을 고르지 않으면 빈 문자열이 온다.
     * 선택하지 않는 것은 정상이므로 검증을 통과해야 한다.
     * (빈 문자열은 저장 직전에 ResumeService가 null로 바꾼다. CHECK 제약이 NULL만 허용하기 때문)
     */
    @Test
    void acceptsUnselectedGraduationStatus() {
        ResumeDto.FormRequest form = validForm();
        form.getEducations().get(0).setGraduationStatus("");

        assertThat(validator.validate(form)).isEmpty();
    }

    /** CHECK (graduation_status IS NULL OR graduation_status IN (...)) */
    @Test
    void rejectsGraduationStatusOutsideAllowedValues() {
        ResumeDto.FormRequest form = validForm();
        form.getEducations().get(0).setGraduationStatus("졸업함");

        assertThat(validator.validate(form))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString().equals("educations[0].graduationStatus"));
    }

    @Test
    void acceptsUnselectedEmploymentType() {
        ResumeDto.FormRequest form = validForm();
        form.getCareers().get(0).setEmploymentType("");

        assertThat(validator.validate(form)).isEmpty();
    }

    /** CHECK (employment_type IS NULL OR employment_type IN (...)) */
    @Test
    void rejectsEmploymentTypeOutsideAllowedValues() {
        ResumeDto.FormRequest form = validForm();
        form.getCareers().get(0).setEmploymentType("아르바이트");

        assertThat(validator.validate(form))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString().equals("careers[0].employmentType"));
    }

    /**
     * 화면의 연도 목록과 서버 검증 범위가 어긋나면,
     * 고를 수 있는 연도를 서버가 거부하거나 그 반대가 된다.
     * 양 끝 연도가 검증을 통과하는지로 두 곳이 같은 상수를 쓰는지 확인한다.
     */
    @Test
    void acceptsBothEndsOfTheSelectableYearRange() {
        ResumeDto.FormRequest earliest = validForm();
        earliest.getEducations().get(0).setAdmissionDate(YearMonth.of(DateRanges.MIN_YEAR, 1));
        earliest.getEducations().get(0).setGraduationDate(YearMonth.of(DateRanges.MIN_YEAR, 2));

        ResumeDto.FormRequest latest = validForm();
        latest.getEducations().get(0).setAdmissionDate(YearMonth.of(DateRanges.maxYear(), 1));
        latest.getEducations().get(0).setGraduationDate(YearMonth.of(DateRanges.maxYear(), 12));

        assertThat(validator.validate(earliest)).isEmpty();
        assertThat(validator.validate(latest)).isEmpty();
    }

    /** 목록 밖 연도는 거부되어야 한다. */
    @Test
    void rejectsYearJustOutsideTheSelectableRange() {
        ResumeDto.FormRequest form = validForm();
        form.getEducations().get(0).setAdmissionDate(YearMonth.of(DateRanges.MIN_YEAR - 1, 1));

        assertThat(validator.validate(form))
                .anyMatch(violation -> violation.getPropertyPath()
                        .toString().equals("educations[0].admissionDate"));
    }

    private ResumeDto.FormRequest validForm() {
        ResumeDto.FormRequest.EducationItem education = new ResumeDto.FormRequest.EducationItem();
        education.setSchoolName("첫출근대학교");
        education.setMajor("컴퓨터공학");
        education.setAdmissionDate(YearMonth.of(2016, 3));
        education.setGraduationDate(YearMonth.of(2020, 2));
        education.setGraduationStatus("졸업");
        education.setGpaScore(new BigDecimal("3.80"));
        education.setGpaScale(new BigDecimal("4.50"));

        ResumeDto.FormRequest.CareerItem career = new ResumeDto.FormRequest.CareerItem();
        career.setCompanyName("첫출근");
        career.setPositionTitle("백엔드 개발");
        career.setEmploymentType("정규직");
        career.setStartDate(YearMonth.of(2020, 3));
        career.setEndDate(YearMonth.of(2023, 5));

        ResumeDto.FormRequest form = new ResumeDto.FormRequest();
        form.setTitle("신입 백엔드 이력서");
        form.setCareerType("신입");
        form.setEducations(new java.util.ArrayList<>(List.of(education)));
        form.setCareers(new java.util.ArrayList<>(List.of(career)));

        return form;
    }
}
