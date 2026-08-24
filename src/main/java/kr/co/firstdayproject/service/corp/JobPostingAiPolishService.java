package kr.co.firstdayproject.service.corp;

import java.util.List;
import java.util.Map;
import kr.co.firstdayproject.dto.corp.job.JobPostingAiPolishRequest;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class JobPostingAiPolishService {

    private static final int MAX_CONTENT_LENGTH = 5000;
    private static final int MAX_GENERATION_ATTEMPTS = 2;

    private static final Map<String, String> FIELD_LABELS = Map.of(
            "INTRODUCTION", "공고 소개",
            "MAIN_TASKS", "주요 업무",
            "QUALIFICATIONS", "자격요건",
            "PREFERRED_CONDITIONS", "우대사항"
    );

    private static final Map<String, String> FIELD_INSTRUCTIONS = Map.of(
            "INTRODUCTION",
            "기업의 특징과 채용 직무가 자연스럽게 연결되는 소개문으로 작성합니다. "
                    + "기업 한 줄 소개와 기업 소개에 담긴 분위기와 강점을 적극 활용합니다.",
            "MAIN_TASKS",
            "지원자가 실제로 수행할 일을 빠르게 이해할 수 있도록 행동 중심으로 작성합니다. "
                    + "직무와 기술 스택 정보를 원문의 업무에 자연스럽게 연결합니다.",
            "QUALIFICATIONS",
            "지원자가 필수 역량과 경험을 명확하게 확인할 수 있는 자격요건으로 작성합니다. "
                    + "원문에 없는 필수 조건을 새로 만들거나 조건의 강도를 높이지 않습니다.",
            "PREFERRED_CONDITIONS",
            "우대하는 역량과 경험이 분명히 보이도록 작성하되 필수 조건처럼 단정하지 않습니다. "
                    + "관련 기술 스택은 실제 제공된 항목만 활용합니다."
    );

    private static final String SYSTEM_PROMPT = """
            당신은 한국어 채용공고를 전문적으로 첨삭하는 에디터입니다.

            목표는 원문의 표현만 조금 바꾸는 것이 아니라, 원문을 핵심 재료로 삼고
            제공된 기업 정보와 공고 정보를 활용해 지원자가 읽기 좋은 완성형 문장으로 재작성하는 것입니다.

            다음 원칙을 지키세요.
            - 원문의 핵심 업무, 조건, 의도는 빠뜨리지 않습니다.
            - 기업 한 줄 소개, 기업 소개, 업종, 직무, 기술 스택을 문맥에 맞게 적극 활용합니다.
            - 참고 정보는 그대로 나열하지 말고 해당 항목에 도움이 되는 내용만 자연스럽게 녹입니다.
            - 문장 순서 변경, 문장 분리와 통합, 중복 제거, 목록 변환, 표현 확장을 자유롭게 수행합니다.
            - 메모나 단어 형태의 원문도 바로 게시할 수 있는 구체적이고 자연스러운 문장으로 발전시킵니다.
            - 입력에 없는 기술, 업무, 경력, 자격, 혜택, 수치 또는 기업 사실은 만들어 내지 않습니다.
            - 자격요건과 우대사항의 의미나 강도를 서로 바꾸지 않습니다.
            - 과장, 차별, 모호한 홍보성 표현은 피합니다.
            - 쉬운 한국어를 사용하고 항목 성격에 따라 짧은 문단이나 줄바꿈 목록으로 구성합니다.
            - '아래 항목은', '다음 내용은', '필수 역량입니다'처럼 내용을 소개하는 상투적인 서두를 쓰지 않습니다.
            - 자격요건과 우대사항은 곧바로 구체적인 조건이나 역량부터 작성합니다.
            - 설명, 제목, 따옴표, 마크다운 코드 블록 없이 첨삭된 본문만 반환합니다.
            """;

    private final ChatClient chatClient;
    private final CompanyRepository companyRepository;

    public JobPostingAiPolishService(
            ChatClient.Builder builder,
            CompanyRepository companyRepository
    ) {
        this.chatClient = builder.build();
        this.companyRepository = companyRepository;
    }

    public String polish(
            Long companyId,
            String fieldType,
            JobPostingAiPolishRequest request
    ) {
        if (companyId == null) {
            throw new IllegalArgumentException("기업회원 로그인이 필요합니다.");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "기업 정보를 찾을 수 없습니다."
                ));
        String fieldLabel = FIELD_LABELS.get(fieldType);
        if (fieldLabel == null) {
            throw new IllegalArgumentException("다듬을 수 없는 항목입니다.");
        }

        String content = request.content();
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("다듬을 문장을 입력해 주세요.");
        }

        String normalizedContent = content.strip();
        if (normalizedContent.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException(
                    "문장은 5,000자 이하로 입력해 주세요."
            );
        }

        String previousResult = normalizeOptional(request.previousResult());
        String result = null;
        try {
            String context = buildContext(company, request);
            for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
                result = generate(
                        fieldLabel,
                        FIELD_INSTRUCTIONS.get(fieldType),
                        context,
                        normalizedContent,
                        previousResult,
                        attempt > 0
                );

                result = removeGenericPreamble(result);
                if (isDifferentResult(result, normalizedContent, previousResult)) {
                    break;
                }
            }
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "AI 수정안을 생성하지 못했습니다. 잠시 후 다시 시도해 주세요.",
                    exception
            );
        }

        if (result == null || result.isBlank()) {
            throw new IllegalStateException(
                    "AI 수정안을 생성하지 못했습니다. 다시 시도해 주세요."
            );
        }

        return result.strip();
    }

    private String generate(
            String fieldLabel,
            String fieldInstruction,
            String context,
            String content,
            String previousResult,
            boolean requireRephrasing
    ) {
        String additionalInstruction = requireRephrasing
                ? "이전 결과가 원문과 지나치게 유사했습니다. 핵심 사실은 유지하되 "
                        + "회사와 공고의 문맥을 반영해 구성, 문장 흐름, 표현을 뚜렷하게 개선하세요."
                : "단순한 단어 치환에 그치지 말고 회사와 공고의 문맥을 반영해 "
                        + "구성, 문장 흐름, 표현을 충분히 개선하세요.";

        String previousResultInstruction = previousResult == null
                ? ""
                : """

                        직전 수정안:
                        %s

                        직전 수정안과 동일하거나 표현만 미세하게 바꾼 결과는 반환하지 마세요.
                        핵심 사실은 유지하면서 문장 구성과 흐름이 분명히 다른 수정안을 작성하세요.
                        """.formatted(previousResult);

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user("""
                        다듬을 채용공고 항목: %s
                        항목별 편집 방향: %s

                        참고할 기업 및 공고 정보:
                        %s

                        아래 원문의 핵심 내용은 유지하면서 참고 정보 중 관련 있는 내용을 활용해
                        실제 채용공고에 바로 사용할 수 있는 완성도 높은 본문으로 첨삭해 주세요.
                        참고 정보에 명시되지 않은 사실은 추가하지 마세요.
                        %s

                        %s

                        원문:
                        %s
                        """.formatted(
                                fieldLabel,
                                fieldInstruction,
                                context,
                                additionalInstruction,
                                previousResultInstruction,
                                content
                        ))
                .call()
                .content();
    }

    private String buildContext(
            Company company,
            JobPostingAiPolishRequest request
    ) {
        StringBuilder context = new StringBuilder();
        appendContext(context, "기업명", company.getCompanyName());
        appendContext(context, "업종", company.getIndustryName());
        appendContext(context, "기업 한 줄 소개", company.getShortDescription());
        appendContext(context, "기업 소개", company.getIntroduction());
        appendContext(context, "공고 제목", request.jobTitle());
        appendContext(context, "직무", request.jobCategory());
        appendContext(context, "고용 형태", request.employmentType());
        appendContext(context, "경력", request.careerType());
        appendContext(context, "학력", request.educationLevel());
        appendContext(context, "근무 지역", request.workRegion());

        List<String> skills = request.skillNames();
        if (skills != null && !skills.isEmpty()) {
            String skillText = skills.stream()
                    .filter(skill -> skill != null && !skill.isBlank())
                    .map(String::strip)
                    .distinct()
                    .limit(5)
                    .reduce((first, second) -> first + ", " + second)
                    .orElse(null);
            appendContext(context, "기술 스택", skillText);
        }

        return context.isEmpty()
                ? "제공된 추가 정보 없음"
                : context.toString().strip();
    }

    private void appendContext(
            StringBuilder context,
            String label,
            String value
    ) {
        if (value == null || value.isBlank()) {
            return;
        }
        String normalized = value.strip();
        if (normalized.length() > 1000) {
            normalized = normalized.substring(0, 1000);
        }
        context.append("- ")
                .append(label)
                .append(": ")
                .append(normalized)
                .append('\n');
    }

    private String normalizeForComparison(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", "").strip();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private boolean isDifferentResult(
            String result,
            String original,
            String previousResult
    ) {
        if (result == null || result.isBlank()) {
            return false;
        }

        String normalizedResult = normalizeForComparison(result);
        if (normalizedResult.equals(normalizeForComparison(original))) {
            return false;
        }
        return previousResult == null
                || !normalizedResult.equals(normalizeForComparison(previousResult));
    }

    private String removeGenericPreamble(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String[] lines = value.strip().split("\\R", -1);
        String firstLine = lines[0].strip();
        boolean isGenericPreamble = (firstLine.startsWith("아래 항목은")
                || firstLine.startsWith("다음 항목은")
                || firstLine.startsWith("아래 내용은")
                || firstLine.startsWith("다음 내용은"))
                && (firstLine.contains("역량")
                || firstLine.contains("요건")
                || firstLine.contains("업무")
                || firstLine.contains("사항"));

        if (!isGenericPreamble || lines.length == 1) {
            return value.strip();
        }

        return String.join("\n", java.util.Arrays.copyOfRange(
                lines,
                1,
                lines.length
        )).strip();
    }
}
