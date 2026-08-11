package kr.co.firstdayproject.controller.report;

import jakarta.validation.Valid;
import java.util.Map;
import kr.co.firstdayproject.dto.report.JobPostingReportRequest;
import kr.co.firstdayproject.exception.DuplicateReportException;
import kr.co.firstdayproject.exception.ResourceNotFoundException;
import kr.co.firstdayproject.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportApiController {

    private final ReportService reportService;

    @PostMapping("/job-postings/{jobPostingId}")
    public ResponseEntity<Map<String, Object>> reportJobPosting(
            @PathVariable Long jobPostingId,
            @Valid @RequestBody JobPostingReportRequest request,
            Authentication authentication
    ) {
        Long reportId = reportService.reportJobPosting(
                jobPostingId,
                request,
                authentication
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "reportId", reportId,
                        "message", "신고가 접수되었습니다."
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("신고 내용을 확인해주세요.");

        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(
            IllegalArgumentException exception
    ) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(
            AccessDeniedException exception
    ) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(
            ResourceNotFoundException exception
    ) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DuplicateReportException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(
            DuplicateReportException exception
    ) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    private ResponseEntity<Map<String, String>> error(
            HttpStatus status,
            String message
    ) {
        return ResponseEntity
                .status(status)
                .body(Map.of("message", message));
    }
}
