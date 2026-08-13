package kr.co.firstdayproject.service.ai;

import java.util.List;
import kr.co.firstdayproject.dto.ai.CoverLetterItemReviewResult;
import kr.co.firstdayproject.entity.job.JobPosting;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 자기소개서 문항 1개를 대상 채용공고 요구사항에 맞춰 첨삭하는 최소 단위 로직.
 * 여러 문항을 순회하며 저장하는 오케스트레이션은 이 서비스를 호출하는 상위 서비스가 담당한다.
 */
@Service
@Profile("!test")
public class CoverLetterItemReviewService {

    private static final String SYSTEM_PROMPT = """
        당신은 채용 자기소개서 첨삭 전문가입니다.
        지원자가 선택한 채용공고의 요구사항에 맞춰, 자기소개서 문항 하나에 대한 첨삭을 제공합니다.

        원칙:
        - 가능하면 STAR(상황-과제-행동-결과) 구조로 답변이 구체화되도록 제안하세요.
        - 지원자가 실제로 경험하지 않은 사실이나 수치를 지어내지 마세요. 기존 답변에 있는 내용을 다듬고 구조화하는 데 집중하세요.
        - 대상 채용공고의 요구사항·우대사항과 최대한 연결지어 첨삭하세요.
        - summary는 한두 문장으로 이번 첨삭의 핵심을 요약하세요.
        - improvementPoints는 구체적인 개선 포인트를 항목별로 나열하세요.
        - revisedAnswer는 반드시 지원자가 실제로 제출할 수 있는 완성된 1인칭 자기소개서 문장으로 작성하세요.
          "[프로젝트명]", "[기간]"처럼 지원자가 채워 넣어야 할 안내문이나 빈칸(대괄호 placeholder)을
          절대 포함하지 마세요. 원문이 짧아 구체화할 내용이 부족하더라도, 어떻게 써야 하는지 가이드만
          제시하지 말고 있는 내용 안에서 최대한 자연스러운 완성 문장으로 다듬어 작성하세요.
        """;

    private final ChatClient chatClient;
    private final SimilarJobPostingSearchService similarJobPostingSearchService;

    public CoverLetterItemReviewService(
        ChatClient.Builder chatClientBuilder,
        SimilarJobPostingSearchService similarJobPostingSearchService
    ) {
        this.chatClient = chatClientBuilder.build();
        this.similarJobPostingSearchService = similarJobPostingSearchService;
    }

    public CoverLetterItemReviewResult review(
        String question,
        String answer,
        JobPosting targetPosting
    ) {
        List<Document> similarPostings = similarJobPostingSearchService.findSimilarPostings(
            answer,
            targetPosting.getJobCategoryId(),
            targetPosting.getJobPostingId(),
            3
        );

        String userPrompt = buildUserPrompt(question, answer, targetPosting, similarPostings);

        return chatClient.prompt()
            .system(SYSTEM_PROMPT)
            .user(userPrompt)
            .call()
            .entity(CoverLetterItemReviewResult.class);
    }

    private String buildUserPrompt(
        String question,
        String answer,
        JobPosting targetPosting,
        List<Document> similarPostings
    ) {
        StringBuilder builder = new StringBuilder();

        builder.append("[대상 채용공고]\n");
        appendSection(builder, "제목", targetPosting.getTitle());
        appendSection(builder, "주요업무", targetPosting.getMainTasks());
        appendSection(builder, "자격요건", targetPosting.getQualifications());
        appendSection(builder, "우대사항", targetPosting.getPreferredConditions());
        appendSection(builder, "소개", targetPosting.getIntroduction());

        if (!similarPostings.isEmpty()) {
            builder.append("\n[같은 직무군의 유사 채용공고 — 이 직무군에서 자주 요구되는 역량 참고용]\n");
            for (Document document : similarPostings) {
                builder.append("- ").append(document.getText()).append('\n');
            }
        }

        builder.append("\n[자기소개서 문항]\n").append(question).append('\n');
        builder.append("\n[지원자 답변]\n").append(answer).append('\n');

        return builder.toString();
    }

    private void appendSection(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append(label).append(": ").append(value.trim()).append('\n');
    }
}
