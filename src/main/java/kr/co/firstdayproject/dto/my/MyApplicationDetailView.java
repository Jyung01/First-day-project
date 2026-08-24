package kr.co.firstdayproject.dto.my;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public record MyApplicationDetailView(
        Long applicationId,
        Long jobPostingId,
        String companyName,
        String companyLogoUrl,
        String companyInitial,
        String jobTitle,
        String currentStatus,
        String statusLabel,
        String statusVariant,
        boolean processSuspended,
        boolean companyWithdrawn,
        LocalDateTime appliedAt,
        Map<String, Object> resume,
        Map<String, Object> coverLetter,
        List<MyApplicationHistoryItem> statusHistory
) {
    public boolean hasCoverLetter() {
        return !coverLetter.isEmpty();
    }

    public boolean canCancel() {
        return "지원완료".equals(currentStatus);
    }

    public boolean companyUnavailable() {
        return processSuspended || companyWithdrawn;
    }

    public List<MyApplicationProgressStage> progressStages() {
        if ("불합격".equals(currentStatus)) {
            return rejectedProgressStages();
        }

        List<MyApplicationProgressStage> stages = new ArrayList<>();
        boolean terminal = isTerminalStatus(currentStatus);

        addProgressStage(stages, "지원완료", "지원 완료", terminal);
        addProgressStage(
                stages,
                "서류검토중",
                documentReviewLabel(),
                terminal
        );
        addProgressStage(stages, "서류합격", "서류 합격", terminal);
        addProgressStage(
                stages,
                List.of("면접예정", "면접완료"),
                "면접 전형",
                terminal
        );
        addProgressStage(stages, "최종합격", "최종 합격", terminal);
        if (!"입사포기".equals(currentStatus)) {
            addProgressStage(stages, "입사완료", "입사 완료", terminal);
        }

        if (terminal) {
            MyApplicationHistoryItem history = findHistory(currentStatus);
            stages.add(new MyApplicationProgressStage(
                    currentStatus,
                    statusLabel(currentStatus),
                    history == null ? null : history.changedAt(),
                    "terminated"
            ));
        }

        return stages;
    }

    private List<MyApplicationProgressStage> rejectedProgressStages() {
        List<MyApplicationProgressStage> stages = new ArrayList<>();
        MyApplicationHistoryItem rejection = findHistory("불합격");
        boolean rejectedAfterInterview = statusHistory.stream()
                .anyMatch(history -> List.of(
                        "면접예정",
                        "면접완료"
                ).contains(history.status()));

        addProgressStage(stages, "지원완료", "지원 완료", true);

        if (rejectedAfterInterview) {
            addProgressStage(stages, "서류검토중", "서류 검토", true);
            addProgressStage(stages, "서류합격", "서류 합격", true);
            addProgressStage(
                    stages,
                    List.of("면접예정", "면접완료"),
                    "면접 전형",
                    true
            );
            stages.add(rejectedStage(
                    "면접불합격",
                    "면접 불합격",
                    rejection
            ));
            stages.add(plannedStage("입사완료", "입사 완료"));
        } else {
            addProgressStage(stages, "서류검토중", "서류 검토", true);
            stages.add(rejectedStage(
                    "서류불합격",
                    "서류 불합격",
                    rejection
            ));
            stages.add(plannedStage("면접전형", "면접 전형"));
            stages.add(plannedStage("최종합격", "최종 합격"));
            stages.add(plannedStage("입사완료", "입사 완료"));
        }

        return stages;
    }

    private String documentReviewLabel() {
        return "서류검토중".equals(currentStatus)
                ? "서류 검토 중"
                : "서류 검토";
    }

    private MyApplicationProgressStage rejectedStage(
            String status,
            String label,
            MyApplicationHistoryItem rejection
    ) {
        return new MyApplicationProgressStage(
                status,
                label,
                rejection == null ? null : rejection.changedAt(),
                "terminated"
        );
    }

    private MyApplicationProgressStage plannedStage(
            String status,
            String label
    ) {
        return new MyApplicationProgressStage(
                status,
                label,
                null,
                "planned"
        );
    }

    private void addProgressStage(
            List<MyApplicationProgressStage> stages,
            String status,
            String label,
            boolean terminal
    ) {
        addProgressStage(stages, List.of(status), label, terminal);
    }

    private void addProgressStage(
            List<MyApplicationProgressStage> stages,
            List<String> statuses,
            String label,
            boolean terminal
    ) {
        MyApplicationHistoryItem history = statusHistory.stream()
                .filter(item -> statuses.contains(item.status()))
                .reduce((first, second) -> second)
                .orElse(null);
        String state;

        if (!terminal && statuses.contains(currentStatus)) {
            state = "current";
        } else if (history != null) {
            state = "complete";
        } else {
            state = "planned";
        }

        stages.add(new MyApplicationProgressStage(
                statuses.getFirst(),
                label,
                history == null ? null : history.changedAt(),
                state
        ));
    }

    private MyApplicationHistoryItem findHistory(String status) {
        return statusHistory.stream()
                .filter(history -> status.equals(history.status()))
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private boolean isTerminalStatus(String status) {
        return List.of(
                "불합격",
                "지원취소",
                "입사포기",
                "채용종료"
        ).contains(status);
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

    public String applicantName() {
        return text("applicantName", "지원자");
    }

    public String email() {
        return text("email", "-");
    }

    public String phone() {
        return text("phone", "-");
    }

    private String text(String key, String fallback) {
        Object value = resume.get(key);
        return value instanceof String text && !text.isBlank()
                ? text
                : fallback;
    }
}
