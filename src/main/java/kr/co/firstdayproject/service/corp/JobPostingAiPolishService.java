package kr.co.firstdayproject.service.corp;

import java.util.Map;
import java.util.List;
import kr.co.firstdayproject.dto.corp.job.JobPostingAiPolishRequest;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class JobPostingAiPolishService {

    private static final int MAX_CONTENT_LENGTH = 5000;

    private static final Map<String, String> FIELD_LABELS = Map.of(
            "INTRODUCTION", "공고 소개",
            "MAIN_TASKS", "주요 업무",
            "QUALIFICATIONS", "자격요건",
            "PREFERRED_CONDITIONS", "우대사항"
    );

    private static final Map<String, String> FIELD_INSTRUCTIONS = Map.of(
            "INTRODUCTION",
            "회사의 채용 목적과 지원자에게 전하는 메시지가 자연스럽게 이어지도록 재작성합니다.",
            "MAIN_TASKS",
            "각 업무가 명확한 행동 중심 표현이 되도록 다듬고 문장 종결 형식을 통일합니다.",
            "QUALIFICATIONS",
            "지원 자격의 의미는 유지하면서 지원자가 확인하기 쉬운 일관된 자격요건 표현으로 재작성합니다.",
            "PREFERRED_CONDITIONS",
            "우대 조건의 의미는 유지하면서 강압적이지 않고 일관된 우대사항 표현으로 재작성합니다."
    );

    private static final String SYSTEM_PROMPT = """
            당신은 한국어 채용공고 전문 편집자입니다.
            다음 규칙을 반드시 지키세요.
            - 원문의 의미와 사실관계를 유지합니다.
            - 원문에 없는 기술, 경력, 자격, 혜택을 추가하지 않습니다.
            - 과장, 차별, 모호한 표현을 피합니다.
            - 지원자가 이해하기 쉽도록 자연스럽고 전문적으로 다듬습니다.
            - 원문을 그대로 반복하지 말고 문장 구조, 어미, 어휘 중 하나 이상을 반드시 개선합니다.
            - 짧고 단순한 문장은 구체적이고 매끄러운 채용공고 표현으로 재작성합니다.
            - 원문의 줄바꿈과 목록 구조는 가능한 한 유지합니다.
            - 설명, 제목, 따옴표, 마크다운 코드 블록 없이 수정된 본문만 반환합니다.
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
        String fieldInstruction = FIELD_INSTRUCTIONS.get(fieldType);
        String context = buildContext(company, request);
        if (normalizedContent.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException(
                    "문장은 5,000자 이하로 입력해 주세요."
            );
        }

        String result;
        try {
            result = generate(
                    fieldLabel,
                    fieldInstruction,
                    context,
                    normalizedContent,
                    false
            );

            if (result != null
                    && normalizeForComparison(result).equals(
                            normalizeForComparison(normalizedContent)
                    )) {
                result = generate(
                        fieldLabel,
                        fieldInstruction,
                        context,
                        normalizedContent,
                        true
                );
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
            boolean requireRephrasing
    ) {
        String additionalInstruction = requireRephrasing
                ? "원문과 동일하게 반환하지 말고, 사실관계를 유지하면서 표현을 분명히 바꿔 주세요."
                : "문장 구조와 표현을 실제로 개선해 주세요.";

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user("""
                        채용공고 항목: %s
                        항목별 편집 기준: %s

                        참고 가능한 기업·공고 정보:
                        %s

                        아래 원문을 작성 규칙에 맞게 다듬어 주세요.
                        참고 정보에 명시된 내용만 구체화에 활용하고, 없는 정보는 추측하지 마세요.
                        %s

                        원문:
                        %s
                        """.formatted(
                                fieldLabel,
                                fieldInstruction,
                                context,
                                additionalInstruction,
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
        appendContext(
                context,
                "기업 소개",
                firstText(company.getShortDescription(), company.getIntroduction())
        );
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
                    .limit(5)
                    .map(String::strip)
                    .distinct()
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

    private String firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private String normalizeForComparison(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", "").strip();
    }
}
