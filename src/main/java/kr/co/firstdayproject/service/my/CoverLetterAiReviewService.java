package kr.co.firstdayproject.service.my;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import kr.co.firstdayproject.dto.ai.CoverLetterAiReviewDetail;
import kr.co.firstdayproject.dto.ai.CoverLetterAiReviewHistoryItem;
import kr.co.firstdayproject.dto.ai.CoverLetterAiReviewItemView;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.coverletter.CoverLetterAiReview;
import kr.co.firstdayproject.entity.coverletter.CoverLetterItem;
import kr.co.firstdayproject.entity.job.JobPosting;
import kr.co.firstdayproject.exception.AiReviewGenerationException;
import kr.co.firstdayproject.exception.ResourceNotFoundException;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.repository.coverletter.CoverLetterAiReviewRepository;
import kr.co.firstdayproject.repository.coverletter.CoverLetterItemRepository;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.dto.ai.CoverLetterItemReviewOutcome;
import kr.co.firstdayproject.dto.ai.RagEvidence;
import kr.co.firstdayproject.service.ai.ApplicantResumeSummaryService;
import kr.co.firstdayproject.service.ai.CoverLetterItemReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 자소서 AI 첨삭 요청을 오케스트레이션한다.
 * 문항 하나하나의 첨삭 생성 자체는 CoverLetterItemReviewService가 맡고,
 * 여기서는 문항을 순회하며 결과를 모아 CoverLetterAiReview 1행으로 저장/조회한다.
 *
 * CoverLetterItemReviewService는 VectorStore/ChatClient가 필요해 test 프로필에서
 * 빈으로 등록되지 않는다({@code @Profile("!test")}). 이 서비스는 CoverLetterController가
 * 프로필 구분 없이 항상 의존하는 빈이라 함께 test에서 빠질 수 없으므로, 해당 의존성만
 * {@code @Lazy}로 주입해 실제로 호출되기 전까지는 빈 조회를 미룬다.
 */
@Service
public class CoverLetterAiReviewService {

    private static final Logger log =
        LoggerFactory.getLogger(CoverLetterAiReviewService.class);

    private final CoverLetterService coverLetterService;
    private final CoverLetterAiReviewRepository coverLetterAiReviewRepository;
    private final CoverLetterItemRepository coverLetterItemRepository;
    private final JobPostingRepository jobPostingRepository;
    private final CompanyRepository companyRepository;
    private final CoverLetterItemReviewService coverLetterItemReviewService;
    private final ApplicantResumeSummaryService applicantResumeSummaryService;
    // 이 프로젝트는 spring-boot-starter-webmvc만 사용해 Jackson ObjectMapper 빈이
    // 자동 등록되지 않으므로, 전역 설정에 손대지 않고 이 서비스 전용으로 직접 생성한다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CoverLetterAiReviewService(
        CoverLetterService coverLetterService,
        CoverLetterAiReviewRepository coverLetterAiReviewRepository,
        CoverLetterItemRepository coverLetterItemRepository,
        JobPostingRepository jobPostingRepository,
        CompanyRepository companyRepository,
        @Lazy CoverLetterItemReviewService coverLetterItemReviewService,
        ApplicantResumeSummaryService applicantResumeSummaryService
    ) {
        this.applicantResumeSummaryService = applicantResumeSummaryService;
        this.coverLetterService = coverLetterService;
        this.coverLetterAiReviewRepository = coverLetterAiReviewRepository;
        this.coverLetterItemRepository = coverLetterItemRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.companyRepository = companyRepository;
        this.coverLetterItemReviewService = coverLetterItemReviewService;
    }

    /**
     * 문항을 첨삭하고 결과를 저장한다. 문항끼리는 서로 독립적이라 OpenAI 호출을 병렬로
     * 실행한다 — 순차 호출이면 "문항 수 × 개별 응답시간"이 그대로 더해지지만, 병렬이면
     * 가장 느린 문항 1개 응답시간 정도로 전체 대기시간이 줄어든다.
     * 호출부(컨트롤러)가 이 메서드의 완료를 기다린 뒤 리다이렉트한다.
     *
     * 이 메서드에는 의도적으로 {@code @Transactional}을 붙이지 않는다. 트랜잭션으로 감싸면
     * OpenAI 응답을 기다리는 수 초~수십 초 동안 DB 커넥션을 빌린 채로 붙잡게 되는데,
     * 커넥션 풀은 기본 10개뿐이라 동시 요청이 몰리면 첨삭과 무관한 페이지까지 커넥션을 못 받아
     * 사이트 전체가 멈춘다. 앞쪽 조회들은 각자 짧게 커넥션을 쓰고 반납하며, 마지막 save()는
     * 리포지토리가 자체 트랜잭션으로 처리하는 단일 INSERT라 원자성도 유지된다.
     *
     * 참고로 트랜잭션은 호출 스레드에 묶이므로 병렬 스트림이 갈라낸 스레드는 어차피
     * 트랜잭션 밖이다. 저 람다 안에서는 DB를 건드리지 말 것.
     */
    public Long requestReview(
        Long coverLetterId,
        Long userId,
        Long jobPostingId,
        Map<Long, String> additionalInfoByItemId
    ) {
        List<CoverLetterItem> items = coverLetterService.getItems(coverLetterId, userId);
        if (items.isEmpty()) {
            throw new IllegalStateException("첨삭할 문항이 없습니다.");
        }

        JobPosting targetPosting = jobPostingRepository.findById(jobPostingId)
            .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 채용공고입니다."));

        // jobPostingId는 클라이언트가 보낸 값이라 그대로 믿지 않는다. 검증이 없으면 공고 선택
        // 화면에 뜨지도 않는 공고(마감됐거나, 미승인·이용정지 기업의 공고)로도 첨삭이 돌아간다.
        // 판별 기준은 공고 목록·검색과 같은 findVisibleIdsIn을 재사용해 한 곳에서만 관리한다.
        if (jobPostingRepository.findVisibleIdsIn(List.of(jobPostingId)).isEmpty()) {
            throw new IllegalStateException(
                "지금은 첨삭 대상으로 선택할 수 없는 공고입니다. 모집이 마감되었거나 공개되지 않은 공고일 수 있습니다."
            );
        }

        // 이력서는 사용자가 직접 입력한 본인의 사실이라 첨삭 재료로 써도 지어내기가 아니다.
        // 다만 본문에 섞이면 서로 다른 경험이 한 문장으로 합쳐질 수 있어, 프롬프트에서
        // improvementPoints 용도로만 쓰도록 제한한다. 문항마다 같은 값이므로 한 번만 조회한다.
        String applicantResume = applicantResumeSummaryService.buildSummary(userId);

        // 사용자가 알려준 추가 정보도 함께 남긴다. 첨삭 결과에 그 내용으로 만들어진 문장이
        // 들어가므로, 나중에 "원문에 없는 문장"을 판단할 때 이것도 사용자가 제공한 내용으로 쳐야 한다.
        List<OriginalItemSnapshot> originalSnapshot = items.stream()
            .map(item -> new OriginalItemSnapshot(
                item.getQuestion(),
                item.getAnswer(),
                additionalInfoByItemId == null
                    ? null
                    : additionalInfoByItemId.get(item.getCoverLetterItemId())
            ))
            .toList();

        // 문항 목록 전체를 각 호출에 함께 넘긴다. 문항 하나만 보여주면 모델은 어떤 주제가 다른
        // 문항의 몫인지 판단할 수 없어, 협업 방식이나 테스트 코드처럼 어디에나 붙는 보완 제안이
        // 문항마다 중복해서 나왔다. 넘기는 것은 다른 호출의 '결과'가 아니라 '문항 구성'이므로
        // 병렬 실행 순서에 의존하지 않는다.
        List<String> questions = items.stream()
            .map(CoverLetterItem::getQuestion)
            .toList();

        // 병렬로 돌려도 IntStream.range의 순서(문항 순서)는 그대로 유지된다.
        // 리스트에 add하는 방식으로 바꾸면 병렬 실행에서 순서가 깨져 toDetail()의 인덱스 매칭이
        // 어긋나므로, 아래 세 리스트 모두 map().toList() 형태를 유지해야 한다.
        //
        // 문항 하나가 실패해도 나머지는 살린다. 예외를 그대로 올리면 이미 응답을 받아온 다른
        // 문항까지 버려지고, 사용자는 수십 초를 기다린 끝에 아무것도 받지 못한다.
        // 실패한 자리는 null로 남기며, toDetail()이 이미 null을 다루고 있어 조회는 그대로 동작한다.
        List<CoverLetterItemReviewOutcome> outcomes = IntStream.range(0, items.size())
            .parallel()
            .mapToObj(index -> {
                CoverLetterItem item = items.get(index);
                try {
                    return coverLetterItemReviewService.review(
                        questions,
                        index,
                        item.getAnswer(),
                        targetPosting,
                        applicantResume,
                        // 이 문항에 대해 사용자가 적은 내용만 넘긴다 — 다른 문항 것은 애초에 보이지 않는다.
                        additionalInfoByItemId == null
                            ? null
                            : additionalInfoByItemId.get(item.getCoverLetterItemId())
                    );
                } catch (RuntimeException exception) {
                    log.error(
                        "자소서 문항 첨삭 실패 — 나머지 문항은 계속 진행: coverLetterId={}, coverLetterItemId={}",
                        coverLetterId,
                        item.getCoverLetterItemId(),
                        exception
                    );
                    return null;
                }
            })
            .toList();

        // 남길 결과가 하나도 없으면 빈 이력을 만들지 않고 실패로 응답한다.
        if (outcomes.stream().allMatch(Objects::isNull)) {
            throw new AiReviewGenerationException("모든 문항의 첨삭 생성에 실패했습니다.");
        }

        List<RevisedItemContent> revisedContent = outcomes.stream()
            .map(outcome -> outcome == null ? null : new RevisedItemContent(
                outcome.result().summary(),
                outcome.result().improvementPoints(),
                outcome.result().revisedAnswer()
            ))
            .toList();

        // 첨삭의 근거로 프롬프트에 들어간 검색 문단을 문항 순서 그대로 남긴다(REQ-903).
        // original_content·revised_content와 같은 인덱스 규칙이라 나란히 읽을 수 있다.
        // 원소가 빈 배열이면 검색 결과가 없었던 것, null이면 그 문항의 첨삭 자체가 실패한 것이다.
        List<List<RagEvidence>> ragContext = outcomes.stream()
            .map(outcome -> outcome == null ? null : outcome.evidence())
            .toList();

        CoverLetterAiReview review = CoverLetterAiReview.builder()
            .coverLetterId(coverLetterId)
            .jobPostingId(jobPostingId)
            .originalContent(writeJson(originalSnapshot))
            .revisedContent(writeJson(revisedContent))
            .feedback(buildOverallFeedback(revisedContent))
            .ragContext(writeJson(ragContext))
            .createdAt(LocalDateTime.now())
            .build();

        // 트랜잭션 밖에서 저장하므로 영속성 컨텍스트에 기대지 않고 저장된 엔티티에서 id를 읽는다.
        CoverLetterAiReview saved = coverLetterAiReviewRepository.save(review);
        return saved.getCoverLetterAiReviewId();
    }

    /**
     * reviewId를 지정하면 그 이력을, 없으면 가장 최근 이력을 조회한다.
     * reviewId가 이 자기소개서의 것이 아니면(다른 사용자 것이거나 잘못된 값) 빈 값을 돌려준다.
     */
    @Transactional(readOnly = true)
    public Optional<CoverLetterAiReviewDetail> getReview(Long coverLetterId, Long userId, Long reviewId) {
        coverLetterService.getMine(coverLetterId, userId);

        Optional<CoverLetterAiReview> review = reviewId != null
            ? coverLetterAiReviewRepository.findById(reviewId)
                .filter(found -> found.getCoverLetterId().equals(coverLetterId))
            : coverLetterAiReviewRepository.findFirstByCoverLetterIdOrderByCreatedAtDesc(coverLetterId);

        return review.map(this::toDetail);
    }

    /** 이전 첨삭 이력 드롭다운에 표시할 목록 — 최신순, 대상 공고명 포함 */
    @Transactional(readOnly = true)
    public List<CoverLetterAiReviewHistoryItem> getReviewHistory(Long coverLetterId, Long userId) {
        coverLetterService.getMine(coverLetterId, userId);
        List<CoverLetterAiReview> reviews =
            coverLetterAiReviewRepository.findByCoverLetterIdOrderByCreatedAtDesc(coverLetterId);

        Set<Long> jobPostingIds = reviews.stream()
            .map(CoverLetterAiReview::getJobPostingId)
            .collect(Collectors.toSet());
        Map<Long, String> titleByJobPostingId = jobPostingRepository.findAllById(jobPostingIds).stream()
            .collect(Collectors.toMap(JobPosting::getJobPostingId, JobPosting::getTitle));

        return reviews.stream()
            .map(review -> new CoverLetterAiReviewHistoryItem(
                review.getCoverLetterAiReviewId(),
                titleByJobPostingId.getOrDefault(review.getJobPostingId(), "삭제된 공고"),
                review.getCreatedAt()
            ))
            .toList();
    }

    private CoverLetterAiReviewDetail toDetail(CoverLetterAiReview review) {
        JobPosting targetPosting = jobPostingRepository.findById(review.getJobPostingId()).orElse(null);
        String companyName = null;
        if (targetPosting != null) {
            companyName = companyRepository.findById(targetPosting.getCompanyId())
                .map(Company::getCompanyName)
                .orElse(null);
        }

        List<OriginalItemSnapshot> originals = readJson(
            review.getOriginalContent(),
            new TypeReference<List<OriginalItemSnapshot>>() {}
        );
        List<RevisedItemContent> revisions = readJson(
            review.getRevisedContent(),
            new TypeReference<List<RevisedItemContent>>() {}
        );

        // "이 내용으로 적용" 시 갱신할 실제 CoverLetterItem을 찾기 위해 현재 문항 목록도
        // 같은 순서(displayOrder)로 가져와 인덱스 기준으로 매칭한다. 첨삭 요청 이후 문항이
        // 삭제/재정렬됐다면 어긋날 수 있어, 그 경우 해당 문항은 적용 대상 없음(null)으로 둔다.
        List<CoverLetterItem> currentItems =
            coverLetterItemRepository.findByCoverLetterIdOrderByDisplayOrderAsc(review.getCoverLetterId());

        List<CoverLetterAiReviewItemView> items = new ArrayList<>();
        for (int i = 0; i < originals.size(); i++) {
            OriginalItemSnapshot original = originals.get(i);
            RevisedItemContent revision = i < revisions.size() ? revisions.get(i) : null;
            CoverLetterItem currentItem = i < currentItems.size() ? currentItems.get(i) : null;
            Long coverLetterItemId = currentItem != null ? currentItem.getCoverLetterItemId() : null;

            String currentAnswer = currentItem != null ? currentItem.getAnswer() : null;
            // "원문" 표시는 스냅샷이 낡아 보이지 않도록, 현재 답변이 스냅샷과 다르면(이유
            // 불문 — 이 리뷰를 적용했든, 다른 리뷰를 적용했든, 직접 수정했든) 항상 최신 답변을 보여준다.
            boolean changedSinceSnapshot = currentAnswer != null && !currentAnswer.equals(original.answer());
            String displayAnswer = changedSinceSnapshot ? currentAnswer : original.answer();

            // 반면 "이미 반영됨"(버튼 비활성화)은 훨씬 좁게 판단해야 한다 — 현재 답변이
            // "바로 이 리뷰가 제안한 수정안"과 정확히 일치할 때만이다. changedSinceSnapshot로
            // 판단하면, 다른 공고(리뷰) 제안을 적용했을 뿐인데 이력 드롭다운에서 무관한
            // 과거 리뷰를 열어봐도 "이미 반영됨"으로 잘못 뜨는 문제가 있었다.
            boolean resolved = revision != null
                && currentAnswer != null
                && currentAnswer.equals(revision.revisedAnswer());

            items.add(new CoverLetterAiReviewItemView(
                coverLetterItemId,
                original.question(),
                displayAnswer,
                original.additionalInfo(),
                resolved,
                revision != null ? revision.summary() : null,
                revision != null ? revision.improvementPoints() : List.of(),
                revision != null ? revision.revisedAnswer() : null
            ));
        }

        return new CoverLetterAiReviewDetail(
            review.getCoverLetterAiReviewId(),
            targetPosting != null ? targetPosting.getTitle() : "삭제된 공고",
            companyName,
            review.getCreatedAt(),
            items
        );
    }

    private String buildOverallFeedback(List<RevisedItemContent> revisedContent) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < revisedContent.size(); i++) {
            RevisedItemContent revision = revisedContent.get(i);
            if (revision == null) {
                // 생성에 실패한 문항 — 요약할 내용이 없다.
                continue;
            }
            String summary = revision.summary();
            if (summary == null || summary.isBlank()) {
                continue;
            }
            builder.append("문항 ").append(i + 1).append(": ").append(summary.trim()).append('\n');
        }
        return builder.toString().trim();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("첨삭 결과 저장에 실패했습니다.", exception);
        }
    }

    private <T> T readJson(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("첨삭 결과를 읽는 데 실패했습니다.", exception);
        }
    }

    /**
     * additionalInfo는 이 기능이 생기기 전에 만들어진 이력에는 없으므로 null로 역직렬화된다.
     * 별도 컬럼을 만들지 않고 기존 original_content JSON 안에 넣어 마이그레이션을 피했다.
     */
    private record OriginalItemSnapshot(String question, String answer, String additionalInfo) {
    }

    private record RevisedItemContent(
        String summary,
        List<String> improvementPoints,
        String revisedAnswer
    ) {
    }
}
