package kr.co.firstdayproject.dto.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobPostingReportRequest(
        @NotBlank(message = "신고 사유를 선택해주세요.")
        @Size(max = 30, message = "올바른 신고 사유를 선택해주세요.")
        String reasonCode,

        @NotBlank(message = "상세 내용을 입력해주세요.")
        @Size(max = 500, message = "상세 내용은 500자를 초과할 수 없습니다.")
        String detail
) {
}
