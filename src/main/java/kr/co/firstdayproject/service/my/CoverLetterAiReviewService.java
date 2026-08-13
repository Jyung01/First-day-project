package kr.co.firstdayproject.service.my;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kr.co.firstdayproject.dto.ai.CoverLetterAiReviewDetail;
import kr.co.firstdayproject.dto.ai.CoverLetterAiReviewItemView;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.coverletter.CoverLetterAiReview;
import kr.co.firstdayproject.entity.coverletter.CoverLetterItem;
import kr.co.firstdayproject.entity.job.JobPosting;
import kr.co.firstdayproject.exception.ResourceNotFoundException;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.repository.coverletter.CoverLetterAiReviewRepository;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.dto.ai.CoverLetterItemReviewResult;
import kr.co.firstdayproject.service.ai.CoverLetterItemReviewService;
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

    private final CoverLetterService coverLetterService;
    private final CoverLetterAiReviewRepository coverLetterAiReviewRepository;
    private final JobPostingRepository jobPostingRepository;
    private final CompanyRepository companyRepository;
    private final CoverLetterItemReviewService coverLetterItemReviewService;
    // 이 프로젝트는 spring-boot-starter-webmvc만 사용해 Jackson ObjectMapper 빈이
    // 자동 등록되지 않으므로, 전역 설정에 손대지 않고 이 서비스 전용으로 직접 생성한다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CoverLetterAiReviewService(
        CoverLetterService coverLetterService,
        CoverLetterAiReviewRepository coverLetterAiReviewRepository,
        JobPostingRepository jobPostingRepository,
        CompanyRepository companyRepository,
        @Lazy CoverLetterItemReviewService coverLetterItemReviewService
    ) {
        this.coverLetterService = coverLetterService;
        this.coverLetterAiReviewRepository = coverLetterAiReviewRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.companyRepository = companyRepository;
        this.coverLetterItemReviewService = coverLetterItemReviewService;
    }

    /**
     * 문항을 순서대로 첨삭하고 결과를 저장한다. 문항 수만큼 OpenAI를 순차 호출하므로
     * 응답까지 몇 초가 걸릴 수 있다 — 호출부(컨트롤러)가 완료를 기다린 뒤 리다이렉트한다.
     */
    @Transactional
    public Long requestReview(Long coverLetterId, Long userId, Long jobPostingId) {
        List<CoverLetterItem> items = coverLetterService.getItems(coverLetterId, userId);
        if (items.isEmpty()) {
            throw new IllegalStateException("첨삭할 문항이 없습니다.");
        }

        JobPosting targetPosting = jobPostingRepository.findById(jobPostingId)
            .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 채용공고입니다."));

        List<OriginalItemSnapshot> originalSnapshot = new ArrayList<>();
        List<RevisedItemContent> revisedContent = new ArrayList<>();

        for (CoverLetterItem item : items) {
            originalSnapshot.add(new OriginalItemSnapshot(item.getQuestion(), item.getAnswer()));

            CoverLetterItemReviewResult result = coverLetterItemReviewService.review(
                item.getQuestion(),
                item.getAnswer(),
                targetPosting
            );

            revisedContent.add(new RevisedItemContent(
                result.summary(),
                result.improvementPoints(),
                result.revisedAnswer()
            ));
        }

        CoverLetterAiReview review = CoverLetterAiReview.builder()
            .coverLetterId(coverLetterId)
            .jobPostingId(jobPostingId)
            .originalContent(writeJson(originalSnapshot))
            .revisedContent(writeJson(revisedContent))
            .feedback(buildOverallFeedback(revisedContent))
            .createdAt(LocalDateTime.now())
            .build();

        coverLetterAiReviewRepository.save(review);
        return review.getCoverLetterAiReviewId();
    }

    @Transactional(readOnly = true)
    public Optional<CoverLetterAiReviewDetail> getLatestReview(Long coverLetterId, Long userId) {
        coverLetterService.getMine(coverLetterId, userId);
        return coverLetterAiReviewRepository
            .findFirstByCoverLetterIdOrderByCreatedAtDesc(coverLetterId)
            .map(this::toDetail);
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

        List<CoverLetterAiReviewItemView> items = new ArrayList<>();
        for (int i = 0; i < originals.size(); i++) {
            OriginalItemSnapshot original = originals.get(i);
            RevisedItemContent revision = i < revisions.size() ? revisions.get(i) : null;

            items.add(new CoverLetterAiReviewItemView(
                original.question(),
                original.answer(),
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
            String summary = revisedContent.get(i).summary();
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

    private record OriginalItemSnapshot(String question, String answer) {
    }

    private record RevisedItemContent(
        String summary,
        List<String> improvementPoints,
        String revisedAnswer
    ) {
    }
}
