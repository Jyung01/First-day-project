package kr.co.firstdayproject.controller.job;

import java.util.Map;
import kr.co.firstdayproject.exception.ResourceNotFoundException;
import kr.co.firstdayproject.service.job.SavedJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class SavedJobApiController {

    private final SavedJobService savedJobService;

    /**
     * 관심공고 등록/해제.
     *
     * 이미 등록된 공고면 삭제하고 bookmarked=false,
     * 등록되지 않은 공고면 저장하고 bookmarked=true를 반환한다.
     */
    @PostMapping("/{jobPostingId}/bookmark")
    public ResponseEntity<Map<String, Object>> toggleBookmark(
            @PathVariable Long jobPostingId,
            Authentication authentication
    ) {
        boolean bookmarked = savedJobService.toggleSavedJob(
                jobPostingId,
                authentication
        );

        return ResponseEntity.ok(
                Map.of(
                        "jobPostingId", jobPostingId,
                        "bookmarked", bookmarked
                )
        );
    }

    /**
     * 인증되지 않았거나 개인회원이 아닌 경우.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(
                        Map.of(
                                "message",
                                exception.getMessage()
                        )
                );
    }

    /**
     * 존재하지 않는 공고 id로 요청한 경우.
     *
     * <p>GlobalExceptionHandler는 뷰 이름을 반환하는 화면용이라 이 컨트롤러가 그쪽으로 넘어가면
     * 뷰 이름이 그대로 응답 본문이 된다. RestController에서는 여기서 직접 처리한다.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        Map.of(
                                "message",
                                exception.getMessage()
                        )
                );
    }
}