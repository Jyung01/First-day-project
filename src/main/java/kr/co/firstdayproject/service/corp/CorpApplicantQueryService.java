package kr.co.firstdayproject.service.corp;

import java.time.LocalDate;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.co.firstdayproject.dto.corp.applicant.CorpApplicantDetailProjection;
import kr.co.firstdayproject.dto.corp.applicant.CorpApplicantDetailView;
import kr.co.firstdayproject.dto.corp.applicant.CorpApplicationStatusHistoryItem;
import kr.co.firstdayproject.dto.corp.applicant.CorpApplicantListItem;
import kr.co.firstdayproject.dto.corp.applicant.CorpApplicantListProjection;
import kr.co.firstdayproject.dto.corp.applicant.CorpApplicantListResult;
import kr.co.firstdayproject.dto.corp.applicant.CorpApplicantSummary;
import kr.co.firstdayproject.repository.application.ApplicationRepository;
import kr.co.firstdayproject.repository.application.ApplicationMemoRepository;
import kr.co.firstdayproject.repository.application.ApplicationStatusHistoryRepository;
import kr.co.firstdayproject.repository.member.UserRepository;
import kr.co.firstdayproject.exception.ResourceNotFoundException;
import kr.co.firstdayproject.util.PageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CorpApplicantQueryService {

    private static final int PAGE_SIZE = 8;
    private static final Set<String> APPLICATION_STATUSES = Set.of(
            "지원완료",
            "서류검토중",
            "서류합격",
            "면접예정",
            "면접완료",
            "최종합격",
            "입사완료",
            "불합격",
            "지원취소",
            "채용종료"
    );

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final ApplicationMemoRepository applicationMemoRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public CorpApplicantDetailView getApplicantDetail(
            Long companyId,
            Long applicationId
    ) {
        if (companyId == null) {
            throw new IllegalArgumentException("기업 정보가 필요합니다.");
        }

        CorpApplicantDetailProjection application = applicationRepository
                .findCorpApplicantDetail(companyId, applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "지원 정보를 찾을 수 없습니다."
                ));
        Map<String, Object> resume = readSnapshot(
                application.getResumeSnapshotJson()
        );
        Map<String, Object> coverLetter = readSnapshot(
                application.getCoverLetterSnapshotJson()
        );
        String applicantName = getSnapshotText(
                resume,
                "applicantName",
                application.getApplicantName()
        );
        String email = getSnapshotText(
                resume,
                "email",
                application.getApplicantEmail()
        );
        String phone = getSnapshotText(
                resume,
                "phone",
                application.getApplicantPhone()
        );
        var memo = applicationMemoRepository
                .findFirstByApplicationIdOrderByUpdatedAtDescApplicationMemoIdDesc(
                        applicationId
                )
                .orElse(null);
        String memoAuthorName = memo == null
                ? null
                : userRepository.findById(memo.getAuthorUserId())
                        .map(user -> user.getName())
                        .orElse("탈퇴한 담당자");

        return new CorpApplicantDetailView(
                application.getApplicationId(),
                applicantName,
                getInitial(applicantName),
                email,
                phone,
                application.getJobTitle(),
                application.getAppliedAt(),
                application.getCurrentStatus(),
                resolveStatusVariant(application.getCurrentStatus()),
                ApplicationStatusTransitionPolicy.getAllowedNextStatuses(
                        application.getCurrentStatus()
                ),
                resume,
                coverLetter,
                getStatusHistory(application),
                memo == null ? null : memo.getMemo(),
                memoAuthorName,
                memo == null ? null : memo.getUpdatedAt()
        );
    }

    public CorpApplicantListResult getApplicants(
            Long companyId,
            Long jobPostingId,
            String requestedStatus,
            String requestedKeyword,
            int requestedPage
    ) {
        if (companyId == null) {
            throw new IllegalArgumentException("기업 정보가 필요합니다.");
        }

        String status = requestedStatus != null
                && APPLICATION_STATUSES.contains(requestedStatus)
                ? requestedStatus
                : null;
        String keyword = normalizeKeyword(requestedKeyword);
        long total = applicationRepository.countCorpApplicantList(
                companyId,
                jobPostingId,
                status,
                keyword
        );
        int lastPage = Math.max(
                1,
                (int) Math.ceil((double) total / PAGE_SIZE)
        );
        int page = Math.min(
                Math.max(requestedPage, 1),
                lastPage
        );
        PageHandler pageHandler = new PageHandler(
                page,
                Math.toIntExact(total),
                PAGE_SIZE
        );

        var applicants = applicationRepository.findCorpApplicantList(
                        companyId,
                        jobPostingId,
                        status,
                        keyword,
                        PageRequest.of(page - 1, PAGE_SIZE)
                )
                .stream()
                .map(this::toListItem)
                .toList();

        return new CorpApplicantListResult(
                applicants,
                applicationRepository.findCorpApplicantJobOptions(companyId),
                getSummary(companyId),
                jobPostingId,
                status,
                keyword == null ? "" : keyword,
                pageHandler
        );
    }

    private CorpApplicantSummary getSummary(Long companyId) {
        LocalDate today = LocalDate.now();

        return new CorpApplicantSummary(
                applicationRepository.countCorpApplicants(companyId),
                applicationRepository.countCorpWaitingApplicants(companyId),
                applicationRepository.countCorpApplicantsAppliedBetween(
                        companyId,
                        today.atStartOfDay(),
                        today.plusDays(1).atStartOfDay()
                )
        );
    }

    private List<CorpApplicationStatusHistoryItem> getStatusHistory(
            CorpApplicantDetailProjection application
    ) {
        var histories = statusHistoryRepository
                .findByApplicationIdOrderByChangedAtAscApplicationStatusIdAsc(
                        application.getApplicationId()
                );
        List<CorpApplicationStatusHistoryItem> result = new ArrayList<>();

        boolean hasAppliedHistory = histories.stream()
                .anyMatch(history -> "지원완료".equals(
                        history.getToStatus()
                ));
        if (!hasAppliedHistory) {
            result.add(new CorpApplicationStatusHistoryItem(
                    "지원완료",
                    null,
                    "지원자",
                    application.getAppliedAt()
            ));
        }

        histories.stream()
                .map(history -> new CorpApplicationStatusHistoryItem(
                        history.getToStatus(),
                        history.getChangeReason(),
                        history.getActorType(),
                        history.getChangedAt()
                ))
                .forEach(result::add);

        return result;
    }

    private CorpApplicantListItem toListItem(
            CorpApplicantListProjection projection
    ) {
        Map<String, Object> snapshot = readSnapshot(
                projection.getResumeSnapshotJson()
        );
        String applicantName = getSnapshotText(
                snapshot,
                "applicantName",
                projection.getApplicantName()
        );
        String careerType = getSnapshotText(
                snapshot,
                "careerType",
                "경력 정보 없음"
        );

        return new CorpApplicantListItem(
                projection.getApplicationId(),
                applicantName,
                getInitial(applicantName),
                projection.getJobTitle(),
                projection.getAppliedAt(),
                resolveApplicationTypeLabel(careerType),
                resolveApplicationTypeVariant(careerType),
                projection.getCurrentStatus(),
                resolveStatusVariant(projection.getCurrentStatus())
        );
    }

    private String resolveApplicationTypeLabel(String careerType) {
        return switch (careerType) {
            case "신입" -> "신입 지원";
            case "경력" -> "경력 지원";
            default -> "구분 없음";
        };
    }

    private String resolveApplicationTypeVariant(String careerType) {
        return "경력".equals(careerType) ? "experienced" : "entry";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readSnapshot(String snapshotJson) {
        if (!StringUtils.hasText(snapshotJson)) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(snapshotJson, Map.class);
        } catch (JacksonException exception) {
            return Map.of();
        }
    }

    private String getSnapshotText(
            Map<String, Object> snapshot,
            String key,
            String fallback
    ) {
        Object value = snapshot.get(key);
        if (value instanceof String text && StringUtils.hasText(text)) {
            return text;
        }
        return fallback;
    }

    private String getInitial(String name) {
        if (!StringUtils.hasText(name)) {
            return "?";
        }
        return name.substring(0, 1);
    }

    private String resolveStatusVariant(String status) {
        return switch (status) {
            case "서류합격", "면접예정", "면접완료",
                 "최종합격", "입사완료" -> "green";
            case "불합격", "지원취소", "채용종료" -> "red";
            case "서류검토중" -> "orange";
            default -> "default";
        };
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim();
    }
}
