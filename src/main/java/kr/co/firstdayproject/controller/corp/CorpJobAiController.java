package kr.co.firstdayproject.controller.corp;

import kr.co.firstdayproject.dto.corp.job.JobPostingAiPolishRequest;
import kr.co.firstdayproject.dto.corp.job.JobPostingAiPolishResponse;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.corp.JobPostingAiPolishService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/corp/api/job-postings")
public class CorpJobAiController {

    private final JobPostingAiPolishService jobPostingAiPolishService;

    @PostMapping("/ai-polish")
    public ResponseEntity<JobPostingAiPolishResponse> polish(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody JobPostingAiPolishRequest request
    ) {
        if (userDetails == null || userDetails.getCompanyId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    JobPostingAiPolishResponse.failure("기업회원 로그인이 필요합니다.")
            );
        }

        if (request == null) {
            return ResponseEntity.badRequest().body(
                    JobPostingAiPolishResponse.failure("요청 내용을 확인해 주세요.")
            );
        }

        try {
            String polishedContent = jobPostingAiPolishService.polish(
                    userDetails.getCompanyId(),
                    request.fieldType(),
                    request
            );
            return ResponseEntity.ok(
                    JobPostingAiPolishResponse.success(polishedContent)
            );
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    JobPostingAiPolishResponse.failure(exception.getMessage())
            );
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                    JobPostingAiPolishResponse.failure(exception.getMessage())
            );
        }
    }
}
