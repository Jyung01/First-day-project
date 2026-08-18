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
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Stores a private candidate profile embedding and searches job-posting embeddings. */
@Service
@Profile("!test")
@RequiredArgsConstructor
public class PersonalizedJobRecommendationService {

    private static final String PROFILE_SOURCE_TYPE = "user_profile";
    private final VectorStore vectorStore;
    @Qualifier("postgresJdbcTemplate")
    private final JdbcTemplate postgresJdbcTemplate;
    private final ResumeRepository resumeRepository;
    private final ResumeCareerRepository resumeCareerRepository;
    private final ResumeSkillRepository resumeSkillRepository;
    private final SkillRepository skillRepository;
    private final CoverLetterRepository coverLetterRepository;
    private final CoverLetterItemRepository coverLetterItemRepository;

    public Map<Long, Integer> findSemanticMatchScores(Long userId) {
        String profileId = profileVectorId(userId);
        String sql = """
                SELECT candidate.metadata ->> 'source_id' AS source_id,
                       ROUND((1 - (candidate.embedding <=> profile.embedding)) * 100)::int AS score
                FROM ai.vector_store candidate
                CROSS JOIN (SELECT embedding FROM ai.vector_store WHERE id = ?::uuid) profile
                WHERE candidate.metadata ->> 'source_type' = 'job_posting'
                ORDER BY candidate.embedding <=> profile.embedding
                LIMIT 100
                """;
        Map<Long, Integer> scores = new LinkedHashMap<>();
        postgresJdbcTemplate.query(sql, resultSet -> {
            try {
                scores.put(Long.valueOf(resultSet.getString("source_id")),
                        Math.max(0, Math.min(resultSet.getInt("score"), 100)));
            } catch (NumberFormatException ignored) {
                // Ignore malformed vector-store metadata.
            }
        }, profileId);
        return scores;
    }

    /** 저장·수정 완료 후에만 OpenAI 임베딩 API를 호출해 프로필 벡터를 갱신한다. */
    public void upsertProfileEmbedding(Long userId) {
        String profile = buildProfile(userId);
        String profileId = profileVectorId(userId);
        vectorStore.delete(List.of(profileId));
        if (profile.isBlank()) return;
        vectorStore.add(List.of(new Document(profileId, profile, Map.of(
                "source_type", PROFILE_SOURCE_TYPE,
                "source_id", String.valueOf(userId)
        ))));
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
