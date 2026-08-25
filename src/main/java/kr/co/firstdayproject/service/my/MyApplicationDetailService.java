package kr.co.firstdayproject.service.my;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import kr.co.firstdayproject.dto.my.MyApplicationDetailProjection;
import kr.co.firstdayproject.dto.my.MyApplicationDetailView;
import kr.co.firstdayproject.dto.my.MyApplicationHistoryItem;
import kr.co.firstdayproject.entity.application.ApplicationStatusHistory;
import kr.co.firstdayproject.exception.ResourceNotFoundException;
import kr.co.firstdayproject.repository.application.ApplicationRepository;
import kr.co.firstdayproject.repository.application.ApplicationStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class MyApplicationDetailService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public MyApplicationDetailView getDetail(
            Long userId,
            Long applicationId
    ) {
        MyApplicationDetailProjection application = applicationRepository
                .findMyApplicationDetail(userId, applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "지원 내역을 찾을 수 없습니다."
                ));
        String companyName = application.getCompanyName();

        return new MyApplicationDetailView(
                application.getApplicationId(),
                application.getJobPostingId(),
                companyName,
                application.getCompanyLogoUrl(),
                StringUtils.hasText(companyName)
                        ? companyName.substring(0, 1)
                        : "?",
                application.getJobTitle(),
                application.getCurrentStatus(),
                "이용정지".equals(application.getCompanyStatus())
                        ? "전형 일시 중지"
                        : statusLabel(application.getCurrentStatus()),
                "이용정지".equals(application.getCompanyStatus())
                        ? "suspended"
                        : statusVariant(application.getCurrentStatus()),
                "이용정지".equals(application.getCompanyStatus()),
                "탈퇴".equals(application.getCompanyStatus()),
                application.getAppliedAt(),
                readSnapshot(application.getResumeSnapshotJson()),
                readSnapshot(application.getCoverLetterSnapshotJson()),
                getHistory(application)
        );
    }

    @Transactional
    public void cancel(Long userId, Long applicationId) {
        var application = applicationRepository
                .findMyApplicationForUpdate(userId, applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "지원 내역을 찾을 수 없습니다."
                ));

        if (!"지원완료".equals(application.getCurrentStatus())) {
            throw new IllegalArgumentException(
                    "기업이 검토를 시작한 지원은 취소할 수 없습니다."
            );
        }

        LocalDateTime now = LocalDateTime.now();
        application.setCurrentStatus("지원취소");
        application.setCancelledAt(now);
        application.setUpdatedAt(now);
        statusHistoryRepository.save(ApplicationStatusHistory.builder()
                .applicationId(applicationId)
                .fromStatus("지원완료")
                .toStatus("지원취소")
                .changedBy(userId)
                .actorType("지원자")
                .changedAt(now)
                .build());
    }

    private List<MyApplicationHistoryItem> getHistory(
            MyApplicationDetailProjection application
    ) {
        var histories = statusHistoryRepository
                .findByApplicationIdOrderByChangedAtAscApplicationStatusIdAsc(
                        application.getApplicationId()
                );
        List<MyApplicationHistoryItem> result = new ArrayList<>();
        boolean hasApplied = histories.stream()
                .anyMatch(history -> "지원완료".equals(history.getToStatus()));

        if (!hasApplied) {
            result.add(new MyApplicationHistoryItem(
                    "지원완료",
                    "지원 완료",
                    application.getAppliedAt(),
                    null
            ));
        }
        histories.stream()
                .map(history -> new MyApplicationHistoryItem(
                        history.getToStatus(),
                        statusLabel(history.getToStatus()),
                        history.getChangedAt(),
                        history.getChangeReason()
                ))
                .forEach(result::add);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readSnapshot(String json) {
        if (!StringUtils.hasText(json)) return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JacksonException exception) {
            return Map.of();
        }
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "지원완료" -> "지원 완료";
            case "서류검토중" -> "서류 검토 중";
            case "서류합격" -> "서류 합격";
            case "면접예정" -> "면접 예정";
            case "면접완료" -> "면접 완료";
            case "최종합격" -> "최종 합격";
            case "입사완료" -> "입사 완료";
            case "입사포기" -> "입사 포기";
            case "지원취소" -> "지원 취소";
            case "채용종료" -> "채용 종료";
            default -> status;
        };
    }

    private String statusVariant(String status) {
        return switch (status) {
            case "지원완료" -> "applied";
            case "서류검토중" -> "reviewing";
            case "서류합격" -> "document-passed";
            case "면접예정" -> "interview";
            case "면접완료" -> "interviewed";
            case "최종합격" -> "passed";
            case "입사완료" -> "joined";
            case "입사포기" -> "canceled";
            case "불합격" -> "rejected";
            default -> "canceled";
        };
    }
}
