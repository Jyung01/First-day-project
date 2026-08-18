package kr.co.firstdayproject.dto.corp.job;

public record JobPostingAiPolishResponse(
        boolean success,
        String polishedContent,
        String message
) {

    public static JobPostingAiPolishResponse success(String polishedContent) {
        return new JobPostingAiPolishResponse(true, polishedContent, null);
    }

    public static JobPostingAiPolishResponse failure(String message) {
        return new JobPostingAiPolishResponse(false, null, message);
    }
}
