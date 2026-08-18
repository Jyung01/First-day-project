package kr.co.firstdayproject.service.ai;

import java.util.List;
import kr.co.firstdayproject.dto.ai.CoverLetterItemReviewOutcome;
import kr.co.firstdayproject.dto.ai.CoverLetterItemReviewResult;
import kr.co.firstdayproject.dto.ai.RagEvidence;
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

    /**
     * revisedAnswer(다시 쓴 답변)와 improvementPoints(보강 제안)의 역할을 명확히 갈라놓는 것이
     * 이 프롬프트의 핵심이다. 둘을 구분하지 않으면 "구체화하라"는 요구와 "지어내지 말라"는 요구가
     * 한 칸 안에서 충돌하고, 원문이 얇을수록 모델에게 남는 선택지는 지어내는 것뿐이 된다.
     * 실제로 그렇게 해서 원문에 없던 성과·기여 문장이 본문에 섞여 들어온 적이 있다.
     * 없는 내용은 본문이 아니라 제안 칸에 질문으로 남겨, 지원자가 직접 채우게 한다.
     */
    private static final String SYSTEM_PROMPT = """
        당신은 채용 자기소개서 첨삭 전문가입니다.
        지원자가 선택한 채용공고의 요구사항에 맞춰, 자기소개서 문항 하나에 대한 첨삭을 제공합니다.

        가장 중요한 원칙 — 사실을 만들어내지 않습니다:
        - revisedAnswer에는 지원자의 원문에 있는 사실만 사용하세요. 원문에 없는 경험·성과·수치·기술·
          협업 내용을 새로 만들어 넣지 마세요. 표현과 구조만 다듬습니다.
        - 원문에 근거가 없는데 넣고 싶은 내용이 있다면 revisedAnswer가 아니라 improvementPoints에
          "~한 경험이 있다면 이 부분에 덧붙이세요"처럼 제안 형태로 쓰세요.
        - revisedAnswer가 원문보다 길어질 필요는 없습니다. 원문이 짧으면 짧은 채로 다듬으세요.
          분량을 늘리려고 내용을 지어내는 것이 가장 나쁜 결과입니다.

        revisedAnswer 작성 방법:
        - 지원자가 그대로 제출할 수 있는 완성된 1인칭 문장으로 쓰세요.
        - "[프로젝트명]", "[기간]"처럼 지원자가 채워 넣어야 할 빈칸(대괄호 placeholder)이나
          "~를 추가하세요" 같은 안내문을 본문에 넣지 마세요. 그런 내용은 improvementPoints로 보내세요.
        - STAR(상황-과제-행동-결과) 구조는 원문에 있는 내용만으로 재배치할 수 있을 때만 적용하세요.
          구조를 채우려고 없는 결과를 만들어내지 마세요.

        사실을 더하지 않고도 해야 하는 편집 — 이것까지 하지 않으면 첨삭이 아닙니다:
        - 두괄식으로 돌리세요. 무엇을 했고 무엇을 고민했는지를 앞에 두고, 계기나 배경은 뒤로 보내세요.
        - 힘 빠지는 소감 표현을 걷어내세요. "흥미를 느꼈습니다", "인상 깊었습니다", "좋은 경험이었습니다"
          같은 문장은 정보를 담고 있지 않습니다. 그 자리에 무엇을 했는지가 오게 하세요.
        - 늘어지는 문장은 나누고, 같은 말을 두 번 하는 곳은 하나로 합치세요.
        - 대상 공고가 쓰는 용어에 표현을 맞추세요. 다만 사실이 아니라 어휘 수준에서만입니다.
        - 어미만 바꾸거나 단어를 동의어로 교체한 결과는 첨삭이 아닙니다. 원문과 거의 같은 문장을
          돌려주지 마세요. 바꿀 이유가 없는 문장은 차라리 그대로 두세요.
        - 내용을 담지 않은 추상적인 채움말로 문장을 늘리지 마세요.
          "실무적 고려사항을 경험했습니다", "다양한 경험을 쌓았습니다", "많은 것을 배웠습니다" 같은
          표현은 읽고 나도 무엇을 했는지 알 수 없습니다. 구체적으로 쓸 재료가 없으면 그 문장을
          아예 쓰지 말고, 무엇을 채우면 좋을지 아래 개선 포인트로 안내하세요.
          결과가 짧아지는 것은 괜찮습니다. 빈 문장으로 분량을 채우는 쪽이 나쁩니다.
        - 예외: 원문이 이미 잘 쓰여 있고 위 편집을 적용할 곳이 없다면 억지로 고치지 말고 거의 그대로
          두세요. 그 경우에는 summary에 원문이 이미 충실하다는 점을 밝히세요.

        채용공고 활용 방법:
        - 대상 채용공고는 원문의 어떤 경험을 앞세우고 어떤 표현을 쓸지 고르는 기준으로만 쓰세요.
        - 공고에 적힌 회사명·제품명·서비스명을 지원자의 경험이나 포부 문장에 넣지 마세요.
          ("귀사의 OO 개발에 기여하고 싶습니다" 같은 문장을 임의로 만들지 마세요.)
        - 같은 직무군의 유사 공고가 함께 주어지면, 이 직무군에서 무엇이 중요하게 평가되는지
          판단하는 데만 참고하세요. 그 공고의 표현이나 요구사항을 지원자 문장으로 옮기지 마세요.

        지원자 이력서가 함께 주어졌을 때 — 매우 중요:
        - 이력서 내용은 improvementPoints를 구체적으로 쓰는 데만 사용하세요.
          revisedAnswer 본문에는 절대 넣지 마세요. 원문에 없으면 이력서에 있어도 본문에 쓰지 않습니다.
        - "관련 경험이 있다면 적으세요"처럼 뭉뚱그리지 말고, 이력서에 실제로 있는 항목을 짚어
          "이력서에 적으신 ○○ 경험을 이 문항에 연결하면 좋습니다"처럼 쓰세요.
        - 이력서의 경험과 자소서 원문의 경험을 하나로 합치지 마세요. 서로 다른 시기·다른 프로젝트의
          일을 한 문장으로 묶으면 각각은 사실이어도 결과는 거짓이 됩니다.
        - 이력서의 보유 기술은 "다룰 줄 안다"는 목록일 뿐입니다. 특정 프로젝트에서 사용했다고
          단정하지 마세요.

        나머지 출력:
        - summary와 improvementPoints의 문장은 지원자가 그대로 읽는 글입니다.
          revisedAnswer, improvementPoints 같은 출력 항목 이름을 문장 안에 쓰지 마세요.
          가리켜야 한다면 "아래 개선 포인트"처럼 부르세요.
        - summary는 한두 문장으로 이번 첨삭의 핵심을 요약하세요.
          원문이 이미 충실해 크게 고칠 부분이 없다면 그 사실을 먼저 밝히고, 그다음에 보강 방향을 말하세요.
        - improvementPoints는 구체적인 개선 포인트를 항목별로 나열하세요.
          원문에 없어서 본문에 넣지 못한 보강 사항도 여기에 담으세요.
          이 공고에 중요한 순서로 최대 5개까지만 쓰세요. 항목을 채우려고 사소한 것까지 늘리지 마세요.
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

    /**
     * 검색된 근거 문단은 프롬프트에만 쓰고 버리지 않고, 첨삭 결과와 함께 돌려준다.
     * 상위 서비스가 이를 첨삭 이력 행에 같이 저장해 나중에 근거를 되짚을 수 있게 한다(REQ-903).
     */
    public CoverLetterItemReviewOutcome review(
        String question,
        String answer,
        JobPosting targetPosting,
        String applicantResume
    ) {
        List<Document> similarPostings = similarJobPostingSearchService.findSimilarPostings(
            answer,
            targetPosting.getJobCategoryId(),
            targetPosting.getJobPostingId(),
            3
        );

        String userPrompt = buildUserPrompt(
            question, answer, targetPosting, similarPostings, applicantResume
        );

        CoverLetterItemReviewResult result = chatClient.prompt()
            .system(SYSTEM_PROMPT)
            .user(userPrompt)
            .call()
            .entity(CoverLetterItemReviewResult.class);

        return new CoverLetterItemReviewOutcome(result, toEvidence(similarPostings));
    }

    /**
     * 검색 결과가 없으면 빈 리스트를 그대로 돌려준다 — null로 바꾸지 않는다.
     * 저장 단계에서 "검색은 돌았으나 결과 없음(빈 배열)"과 "이 기능 이전에 만들어진 첨삭(null)"을
     * 구분할 수 있어야, 나중에 RAG가 왜 안 먹었는지 추적할 수 있다.
     */
    private List<RagEvidence> toEvidence(List<Document> documents) {
        return documents.stream()
            .map(document -> {
                Object sourceId = document.getMetadata().get("source_id");
                return new RagEvidence(
                    sourceId != null ? sourceId.toString() : null,
                    document.getText(),
                    document.getScore()
                );
            })
            .toList();
    }

    private String buildUserPrompt(
        String question,
        String answer,
        JobPosting targetPosting,
        List<Document> similarPostings,
        String applicantResume
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

        if (applicantResume != null && !applicantResume.isBlank()) {
            builder.append("\n[지원자 이력서 — improvementPoints를 구체적으로 쓰는 데만 사용]\n")
                .append(applicantResume.trim())
                .append('\n');
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
