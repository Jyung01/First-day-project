package kr.co.firstdayproject.service.corp;

import java.time.LocalDateTime;
import kr.co.firstdayproject.entity.application.ApplicationMemo;
import kr.co.firstdayproject.entity.application.ApplicationStatusHistory;
import kr.co.firstdayproject.exception.ResourceNotFoundException;
import kr.co.firstdayproject.repository.application.ApplicationMemoRepository;
import kr.co.firstdayproject.repository.application.ApplicationRepository;
import kr.co.firstdayproject.repository.application.ApplicationStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CorpApplicantManagementService {

    private static final int MAX_MEMO_LENGTH = 2000;

    private final ApplicationRepository applicationRepository;
    private final ApplicationMemoRepository applicationMemoRepository;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;

    @Transactional
    public void changeStatus(
            Long companyId,
            Long changedBy,
            Long applicationId,
            String nextStatus
    ) {
        if (companyId == null || changedBy == null) {
            throw new IllegalArgumentException("기업 담당자 정보가 필요합니다.");
        }

        var application = applicationRepository
                .findCorpApplicationForUpdate(companyId, applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "지원 정보를 찾을 수 없습니다."
                ));
        String currentStatus = application.getCurrentStatus();

        if (!ApplicationStatusTransitionPolicy.canTransition(
                currentStatus,
                nextStatus
        )) {
            throw new IllegalArgumentException(
                    "현재 상태에서 선택한 상태로 변경할 수 없습니다."
            );
        }

        LocalDateTime now = LocalDateTime.now();
        application.setCurrentStatus(nextStatus);
        application.setUpdatedAt(now);
        statusHistoryRepository.save(ApplicationStatusHistory.builder()
                .applicationId(applicationId)
                .fromStatus(currentStatus)
                .toStatus(nextStatus)
                .changedBy(changedBy)
                .actorType("기업")
                .changedAt(now)
                .build());
    }

    @Transactional
    public void saveMemo(
            Long companyId,
            Long authorUserId,
            Long applicationId,
            String requestedMemo
    ) {
        if (companyId == null || authorUserId == null) {
            throw new IllegalArgumentException("기업 담당자 정보가 필요합니다.");
        }

        applicationRepository.findCorpApplicationForUpdate(
                        companyId,
                        applicationId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "지원 정보를 찾을 수 없습니다."
                ));

        ApplicationMemo savedMemo = applicationMemoRepository
                .findFirstByApplicationIdOrderByUpdatedAtDescApplicationMemoIdDesc(
                        applicationId
                )
                .orElse(null);
        String memo = normalizeMemo(requestedMemo);

        if (!StringUtils.hasText(memo)) {
            if (savedMemo != null) {
                applicationMemoRepository.delete(savedMemo);
            }
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (savedMemo == null) {
            applicationMemoRepository.save(ApplicationMemo.builder()
                    .applicationId(applicationId)
                    .authorUserId(authorUserId)
                    .memo(memo)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            return;
        }

        savedMemo.setAuthorUserId(authorUserId);
        savedMemo.setMemo(memo);
        savedMemo.setUpdatedAt(now);
    }

    private String normalizeMemo(String requestedMemo) {
        if (!StringUtils.hasText(requestedMemo)) {
            return null;
        }

        String memo = requestedMemo.trim();
        if (memo.length() > MAX_MEMO_LENGTH) {
            throw new IllegalArgumentException(
                    "담당자 메모는 2,000자 이내로 입력해주세요."
            );
        }
        return memo;
    }
}
