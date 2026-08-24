package kr.co.firstdayproject.service.corp;

import java.util.List;
import java.util.Map;

public final class ApplicationStatusTransitionPolicy {

    private static final Map<String, List<String>> TRANSITIONS = Map.of(
            "지원완료", List.of("서류검토중", "불합격"),
            "서류검토중", List.of("서류합격", "불합격"),
            "서류합격", List.of("면접예정", "불합격"),
            "면접예정", List.of("면접완료", "불합격"),
            "면접완료", List.of("최종합격", "불합격"),
            "최종합격", List.of("입사완료", "입사포기")
    );

    private ApplicationStatusTransitionPolicy() {
    }

    public static List<String> getAllowedNextStatuses(
            String currentStatus
    ) {
        return TRANSITIONS.getOrDefault(currentStatus, List.of());
    }

    public static boolean canTransition(
            String currentStatus,
            String nextStatus
    ) {
        return getAllowedNextStatuses(currentStatus).contains(nextStatus);
    }
}
