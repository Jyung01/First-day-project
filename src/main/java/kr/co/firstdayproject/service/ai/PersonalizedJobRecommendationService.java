package kr.co.firstdayproject.service.ai;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.co.firstdayproject.entity.coverletter.CoverLetter;
import kr.co.firstdayproject.entity.resume.Resume;
import kr.co.firstdayproject.entity.resume.ResumeCareer;
import kr.co.firstdayproject.entity.resume.ResumeSkill;
import kr.co.firstdayproject.repository.coverletter.CoverLetterItemRepository;
import kr.co.firstdayproject.repository.coverletter.CoverLetterRepository;
import kr.co.firstdayproject.repository.job.SkillRepository;
import kr.co.firstdayproject.repository.resume.ResumeCareerRepository;
import kr.co.firstdayproject.repository.resume.ResumeRepository;
import kr.co.firstdayproject.repository.resume.ResumeSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Stores a private candidate profile embedding and searches job-posting embeddings. */
@Service
@Profile("!test")
@RequiredArgsConstructor
public class PersonalizedJobRecommendationService {

    private static final String PROFILE_SOURCE_TYPE = "user_profile";
    private final VectorStore vectorStore;
    private final ResumeRepository resumeRepository;
    private final ResumeCareerRepository resumeCareerRepository;
    private final ResumeSkillRepository resumeSkillRepository;
    private final SkillRepository skillRepository;
    private final CoverLetterRepository coverLetterRepository;
    private final CoverLetterItemRepository coverLetterItemRepository;

    public Map<Long, Integer> findSemanticMatchScores(Long userId) {
        String profile = buildProfile(userId);
        if (profile.isBlank()) return Map.of();

        String profileId = profileVectorId(userId);
        vectorStore.delete(List.of(profileId));
        vectorStore.add(List.of(new Document(profileId, profile, Map.of(
                "source_type", PROFILE_SOURCE_TYPE,
                "source_id", String.valueOf(userId)
        ))));

        Map<Long, Integer> scores = new LinkedHashMap<>();
        vectorStore.similaritySearch(SearchRequest.builder()
                        .query(profile)
                        .topK(100)
                        .similarityThreshold(0.2)
                        .filterExpression("source_type == 'job_posting'")
                        .build())
                .forEach(document -> {
                    Object sourceId = document.getMetadata().get("source_id");
                    if (sourceId == null) return;
                    try {
                        int percent = (int) Math.round(document.getScore() * 100);
                        scores.put(Long.valueOf(sourceId.toString()), Math.max(0, Math.min(percent, 100)));
                    } catch (NumberFormatException ignored) {
                        // Ignore malformed vector-store metadata.
                    }
                });
        return scores;
    }

    private String buildProfile(Long userId) {
        StringBuilder profile = new StringBuilder();
        Resume resume = resumeRepository
                .findFirstByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId)
                .orElse(null);
        if (resume != null) {
            append(profile, "이력서 요약", resume.getSummary());
            append(profile, "경력 유형", resume.getCareerType());
            resumeCareerRepository.findByResumeIdOrderByDisplayOrderAsc(resume.getResumeId())
                    .forEach(career -> append(profile, "경력", career.getPositionTitle()
                            + " " + career.getDescription()));
            List<Long> skillIds = resumeSkillRepository
                    .findByIdResumeIdOrderByDisplayOrderAsc(resume.getResumeId()).stream()
                    .map(ResumeSkill::getId).map(id -> id.getSkillId()).toList();
            skillRepository.findAllById(skillIds)
                    .forEach(skill -> append(profile, "보유 기술", skill.getSkillName()));
        }
        coverLetterRepository.findFirstByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId)
                .ifPresent(letter -> coverLetterItemRepository
                        .findByCoverLetterIdOrderByDisplayOrderAsc(letter.getCoverLetterId())
                        .forEach(item -> append(profile, "자기소개서", item.getAnswer())));
        return profile.toString().trim();
    }

    private void append(StringBuilder builder, String label, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(label).append(": ").append(value.trim()).append('\n');
        }
    }

    private String profileVectorId(Long userId) {
        return UUID.nameUUIDFromBytes((PROFILE_SOURCE_TYPE + ":" + userId)
                .getBytes(StandardCharsets.UTF_8)).toString();
    }
}
