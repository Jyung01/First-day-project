package kr.co.firstdayproject.service.ai;

import java.util.ArrayList;
import java.util.List;
import kr.co.firstdayproject.dto.ai.JobPostingEmbeddingBackfillResult;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * 이미 저장된 채용공고를 pgvector에 일괄 색인한다.
 *
 * 평소 임베딩은 공고 저장 경로가 발행하는 이벤트를 JobPostingEmbeddingEventListener가 받아
 * 처리하므로, 그 경로를 거치지 않고 들어온 공고(SQL 직접 삽입, 새 환경 구축, 임베딩 모델 교체 등)는
 * 벡터 스토어에 없다. 이 서비스는 그런 공고를 나중에 채워 넣기 위한 수동 도구다.
 *
 * 공고 상태(모집중/마감)나 기업 상태는 보지 않고 전부 색인한다. 노출 여부는 검색 단계에서
 * SimilarJobPostingSearchService가 MySQL에 물어 거르므로, 색인 단계까지 상태를 따지면
 * 상태가 바뀔 때마다 색인을 맞춰줘야 하는 동기화 부담만 생긴다.
 *
 * JobPostingEmbeddingService.upsert()는 공고 ID로부터 결정적 UUID를 만들어 delete 후 add하므로
 * 몇 번을 다시 돌려도 중복이 쌓이지 않는다.
 */
@Service
@Profile("!test")
@RequiredArgsConstructor
public class JobPostingEmbeddingBackfillService {

    private static final Logger log =
        LoggerFactory.getLogger(JobPostingEmbeddingBackfillService.class);

    /** 한 번의 호출에서 처리할 수 있는 최대 공고 수 — HTTP 요청이 과도하게 길어지는 것을 막는다. */
    private static final int MAX_LIMIT = 200;
    private static final int MIN_LIMIT = 1;

    private final JobPostingRepository jobPostingRepository;
    private final JobPostingEmbeddingService embeddingService;

    /**
     * afterJobPostingId 다음 공고부터 limit개를 색인한다.
     * 공고 하나가 실패해도 멈추지 않고 끝까지 진행한 뒤 실패 목록을 함께 돌려준다 —
     * 중간에 멈추면 어디까지 됐는지 알기 어렵고, 실패 하나 때문에 나머지를 막을 이유도 없다.
     */
    public JobPostingEmbeddingBackfillResult backfill(Long afterJobPostingId, int limit) {
        long cursor = afterJobPostingId == null || afterJobPostingId < 0 ? 0L : afterJobPostingId;
        int pageSize = Math.max(MIN_LIMIT, Math.min(limit, MAX_LIMIT));

        List<Long> jobPostingIds =
            jobPostingRepository.findIdsAfter(cursor, PageRequest.of(0, pageSize));

        int succeeded = 0;
        List<Long> failed = new ArrayList<>();
        for (Long jobPostingId : jobPostingIds) {
            try {
                embeddingService.upsert(jobPostingId);
                succeeded++;
            } catch (Exception exception) {
                failed.add(jobPostingId);
                log.error("채용공고 임베딩 백필 실패: jobPostingId={}", jobPostingId, exception);
            }
        }

        // 처리한 게 없으면 커서를 그대로 둔다 — 같은 값으로 다시 호출해도 무한히 제자리걸음하지 않도록
        // remaining이 0으로 떨어져 호출부가 끝을 알 수 있다.
        Long lastJobPostingId = jobPostingIds.isEmpty()
            ? cursor
            : jobPostingIds.get(jobPostingIds.size() - 1);
        long remaining = jobPostingRepository.countByJobPostingIdGreaterThan(lastJobPostingId);

        log.info(
            "채용공고 임베딩 백필: 시도={}, 성공={}, 실패={}, 마지막ID={}, 남음={}",
            jobPostingIds.size(), succeeded, failed.size(), lastJobPostingId, remaining
        );

        return new JobPostingEmbeddingBackfillResult(
            jobPostingIds.size(), succeeded, failed, lastJobPostingId, remaining
        );
    }
}
