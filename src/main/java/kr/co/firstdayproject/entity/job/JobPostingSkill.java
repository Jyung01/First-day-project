package kr.co.firstdayproject.entity.job;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 공고별 기술 스택; 등록·수정 최대 5개는 서비스 계층에서 검증
 * DB table: job_posting_skills
 */
@Entity
@Table(name = "job_posting_skills")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class JobPostingSkill {

    @EmbeddedId
    private JobPostingSkillId id;

    public static JobPostingSkill of(
        Long jobPostingId,
        Long skillId
    ) {
        return JobPostingSkill.builder()
            .id(JobPostingSkillId.builder()
                .jobPostingId(jobPostingId)
                .skillId(skillId)
                .build())
            .build();
    }
}
