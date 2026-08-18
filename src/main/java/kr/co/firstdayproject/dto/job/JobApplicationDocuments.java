package kr.co.firstdayproject.dto.job;

import java.time.LocalDateTime;
import java.util.List;

public record JobApplicationDocuments(
        List<DocumentItem> resumes,
        List<DocumentItem> coverLetters
) {

    public static JobApplicationDocuments empty() {
        return new JobApplicationDocuments(List.of(), List.of());
    }

    public record DocumentItem(
            Long id,
            String title,
            LocalDateTime updatedAt
    ) {
    }
}
