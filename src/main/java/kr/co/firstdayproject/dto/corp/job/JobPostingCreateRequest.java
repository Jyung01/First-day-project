package kr.co.firstdayproject.dto.corp.job;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@NoArgsConstructor
public class JobPostingCreateRequest {

    private Long jobCategoryId;

    @NotBlank(message = "공고 제목을 입력해 주세요.")
    @Size(max = 255, message = "공고 제목은 255자 이하로 입력해 주세요.")
    private String title;

    private String employmentType;

    private String careerType;

    @Min(value = 0, message = "최소 경력은 0년 이상이어야 합니다.")
    @Max(value = 50, message = "최소 경력은 50년 이하로 입력해 주세요.")
    private Integer minExperienceYears;

    @Min(value = 0, message = "최대 경력은 0년 이상이어야 합니다.")
    @Max(value = 50, message = "최대 경력은 50년 이하로 입력해 주세요.")
    private Integer maxExperienceYears;

    private String educationLevel;

    @Size(max = 100, message = "근무 지역은 100자 이하로 입력해 주세요.")
    private String workRegion;

    @Size(max = 400, message = "기본 주소는 400자 이하로 입력해 주세요.")
    private String address;

    @Size(max = 100, message = "상세 주소는 100자 이하로 입력해 주세요.")
    private String addressDetail;

    @Size(max = 100, message = "급여 표시 문구는 100자 이하로 입력해 주세요.")
    private String salaryText;

    @PositiveOrZero(message = "최소 연봉은 0 이상이어야 합니다.")
    private Integer salaryMin;

    @PositiveOrZero(message = "최대 연봉은 0 이상이어야 합니다.")
    private Integer salaryMax;

    @Min(value = 1, message = "모집 인원은 1명 이상이어야 합니다.")
    @Max(value = 65535, message = "모집 인원은 65,535명 이하로 입력해 주세요.")
    private Integer headcount;

    @FutureOrPresent(message = "접수 마감일은 오늘 이후여야 합니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate applyEndDate;

    private String introduction;

    private String mainTasks;

    private String qualifications;

    private String preferredConditions;

    @Size(max = 10, message = "복리후생은 최대 10개까지 선택할 수 있습니다.")
    private List<String> benefits = new ArrayList<>();

    @Size(max = 5, message = "기술 스택은 최대 5개까지 선택할 수 있습니다.")
    private List<Long> skillIds = new ArrayList<>();

    @NotBlank(message = "저장 방식을 확인해 주세요.")
    @Pattern(
            regexp = "DRAFT|PUBLISH|REVIEW",
            message = "올바르지 않은 저장 방식입니다."
    )
    private String submitType;
}
