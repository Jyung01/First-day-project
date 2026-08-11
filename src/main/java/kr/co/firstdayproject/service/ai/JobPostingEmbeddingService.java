package kr.co.firstdayproject.service.ai;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.co.firstdayproject.entity.job.JobPosting;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 채용공고 콘텐츠를 임베딩해 ai.vector_store에 upsert/delete한다.
 * ai.vector_store.id는 UUID 컬럼이라, "job_posting:{jobPostingId}" 문자열을
 * name-based UUID(v3)로 변환해 결정적 id로 쓴다 — 같은 공고는 항상 같은 id가 나와
 * 재삽입 시 이전 행과 충돌하지 않는다.
 */
@Service
@Profile("!test")
@RequiredArgsConstructor
public class JobPostingEmbeddingService {

    private static final String SOURCE_TYPE = "job_posting";

    private final VectorStore vectorStore;
    private final JobPostingRepository jobPostingRepository;

    public void upsert(Long jobPostingId) {
        JobPosting posting = jobPostingRepository.findById(jobPostingId)
            .orElse(null);
        if (posting == null) {
            delete(jobPostingId);
            return;
        }

        String content = buildContent(posting);
        if (content.isBlank()) {
            delete(jobPostingId);
            return;
        }

        vectorStore.delete(List.of(vectorId(jobPostingId)));
        vectorStore.add(List.of(new Document(
            vectorId(jobPostingId),
            content,
            buildMetadata(posting)
        )));
    }

    public void delete(Long jobPostingId) {
        vectorStore.delete(List.of(vectorId(jobPostingId)));
    }

    private String vectorId(Long jobPostingId) {
        String name = SOURCE_TYPE + ":" + jobPostingId;
        return UUID.nameUUIDFromBytes(
            name.getBytes(StandardCharsets.UTF_8)
        ).toString();
    }

    private String buildContent(JobPosting posting) {
        StringBuilder builder = new StringBuilder();
        appendSection(builder, "제목", posting.getTitle());
        appendSection(builder, "주요업무", posting.getMainTasks());
        appendSection(builder, "자격요건", posting.getQualifications());
        appendSection(builder, "우대사항", posting.getPreferredConditions());
        appendSection(builder, "소개", posting.getIntroduction());
        return builder.toString().trim();
    }

    private void appendSection(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append(label).append(": ").append(value.trim()).append('\n');
    }

    private Map<String, Object> buildMetadata(JobPosting posting) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source_type", SOURCE_TYPE);
        metadata.put("source_id", String.valueOf(posting.getJobPostingId()));
        if (posting.getJobCategoryId() != null) {
            metadata.put(
                "job_category_id",
                String.valueOf(posting.getJobCategoryId())
            );
        }
        return metadata;
    }
}
