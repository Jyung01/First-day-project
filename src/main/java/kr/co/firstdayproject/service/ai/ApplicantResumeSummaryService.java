package kr.co.firstdayproject.service.ai;

import java.util.List;
import kr.co.firstdayproject.entity.resume.Resume;
import kr.co.firstdayproject.entity.resume.ResumeSkill;
import kr.co.firstdayproject.repository.job.SkillRepository;
import kr.co.firstdayproject.repository.resume.ResumeCareerRepository;
import kr.co.firstdayproject.repository.resume.ResumeRepository;
import kr.co.firstdayproject.repository.resume.ResumeSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 자소서 첨삭에 넘길 지원자 이력서 발췌를 만든다.
 *
 * 첨삭이 참고할 수 있는 "지원자 본인의 사실"이 자소서 답변 한 덩어리뿐이라, 답변이 얇으면
 * AI가 보강할 재료가 아예 없다. 이력서는 사용자가 직접 입력한 본인의 사실이므로 이를 재료로
 * 쓰는 것은 지어내기가 아니다.
 *
 * 담는 범위는 맞춤 추천(PersonalizedJobRecommendationService)이 쓰는 것과 같게 맞춘다 —
 * 요약·경력 유형·경력 직무명과 설명·보유 기술. 이름이나 연락처 같은 식별 정보는 넣지 않는다.
 * 이 문자열은 OpenAI로 전송되므로 범위를 넓힐 때는 반드시 다시 검토할 것.
 */
@Service
@RequiredArgsConstructor
public class ApplicantResumeSummaryService {

    private final ResumeRepository resumeRepository;
    private final ResumeCareerRepository resumeCareerRepository;
    private final ResumeSkillRepository resumeSkillRepository;
    private final SkillRepository skillRepository;

    /** 이력서가 없거나 내용이 비어 있으면 빈 문자열 — 호출부는 이때 이력서 섹션을 통째로 생략한다. */
    public String buildSummary(Long userId) {
        Resume resume = resumeRepository
            .findFirstByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId)
            .orElse(null);
        if (resume == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        append(builder, "이력서 요약", resume.getSummary());
        append(builder, "경력 유형", resume.getCareerType());

        resumeCareerRepository.findByResumeIdOrderByDisplayOrderAsc(resume.getResumeId())
            .forEach(career -> append(
                builder,
                "경력",
                joinNonBlank(career.getPositionTitle(), career.getDescription())
            ));

        List<Long> skillIds = resumeSkillRepository
            .findByIdResumeIdOrderByDisplayOrderAsc(resume.getResumeId()).stream()
            .map(ResumeSkill::getId)
            .map(id -> id.getSkillId())
            .toList();
        if (!skillIds.isEmpty()) {
            String skillNames = skillRepository.findAllById(skillIds).stream()
                .map(skill -> skill.getSkillName())
                .filter(name -> name != null && !name.isBlank())
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
            append(builder, "보유 기술", skillNames);
        }

        return builder.toString().trim();
    }

    private String joinNonBlank(String left, String right) {
        if (left == null || left.isBlank()) {
            return right;
        }
        if (right == null || right.isBlank()) {
            return left;
        }
        return left + " — " + right;
    }

    private void append(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append(label).append(": ").append(value.trim()).append('\n');
    }
}
