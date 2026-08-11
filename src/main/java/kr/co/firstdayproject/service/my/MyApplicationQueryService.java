package kr.co.firstdayproject.service.my;

import java.util.List;
import java.util.Map;
import java.util.Set;
import kr.co.firstdayproject.dto.my.MyApplicationFilterCounts;
import kr.co.firstdayproject.dto.my.MyApplicationListItem;
import kr.co.firstdayproject.dto.my.MyApplicationListProjection;
import kr.co.firstdayproject.dto.my.MyApplicationListResult;
import kr.co.firstdayproject.repository.application.ApplicationRepository;
import kr.co.firstdayproject.util.PageHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyApplicationQueryService {

    private static final int PAGE_SIZE = 5;

    private static final Set<String> APPLIED_STATUSES = Set.of(
            "지원완료"
    );
    private static final Set<String> PROGRESS_STATUSES = Set.of(
            "서류검토중",
            "서류합격",
            "면접예정",
            "면접완료"
    );
    private static final Set<String> RESULT_STATUSES = Set.of(
            "최종합격",
            "입사완료",
            "불합격",
            "지원취소",
            "채용종료"
    );
    private static final Set<String> ALL_STATUSES = Set.of(
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
    private static final Map<String, Set<String>> FILTER_STATUSES = Map.of(
            "all", ALL_STATUSES,
            "applied", APPLIED_STATUSES,
            "progress", PROGRESS_STATUSES,
            "result", RESULT_STATUSES
    );

    private final ApplicationRepository applicationRepository;

    public MyApplicationListResult getApplications(
            Long userId,
            String requestedFilter,
            int requestedPage
    ) {
        String filter = FILTER_STATUSES.containsKey(requestedFilter)
                ? requestedFilter
                : "all";
        Set<String> statuses = FILTER_STATUSES.get(filter);

        long total = applicationRepository
                .countByApplicantUserIdAndCurrentStatusIn(userId, statuses);
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

        List<MyApplicationListItem> applications = applicationRepository
                .findMyApplicationList(
                        userId,
                        statuses,
                        PageRequest.of(page - 1, PAGE_SIZE)
                )
                .stream()
                .map(this::toListItem)
                .toList();

        return new MyApplicationListResult(
                applications,
                getFilterCounts(userId),
                filter,
                pageHandler
        );
    }

    private MyApplicationFilterCounts getFilterCounts(Long userId) {
        return new MyApplicationFilterCounts(
                applicationRepository.countByApplicantUserId(userId),
                count(userId, APPLIED_STATUSES),
                count(userId, PROGRESS_STATUSES),
                count(userId, RESULT_STATUSES)
        );
    }

    private long count(Long userId, Set<String> statuses) {
        return applicationRepository
                .countByApplicantUserIdAndCurrentStatusIn(
                        userId,
                        statuses
                );
    }

    private MyApplicationListItem toListItem(
            MyApplicationListProjection projection
    ) {
        String status = projection.getCurrentStatus();

        return new MyApplicationListItem(
                projection.getApplicationId(),
                projection.getCompanyName(),
                projection.getCompanyLogoUrl(),
                StringUtils.hasText(projection.getCompanyName())
                        ? projection.getCompanyName().substring(0, 1)
                        : "?",
                projection.getJobTitle(),
                status,
                resolveStatusLabel(status),
                resolveStatusVariant(status),
                projection.getAppliedAt(),
                projection.getLatestChangedAt()
        );
    }

    private String resolveStatusLabel(String status) {
        return switch (status) {
            case "지원완료" -> "지원 완료";
            case "서류검토중" -> "서류 검토 중";
            case "서류합격" -> "서류 합격";
            case "면접예정" -> "면접 예정";
            case "면접완료" -> "면접 완료";
            case "최종합격" -> "최종 합격";
            case "입사완료" -> "입사 완료";
            case "지원취소" -> "지원 취소";
            case "채용종료" -> "채용 종료";
            default -> status;
        };
    }

    private String resolveStatusVariant(String status) {
        return switch (status) {
            case "지원완료" -> "applied";
            case "서류검토중" -> "reviewing";
            case "서류합격" -> "document-passed";
            case "면접예정" -> "interview";
            case "면접완료" -> "interviewed";
            case "최종합격" -> "passed";
            case "입사완료" -> "joined";
            case "불합격" -> "rejected";
            case "지원취소", "채용종료" -> "canceled";
            default -> "applied";
        };
    }
}
