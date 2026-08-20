package kr.co.firstdayproject.dto.corp.job;

import java.util.List;

public record JobPostingAiPolishRequest(
        String fieldType,
        String content,
        String jobTitle,
        String jobCategory,
        String employmentType,
        String careerType,
        String educationLevel,
        String workRegion,
        List<String> skillNames,
        String previousResult
) {
}
