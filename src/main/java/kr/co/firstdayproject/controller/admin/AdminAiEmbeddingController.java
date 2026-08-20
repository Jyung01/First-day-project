package kr.co.firstdayproject.controller.admin;

import java.util.Map;
import java.util.Optional;
import kr.co.firstdayproject.service.ai.JobPostingEmbeddingBackfillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 채용공고 임베딩 운영용 엔드포인트.
 *
 * 경로가 /admin 아래라 SecurityConfig의 {@code .requestMatchers("/admin/**").hasRole("ADMIN")}으로
 * 이미 보호된다 — 별도 권한 설정이 필요 없다.
 *
 * 백필 서비스는 VectorStore가 필요해 test 프로필에서 빈으로 뜨지 않으므로({@code @Profile("!test")}),
 * 컨트롤러가 그것 때문에 함께 빠지지 않도록 JobService와 같은 방식으로 Optional로 주입받는다.
 */
@RestController
@RequestMapping("/admin/ai")
@RequiredArgsConstructor
public class AdminAiEmbeddingController {

    private static final int DEFAULT_LIMIT = 50;

    private final Optional<JobPostingEmbeddingBackfillService> backfillService;

    /**
     * 채용공고를 pgvector에 일괄 색인한다.
     * 한 번에 다 하지 않고 after 커서를 옮겨가며 여러 번 호출하는 방식이다 —
     * 공고 수만큼 임베딩 API 호출이 나가서 한 번에 처리하면 요청이 타임아웃될 수 있다.
     *
     * 응답의 lastJobPostingId를 다음 호출의 after로 넘기고, remaining이 0이 되면 끝이다.
     */
    @PostMapping("/job-posting-embeddings/backfill")
    public ResponseEntity<Object> backfillJobPostingEmbeddings(
        @RequestParam(name = "after", defaultValue = "0") Long after,
        @RequestParam(name = "limit", defaultValue = "" + DEFAULT_LIMIT) int limit
    ) {
        if (backfillService.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "임베딩 기능이 비활성화된 환경입니다."));
        }
        return ResponseEntity.ok(backfillService.get().backfill(after, limit));
    }
}
