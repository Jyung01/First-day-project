package kr.co.firstdayproject.dto.main;

/** 메인에서 개인회원의 취업 준비 단계를 표시하기 위한 읽기 전용 DTO. */
public record JobPreparationProgress(
        boolean desiredJobCompleted,
        boolean resumeCompleted,
        boolean coverLetterCompleted,
        boolean applicationStarted,
        int completedCount,
        String nextTitle,
        String nextHref,
        String nextActionLabel
) {
}
