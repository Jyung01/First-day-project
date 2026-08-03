package kr.co.firstdayproject.entity.resume;

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
 * 이력서 보유 기술; 최대 10개는 서비스 계층에서 검증
 * DB table: resume_skills
 */
@Entity
@Table(name = "resume_skills")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ResumeSkill {

    @EmbeddedId
    private ResumeSkillId id;
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
