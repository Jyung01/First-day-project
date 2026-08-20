package kr.co.firstdayproject.service.main;

import kr.co.firstdayproject.dto.main.JobPreparationProgress;
import kr.co.firstdayproject.repository.application.ApplicationRepository;
import kr.co.firstdayproject.repository.coverletter.CoverLetterRepository;
import kr.co.firstdayproject.repository.job.UserDesiredJobRepository;
import kr.co.firstdayproject.repository.resume.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobPreparationProgressService {

    private final UserDesiredJobRepository userDesiredJobRepository;
    private final ResumeRepository resumeRepository;
    private final CoverLetterRepository coverLetterRepository;
    private final ApplicationRepository applicationRepository;

    public JobPreparationProgress getProgress(Long userId) {
        boolean desiredJobCompleted = userDesiredJobRepository.existsByIdUserId(userId);
        // 준비 현황은 순서형 안내다. 뒤 단계의 과거 데이터가 있더라도 앞 단계가
        // 미완료이면 건너뛰어 체크하지 않아야 현재 사용자가 할 일을 알 수 있다.
        boolean resumeCompleted = desiredJobCompleted
                && resumeRepository.countByUserIdAndDeletedAtIsNull(userId) > 0;
        boolean coverLetterCompleted = resumeCompleted
                && coverLetterRepository.countByUserIdAndDeletedAtIsNull(userId) > 0;
        boolean applicationStarted = coverLetterCompleted
                && applicationRepository.countByApplicantUserId(userId) > 0;
        int completedCount = countCompleted(
                desiredJobCompleted, resumeCompleted, coverLetterCompleted, applicationStarted
        );

        if (!desiredJobCompleted) {
            return progress(desiredJobCompleted, resumeCompleted, coverLetterCompleted, applicationStarted,
                    completedCount, "희망 직무를 설정해 보세요.", "/my/profile-edit", "희망 직무 설정하기");
        }
        if (!resumeCompleted) {
            return progress(desiredJobCompleted, resumeCompleted, coverLetterCompleted, applicationStarted,
                    completedCount, "이력서를 등록해 보세요.", "/my/resume/form", "이력서 작성하기");
        }
        if (!coverLetterCompleted) {
            return progress(desiredJobCompleted, resumeCompleted, coverLetterCompleted, applicationStarted,
                    completedCount, "자기소개서를 작성해 보세요.", "/my/cover-letter/form", "자기소개서 작성하기");
        }
        if (!applicationStarted) {
            return progress(desiredJobCompleted, resumeCompleted, coverLetterCompleted, applicationStarted,
                    completedCount, "준비가 끝났어요. 관심 가는 공고에 지원해 보세요.", "/job/list", "채용공고 둘러보기");
        }
        return progress(desiredJobCompleted, resumeCompleted, coverLetterCompleted, applicationStarted,
                completedCount, "취업 준비가 완료됐어요. 지원 현황을 확인해 보세요.", "/my/applications", "지원 현황 보기");
    }

    private JobPreparationProgress progress(
            boolean desiredJobCompleted, boolean resumeCompleted, boolean coverLetterCompleted,
            boolean applicationStarted, int completedCount, String nextTitle, String nextHref,
            String nextActionLabel
    ) {
        return new JobPreparationProgress(
                desiredJobCompleted, resumeCompleted, coverLetterCompleted, applicationStarted,
                completedCount, nextTitle, nextHref, nextActionLabel
        );
    }

    private int countCompleted(boolean... steps) {
        int count = 0;
        for (boolean step : steps) {
            if (step) count++;
        }
        return count;
    }
}
