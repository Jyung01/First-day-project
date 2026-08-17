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
            "기업과 직무의 특징이 자연스럽게 이어지는 소개문으로 재구성합니다. "
                    + "짧은 초안은 제공된 기업·공고 정보 안에서 읽기 좋은 문단으로 구체화합니다.",
            "MAIN_TASKS",
            "지원자가 실제 업무를 빠르게 이해할 수 있도록 행동 중심으로 재구성합니다. "
                    + "겹치는 내용은 합치고 서로 다른 업무는 읽기 좋은 목록으로 나눕니다.",
            "QUALIFICATIONS",
            "지원 기준을 빠르게 확인할 수 있는 명확한 자격요건으로 재구성합니다. "
                    + "필수 조건의 강도를 높이거나 낮추지 않습니다.",
            "PREFERRED_CONDITIONS",
            "우대 역량과 경험이 분명히 보이도록 재구성하되, "
                    + "필수 조건처럼 단정하지 않는 자연스러운 표현을 사용합니다."
    );

    private static final String SYSTEM_PROMPT = """
            당신은 한국어 채용공고 전문 편집자입니다.
            다음 규칙을 반드시 지키세요.
            - 원문과 함께 제공된 기업·공고 정보는 모두 검증된 사실로 취급합니다.
            - 검증된 사실과 조건은 유지하되, 원문의 문장과 구조를 그대로 유지할 필요는 없습니다.
            - 문장 순서 변경, 문장 분리·통합, 중복 제거, 목록 변환을 자유롭게 활용합니다.
            - 짧거나 메모 형태인 초안은 제공된 정보 안에서 자연스럽고 구체적인 문장으로 확장합니다.
            - 원문과 제공 정보에 없는 기술, 업무, 경력, 자격, 혜택, 수치를 새로 만들지 않습니다.
            - 필수 조건을 우대 조건으로 바꾸거나 우대 조건을 필수 조건으로 강화하지 않습니다.
            - 과장, 차별, 모호한 표현을 피합니다.
            - 단순한 단어 치환을 피하고, 지원자가 읽기 쉬운 완성된 채용공고 문장으로 재작성합니다.
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
                ? "이전 결과가 원문과 지나치게 유사했습니다. "
                        + "사실관계는 유지하면서 문장 구조와 정보 배치를 적극적으로 재구성하세요."
                : "단어 몇 개만 바꾸지 말고 문장 구조와 정보 배치를 자연스럽게 재구성하세요.";

        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user("""
                        채용공고 항목: %s
                        항목별 편집 기준: %s

                        참고 가능한 기업·공고 정보:
                        %s

                        아래 원문을 실제 채용공고에 바로 사용할 수 있는 수준으로 재작성해 주세요.
                        원문과 참고 정보에 명시된 사실은 자유롭게 조합해도 되지만,
                        명시되지 않은 내용은 추측하거나 추가하지 마세요.
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
